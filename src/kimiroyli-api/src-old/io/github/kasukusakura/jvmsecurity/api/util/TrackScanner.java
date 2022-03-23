package io.github.kasukusakura.jvmsecurity.api.util;

import io.github.kasukusakura.jvmsecurity.api.internal.JSApiInternal;

import java.util.Set;

public class TrackScanner {
    private static final ClassLoader JSC = TrackScanner.class.getClassLoader();
    private static final StackWalker WALKER = StackWalker.getInstance(
            Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE),
            4
    );

    public static Class<?> findCallerClass() {
        var frame = findCaller();
        if (frame == null) return null;
        return frame.getDeclaringClass();
    }

    public static Module findCallerModule() {
        var frame = findCaller();
        if (frame == null) return null;
        return frame.getDeclaringClass().getModule();
    }

    public static StackWalker.StackFrame findCaller() {
        return WALKER.walk(stream -> {
            return stream
                    // .skip(2)
                    // .peek(x -> System.out.println("`--- " + x))
                    .filter(c -> {
                        var cl = ClassLoaders.getClassLoader(c.getDeclaringClass());
                        return cl != null && cl != JSC;
                    })
                    .filter(JSApiInternal.FILTER_NOT_PLUGIN_CLASSLOADER)
                    .findFirst()
                    .orElse(null);
        });
    }
}
