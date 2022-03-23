package io.github.kasukusakura.jvmsecurity.api;

import io.github.kasukusakura.jvmsecurity.api.asm.ClassTransformer;
import io.github.kasukusakura.jvmsecurity.api.internal.JSApiInternal;

import java.lang.instrument.Instrumentation;

public class JvmSecurity {
    public static Instrumentation getInstrumentation() {
        return JSApiInternal.INSTRUMENTATION;
    }

    public static void registerTransformer(ClassTransformer transformer) {
        JSApiInternal.INTERNAL.registerTransformer(transformer);
    }

    public static void unregisterTransformer(ClassTransformer transformer) {
        JSApiInternal.INTERNAL.unregisterTransformer(transformer);
    }
}
