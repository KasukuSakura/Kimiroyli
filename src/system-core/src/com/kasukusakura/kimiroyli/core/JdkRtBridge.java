package com.kasukusakura.kimiroyli.core;

import com.kasukusakura.kimiroyli.api.perm.Permission;

import java.lang.reflect.AccessibleObject;
import java.nio.file.StandardOpenOption;
import java.util.Set;

public class JdkRtBridge {
    public static String BRIDGE;

    public static void hi() {
        new Throwable("Hi").printStackTrace(System.out);
    }

    public static void newThreadCheck() {
        new Throwable("New Thread Check: act from " + Thread.currentThread()).printStackTrace(System.out);
    }

    public static void newClassLoaderCheck() {
        new Throwable("New ClassLoader: act from " + Thread.currentThread()).printStackTrace(System.out);
    }

    public static void ThreadGroup$checkAccess(ThreadGroup thiz) {
        new Throwable("ThreadGroup.checkAccess(): " + thiz).printStackTrace(System.out);
    }

    public static void file$read(Object arg) {
        System.out.println("FS Read: " + arg);
    }

    public static void file$write(Object arg) {
        System.out.println("FS Write: " + arg);
    }

    public static void file$raf(Object file, String mode) {
    }

    public static void file$niochannel(Object file, Set<?> options) {
        var read = false;
        var write = false;
        for (var opt : options) {
            if (opt == StandardOpenOption.READ) read = true;
            if (opt == StandardOpenOption.WRITE) write = true;
            if (opt == StandardOpenOption.APPEND) write = true;
            if (opt == StandardOpenOption.CREATE) write = true;
            if (opt == StandardOpenOption.CREATE_NEW) write = true;
            if (opt == StandardOpenOption.DELETE_ON_CLOSE) write = true;
        }
        if (options.isEmpty()) {
            read = true;
        }
        if (read) {
            file$read(file);
        }
        if (write) {
            file$write(file);
        }
        if (!read && !write) {
            throw new UnsupportedOperationException("Not reading or writing file: " + file + ", " + options);
        }
    }

    public static boolean reflect$checkSetAccessible(AccessibleObject ao, Class<?> caller, Class<?> declared, boolean throwIfException) {
        if (caller.getModule() == Object.class.getModule()) return true;

        System.out.println("[REFLECTION] SetAccessible: " + ao + " from " + caller);
        return true;
    }

    public static Class<?> tryResolveApi(String name) {
        if (name.startsWith("com.kasukusakura.kimiroyli.api.")) {
            return Class.forName(Permission.class.getModule(), name);
        }
        return null;
    }
}
