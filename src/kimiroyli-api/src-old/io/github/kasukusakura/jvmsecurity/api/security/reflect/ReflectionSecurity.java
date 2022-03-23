package io.github.kasukusakura.jvmsecurity.api.security.reflect;

import java.lang.reflect.AccessibleObject;

public class ReflectionSecurity {
    private static final ReflectionSecurity DO_NOTHING = new ReflectionSecurity();
    private static ReflectionSecurity INSTANCE;

    public static ReflectionSecurity getInstance() {
        var rsp = INSTANCE;
        if (rsp == null) return DO_NOTHING;
        return rsp;
    }

    public static void setInstance(ReflectionSecurity value) {
        INSTANCE = value;
    }


    /**
     * Verify access to a member and return {@code false} if it is denied.
     *
     * @param currentClass the class performing the access
     * @param memberClass  the declaring class of the member being accessed
     * @param targetClass  the class of target object if accessing instance
     *                     field or method;
     *                     or the declaring class if accessing constructor;
     *                     or null if accessing static field or method
     * @param modifiers    the member's access modifiers
     * @return {@code false} if access to member is denied
     */
    public boolean verifyMemberAccess(
            Class<?> currentClass,
            Class<?> memberClass,
            Class<?> targetClass,
            int modifiers
    ) {
        return true;
    }

    /**
     * Returns {@code false} if caller is denied.
     */
    public boolean checkCanSetAccessible(
            AccessibleObject accessibleObject,
            Class<?> caller,
            Class<?> declaringClass,
            boolean throwExceptionIfDenied
    ) {
        return true;
    }

    /**
     * Returns {@code false} if memberClass's module not exports memberClass's
     * package to currentModule.
     */
    public boolean verifyModuleAccess(Module currentModule, Class<?> memberClass) {
        return true;
    }

    public void verifyProxyAccess(Class<?> caller, ClassLoader loader, Class<?>[] interfaces) {
    }
}
