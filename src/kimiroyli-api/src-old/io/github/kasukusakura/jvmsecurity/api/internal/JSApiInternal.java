package io.github.kasukusakura.jvmsecurity.api.internal;

import io.github.kasukusakura.jvmsecurity.api.asm.ClassTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public abstract class JSApiInternal {
    public static BiConsumer<List<Method>, Class<?>> METHOD_INJECTION_REGISTER;
    public static Instrumentation INSTRUMENTATION;
    public static Predicate<StackWalker.StackFrame> FILTER_NOT_PLUGIN_CLASSLOADER = x -> true;
    public static JSApiInternal INTERNAL;

    public abstract void registerTransformer(ClassTransformer transformer);

    public abstract void unregisterTransformer(ClassTransformer transformer);
}
