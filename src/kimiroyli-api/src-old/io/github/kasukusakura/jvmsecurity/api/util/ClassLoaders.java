package io.github.kasukusakura.jvmsecurity.api.util;

import io.github.karlatemp.unsafeaccessor.UnsafeAccess;

import java.lang.invoke.*;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class ClassLoaders {
    public static final Function<Class<?>, ClassLoader> GET_CLASSLOADER;

    public static ClassLoader getClassLoader(Class<?> c) {
        return GET_CLASSLOADER.apply(c);
    }

    private static <T, R> Function<T, R> bindToFunc(
            MethodHandles.Lookup lk,
            MethodHandle handle
    ) throws Throwable {
        return (Function<T, R>) LambdaMetafactory.metafactory(
                lk, "apply",
                MethodType.methodType(Function.class),
                MethodType.methodType(Object.class, Object.class),
                handle,
                handle.type()
        ).dynamicInvoker().invoke();
    }

    static {
        var uaccess = UnsafeAccess.getInstance();
        { // GET_CLASSLOADER
            var lk = uaccess.getTrustedIn(Class.class);
            Function<Class<?>, ClassLoader> rst;
            search:
            {
                try {
                    var mh = lk.findGetter(Class.class, "classLoader", ClassLoader.class);
                    rst = bindToFunc(lk.in(Class.class), mh);
                    break search;
                } catch (Throwable ignored) {
                }

                try {
                    var mh = lk.findVirtual(Class.class, "getClassLoader0", MethodType.methodType(ClassLoader.class));
                    rst = bindToFunc(lk.in(Class.class), mh);
                    break search;
                } catch (Throwable ignored) {
                }

                try {
                    var mh = lk.findVirtual(Class.class, "getClassLoader", MethodType.methodType(ClassLoader.class));
                    rst = bindToFunc(lk.in(Class.class), mh);
                    break search;
                } catch (Throwable ignored) {
                }

                rst = Class::getClassLoader;
            }
            GET_CLASSLOADER = rst;
        }
    }
}
