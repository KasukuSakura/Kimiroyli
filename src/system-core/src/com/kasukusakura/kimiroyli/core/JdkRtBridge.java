package com.kasukusakura.kimiroyli.core;

import com.kasukusakura.kimiroyli.api.log.Logger;
import com.kasukusakura.kimiroyli.api.perm.Permission;

import java.lang.reflect.AccessibleObject;
import java.nio.file.StandardOpenOption;
import java.util.Set;

public class JdkRtBridge {
    private static final Logger LOGGER = Logger.getLogger("JdkRtBridge");

    public static String BRIDGE;

    public static void hi() {
        new Throwable("Hi").printStackTrace(System.out);
    }

    public static void newThreadCheck() {
        if (LOGGER.isDebugEnabled()) {
            var track = new Throwable("New Thread Check: act from " + Thread.currentThread());
            LOGGER.debug(null, track);
        }
    }

    public static void newClassLoaderCheck() {
        if (LOGGER.isDebugEnabled()) {
            var track = new Throwable("New ClassLoader: act from " + Thread.currentThread());
            LOGGER.debug(null, track);
        }
    }

    public static void ThreadGroup$checkAccess(ThreadGroup thiz) {
        if (LOGGER.isDebugEnabled()) {
            var track = new Throwable("ThreadGroup.checkAccess(): " + thiz);
            LOGGER.debug(null, track);
        }
    }

    public static void file$read(Object arg) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("FS Read: {}", arg);
    }

    public static void file$write(Object arg) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("FS Write: {}", arg);
    }

    public static void file$raf(Object file, String mode) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("new RandomAccessFile: {} <- {}", file, mode);
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

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[REFLECTION] SetAccessible: {} from {}", ao, caller);

        return true;
    }

    public static Class<?> tryResolveApi(String name) {
        if (name.startsWith("com.kasukusakura.kimiroyli.api.")) {
            return Class.forName(Permission.class.getModule(), name);
        }
        return null;
    }

    /*
     * @param isLoadLibrary true:  System.loadLibrary
     * @param isLoadLibrary false: System.load
     */
    public static void onCLibLink(Class<?> caller, String lib, boolean isLoadLibrary) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[System] onCLibLink: caller: {}, lib: {}, loadLib: {}", caller, lib, isLoadLibrary);
    }

    public static void onShutdown(int code, boolean isHalt) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[System] onExit: {}, isHalt={}", code, isHalt);
    }
}
