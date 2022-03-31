package com.kasukusakura.kimiroyli.api.perm;

import com.kasukusakura.kimiroyli.api.internal.ImplBridge;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

public abstract class PermissionContext {
    protected PermissionContext() {
        if (getClass().getModule().getLayer() != PermissionContext.class.getModule().getLayer()) {
            throw new IllegalStateException("Invalid context instance: " + getClass());
        }
    }

    public static void permit(Object context, Permission permission) {
        ImplBridge.INSTANCE.permit(context, permission);
    }

    public static void revoke(Object context, Permission permission) {
        ImplBridge.INSTANCE.revoke(context, permission);
    }

    public static PermissionContext currentContext() {
        return ImplBridge.INSTANCE.currentPermissionContext();
    }

    public static PermissionContext myPermissions() {
        return ImplBridge.INSTANCE.myPermissions();
    }

    public abstract boolean hasPermission(Permission permission);

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
