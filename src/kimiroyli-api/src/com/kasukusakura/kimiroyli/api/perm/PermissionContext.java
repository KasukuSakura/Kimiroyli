package com.kasukusakura.kimiroyli.api.perm;

import com.kasukusakura.kimiroyli.api.internal.ImplBridge;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * A context that stored what can running code do.
 * <p>
 * Each thread run in a special PermissionContext.
 * Running system apis without required permissions will
 * get a runtime error.
 * <p>
 * Typically, some thread can do everything if they're running
 * with {@link StandardPermissions#ROOT}
 */
public abstract class PermissionContext {
    protected PermissionContext() {
        if (getClass().getModule().getLayer() != PermissionContext.class.getModule().getLayer()) {
            throw new IllegalStateException("Invalid context instance: " + getClass());
        }
    }

    /**
     * Permit a code source with a permission.
     * <p>
     * Permission not updated real-time. It will take effect when calling
     * {@link #myPermissions()}, {@link #runWith(PrivilegedAction)}, or
     * new-threaded code.
     *
     * @param context Can be one of {@link java.lang.ThreadGroup},
     *                {@link java.lang.ClassLoader},
     *                {@link java.lang.Module} or {@link java.security.ProtectionDomain}
     * @apiNote Required {@link StandardPermissions#PERMISSION_MANAGER}
     */
    public static void permit(Object context, Permission permission) {
        ImplBridge.INSTANCE.permit(context, permission);
    }

    /**
     * Revoke a permission from a code code.
     *
     * @apiNote Required {@link StandardPermissions#PERMISSION_MANAGER}
     * @see #permit(Object, Permission)
     */
    public static void revoke(Object context, Permission permission) {
        ImplBridge.INSTANCE.revoke(context, permission);
    }

    /**
     * Get PermissionContext that current thread running with
     */
    public static PermissionContext currentContext() {
        return ImplBridge.INSTANCE.currentPermissionContext();
    }

    /**
     * Get permissions that permitted to caller.
     */
    public static PermissionContext myPermissions() {
        return ImplBridge.INSTANCE.myPermissions();
    }

    public abstract boolean hasPermission(Permission permission);

    /**
     * Throw a {@link SecurityException} if this context don't have tested permission
     *
     * @throws SecurityException if don't have provided permission.
     */
    public abstract void checkPermission(Permission permission);

    public abstract Iterator<Permission> permissions();

    /**
     * Drop some held permissions, and run command.
     */
    public abstract <T> T runWithout(Predicate<Permission> filter, PrivilegedAction<T> action);

    /**
     * Run with permissions that permitted to caller.
     */
    public abstract <T> T runWith(PrivilegedAction<T> action);

    /**
     * Run with permissions that permitted to caller.
     */
    public abstract <T> T runWith(Predicate<Permission> filter, PrivilegedAction<T> action);

    /**
     * Run with requested permissions
     *
     * @apiNote Only permissions that permitted to caller will be taken.
     */
    public abstract <T> T runWith(Collection<Permission> perms, PrivilegedAction<T> action);

    public abstract <T> T runAsCurrent(PrivilegedAction<T> action);

    /**
     * Run action with filtered permissions only.
     */
    public abstract <T> T runAs(Predicate<Permission> filter, PrivilegedAction<T> action);

    /**
     * Run action with filtered permissions only.
     */
    public abstract <T> T runAs(Collection<Permission> permissions, PrivilegedAction<T> action);
}
