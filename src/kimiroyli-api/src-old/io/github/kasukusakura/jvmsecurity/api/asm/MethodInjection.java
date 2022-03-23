package io.github.kasukusakura.jvmsecurity.api.asm;

import io.github.kasukusakura.jvmsecurity.api.internal.JSApiInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public abstract class MethodInjection {
    public enum MethodType {
        STATIC, NON_STATIC, CONSTRUCTOR
    }

    public enum InjectionType {
        PRECALL, POST_CALL, REPLACE_CODE
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Hook {
        String name() default "";

        MethodType type();

        InjectionType injection();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface HookInfo {
        String methodDesc() default "";

        Class<?> returnType() default System.class;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface CodeGenerator {
        Class<? extends AsmCodeGenerator> value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TargetClass {
        Class<?> value();
    }

    // region register api
    protected final Class<?> targetClass;

    protected MethodInjection() {
        targetClass = null;
    }

    protected MethodInjection(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    public void register() {
        registerAll(getClass(), targetClass);
    }

    public static void register(Class<?> hook) {
        registerAll(hook, null);
    }
    // endregion

    // region impl
    private static void registerAll(Class<?> hookClass, Class<?> targetClass) {
        if (targetClass == null) {
            var tc = hookClass.getDeclaredAnnotation(TargetClass.class);
            if (tc == null) {
                throw new IllegalArgumentException("TargetClass of " + hookClass.getName() + " not found.");
            }
            targetClass = tc.value();
        }
        var hooks = new ArrayList<Method>();
        var methods = hookClass.getDeclaredMethods();
        for (var method : methods) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            var hook = method.getDeclaredAnnotation(Hook.class);
            if (hook == null) continue;
            hooks.add(method);
        }
        JSApiInternal.METHOD_INJECTION_REGISTER.accept(hooks, targetClass);
    }
    // endregion impl
}
