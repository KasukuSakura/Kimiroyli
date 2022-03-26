package com.kasukusakura.kimiroyli.api.utils;

import io.github.karlatemp.unsafeaccessor.UnsafeAccess;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.WeakHashMap;

public interface StringFormatable {

    public void formatTo(StringBuilder builder);

    public static void toStringTo(Object obj, StringBuilder builder) {
        StringFormatableInternal.format0(obj, builder);
    }
    public static String toStringAs(Object obj) {
        if (obj == null) return "null";
        var sb = new StringBuilder();
        toStringTo(obj, sb);
        return sb.toString();
    }

    public static void formatTo(String template, StringBuilder rsp, Object... args) {
        StringFormatableInternal.format2(template, rsp, args);
    }

    public static String format(String template, Object... args) {
        var sb = new StringBuilder();
        formatTo(template, sb, args);
        return sb.toString();
    }
}

final class StringFormatableInternal {
    private static final UnsafeAccess UA = UnsafeAccess.getInstance();

    private static final MethodHandle FALLBACK;
    private static final WeakHashMap<Class<?>, MethodHandle> FORMAT_TO = new WeakHashMap<>();

    static {
        try {
            FALLBACK = MethodHandles.lookup().findStatic(
                    StringFormatableInternal.class,
                    "formatTo", MethodType.methodType(void.class, Object.class, StringBuilder.class)
            );
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }

    static MethodHandle findFormatToHandle(Class<?> t) {
        if (t == null) return FALLBACK;
        {
            var rsp = FORMAT_TO.get(t);
            if (rsp != null) return rsp;
        }
        try {
            var rsp = UA.getTrustedIn(t)
                    .findVirtual(t, "formatTo", MethodType.methodType(void.class, StringBuilder.class))
                    .asType(MethodType.methodType(void.class, Object.class, StringBuilder.class));
            FORMAT_TO.put(t, rsp);
            return rsp;
        } catch (NoSuchMethodException | IllegalAccessException ignored) {
            FORMAT_TO.put(t, FALLBACK);
            return FALLBACK;
        }
    }

    static void formatTo(Object obj, StringBuilder sb) {
        sb.append(obj);
    }

    static void format0(Object obj, StringBuilder builder) {
        if (obj == null) {
            builder.append("null");
            return;
        }
        if (obj instanceof StringFormatable) {
            ((StringFormatable) obj).formatTo(builder);
            return;
        } else if (obj instanceof CharSequence) {
            builder.append((CharSequence) obj);
            return;
        }
        if (obj instanceof char[]) {
            builder.append((char[]) obj);
        }
        if (obj instanceof Object[]) {
            var arr = (Object[]) obj;
            var len = arr.length;
            if (len == 0) {
                builder.append("[]");
            } else {
                builder.append("[");
                format0(arr[0], builder);
                for (var i = 1; i < len; i++) {
                    format0(arr[i], builder.append(", "));
                }
                builder.append("]");
            }
            return;
        }
        if (obj instanceof int[]) {
            builder.append(Arrays.toString((int[]) obj));
        } else if (obj instanceof byte[]) {
            builder.append(Arrays.toString((byte[]) obj));
        } else if (obj instanceof short[]) {
            builder.append(Arrays.toString((short[]) obj));
        } else if (obj instanceof long[]) {
            builder.append(Arrays.toString((long[]) obj));
        } else if (obj instanceof float[]) {
            builder.append(Arrays.toString((float[]) obj));
        } else if (obj instanceof double[]) {
            builder.append(Arrays.toString((double[]) obj));
        } else if (obj instanceof boolean[]) {
            builder.append(Arrays.toString((boolean[]) obj));
        } else {
            try {
                findFormatToHandle(obj.getClass()).invokeExact(obj, builder);
            } catch (Throwable e) {
                UA.getUnsafe().throwException(e);
                throw new RuntimeException(e);
            }
        }
    }

    public static void format2(String template, StringBuilder rsp, Object[] args) {
        var idx = 0;
        int next;
        var index0 = 0;
        while ((next = template.indexOf("{}", idx)) != -1) {
            rsp.append(template, idx, next);
            idx = next + 2;
            Object arg0 = null;
            if (args != null && index0 < args.length) {
                arg0 = args[index0];
            }
            index0++;
            format0(arg0, rsp);
        }
        rsp.append(template, idx, template.length());
    }
}