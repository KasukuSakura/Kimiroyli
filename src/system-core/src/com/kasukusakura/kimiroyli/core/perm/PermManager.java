package com.kasukusakura.kimiroyli.core.perm;

import com.kasukusakura.kimiroyli.api.perm.Permission;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PermManager {
    public static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static final WeakHashMap<Module, List<Permission>> PERMITTED_TO_MODULES = new WeakHashMap<>();
    public static final WeakHashMap<ClassLoader, List<Permission>> PERMITTED_TO_CLASS_LOADER = new WeakHashMap<>();
    public static final WeakHashMap<ProtectionDomain, List<Permission>> PERMITTED_TO_DOMAIN = new WeakHashMap<>();
    public static final WeakHashMap<ThreadGroup, List<Permission>> PERMITTED_TO_THREAD_GROUP = new WeakHashMap<>();

    public static final ThreadLocal<PermCtxImpl> CTX = ThreadLocal.withInitial(() -> {
        var basePerms = calcCallerPermissions();
        var tg = PERMITTED_TO_THREAD_GROUP.get(Thread.currentThread().getThreadGroup());
        if (tg != null) {
            basePerms.addAll(0, tg);
        }
        return new PermCtxImpl(basePerms);
    });

    private static final Module JAVA_BASE = Object.class.getModule(), JVM_SECURITY_CORE = PermManager.class.getModule();

    @SuppressWarnings("RedundantIfStatement")
    private static final Predicate<StackWalker.StackFrame> FILTER = frame -> {
        var module = frame.getDeclaringClass().getModule();
        if (module == JAVA_BASE) return false;
        if (module == JVM_SECURITY_CORE) return false;
        if (module.getLayer() == JVM_SECURITY_CORE.getLayer()) return false;
        return true;
    };

    private static final Function<Stream<StackWalker.StackFrame>, Optional<StackWalker.StackFrame>> FIND_CALLER = stream -> stream.filter(
            FILTER
    ).findFirst();


    public static ArrayList<Permission> calcCallerPermissions() {
        var caller = WALKER.walk(FIND_CALLER);
        if (caller.isPresent()) {
            var frame = caller.get();
            var rsp = new ArrayList<Permission>();

            var a = PERMITTED_TO_MODULES.get(frame.getDeclaringClass().getModule());
            if (a != null) rsp.addAll(a);

            a = PERMITTED_TO_DOMAIN.get(frame.getDeclaringClass().getProtectionDomain());
            if (a != null) rsp.addAll(a);

            a = PERMITTED_TO_CLASS_LOADER.get(frame.getDeclaringClass().getClassLoader());
            if (a != null) rsp.addAll(a);

            return rsp;
        }
        return new ArrayList<>();
    }
}
