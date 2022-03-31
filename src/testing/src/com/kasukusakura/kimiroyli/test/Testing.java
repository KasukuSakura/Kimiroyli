package com.kasukusakura.kimiroyli.test;

import com.kasukusakura.kimiroyli.api.perm.PermissionContext;
import com.kasukusakura.kimiroyli.api.perm.StandardPermissions;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Proxy;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Random;

@SuppressWarnings({"deprecation", "ThrowablePrintedToSystemOut"})
public class Testing {
    private static <T> PrivilegedAction<T> ew(PrivilegedExceptionAction<T> action) {
        return () -> {
            try {
                return action.run();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("[TESTING] Executed");
        /*var allStackTraces = Thread.getAllStackTraces();
        for (var thread : allStackTraces.keySet()) {
            System.out.println(thread);
        }*/

        Testing.class.getDeclaredMethod("a").setAccessible(true);


        PermissionContext.permit(Testing.class.getModule(), StandardPermissions.PERMISSION_MANAGER);

        PermissionContext.currentContext().runWith(() -> {
            System.out.println(PermissionContext.currentContext());
            return null;
        });

        PermissionContext.currentContext().runAs(List.of(), ew(() -> {

            try {
                System.load(new File("A").getAbsolutePath());
                throw new AssertionError();
            } catch (UnsatisfiedLinkError e) {
                System.out.println(e);
            }
            try {
                System.loadLibrary("A");
                throw new AssertionError();
            } catch (UnsatisfiedLinkError e) {
                System.out.println(e);
            }

            try {
                MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup());
                throw new AssertionError();
            } catch (IllegalAccessException e) {
                System.out.println(e);
            }
            try {
                Unsafe.class.getDeclaredField("theUnsafe").setAccessible(true);
                throw new AssertionError();
            } catch (InaccessibleObjectException e) {
                System.out.println(e);
            }
            if (Unsafe.class.getDeclaredField("theUnsafe").trySetAccessible()) {
                throw new AssertionError();
            }

            try {
                var pxy = Proxy.getProxyClass(Testing.class.getClassLoader(), Class.forName("jdk.internal.access.JavaLangAccess"));
                System.err.println(pxy);
                throw new AssertionError();
            } catch (InaccessibleObjectException e) {
                System.out.println(e);
            }
            try {
                Runtime.getRuntime().exit(0);
                throw new AssertionError();
            } catch (SecurityException se) {
                System.out.println(se);
            }
            return null;
        }));

        Unsafe.class.getDeclaredField("theUnsafe").setAccessible(true);
        MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup());

        Runtime.getRuntime().exit(0);
    }

    public static void a() {
        var a = new Random().nextBoolean();
        if (a) {
            System.out.println("true");
        } else {
            System.out.println("false");
        }
    }
}
