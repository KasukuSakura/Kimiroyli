package com.kasukusakura.kimiroyli.api.internal;

import java.util.function.Supplier;

public class Threads {
    public static final ThreadGroup ROOT = ((Supplier<ThreadGroup>) () -> {
        var rsp = Thread.currentThread().getThreadGroup();
        while (true) {
            var p = rsp.getParent();
            if (p == null) return rsp;
            rsp = p;
        }
    }).get();

    public static final ThreadGroup JVM_SECURITY = new ThreadGroup(ROOT, "Jvm Security System");
}
