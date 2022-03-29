package com.kasukusakura.kimiroyli.test;

import com.kasukusakura.kimiroyli.api.perm.PermissionContext;
import com.kasukusakura.kimiroyli.api.perm.StandardPermissions;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Proxy;
import java.util.Random;

@SuppressWarnings({"deprecation", "ThrowablePrintedToSystemOut"})
public class Testing {
    public static void main(String[] args) throws Throwable {
        System.out.println("[TESTING] Executed");
        /*var allStackTraces = Thread.getAllStackTraces();
        for (var thread : allStackTraces.keySet()) {
            System.out.println(thread);
        }*/

        Testing.class.getDeclaredMethod("a").setAccessible(true);


        PermissionContext.permit(Testing.class.getModule(), StandardPermissions.PERMISSION_MANAGER);

        PermissionContext.currentContext().takePermissions(() -> {
            System.out.println(PermissionContext.currentContext());
            return null;
        });

        try {
            System.load(new File("A").getAbsolutePath());
        } catch (UnsatisfiedLinkError ignored) {
        }
        try {
            System.loadLibrary("A");
        } catch (UnsatisfiedLinkError ignored) {
        }

        try {
            MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup());
        } catch (IllegalAccessException e) {
            System.out.println(e);
        }
        try {
            Unsafe.class.getDeclaredField("theUnsafe").setAccessible(true);
        } catch (InaccessibleObjectException e) {
            System.out.println(e);
        }
        if (Unsafe.class.getDeclaredField("theUnsafe").trySetAccessible()) {
            throw new AssertionError();
        }

        try {
            var pxy = Proxy.getProxyClass(Testing.class.getClassLoader(), Class.forName("jdk.internal.access.JavaLangAccess"));
            System.err.println(pxy);
        } catch (InaccessibleObjectException e) {
            System.out.println(e);
        }
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
