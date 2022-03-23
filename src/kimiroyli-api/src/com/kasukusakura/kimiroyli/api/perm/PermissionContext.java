package com.kasukusakura.kimiroyli.api.perm;

import com.kasukusakura.kimiroyli.api.internal.ImplBridge;

import java.security.PrivilegedAction;
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

    public abstract boolean hasPermission(Permission permission);

    public abstract void checkPermission(Permission permission);

    public abstract Iterator<Permission> permissions();

    /**
     * Drop some held permissions, and run command.
     */
    public abstract <T> T dropPermissions(Predicate<Permission> filter, PrivilegedAction<T> action);

    /**
     * Run with permissions that permitted to caller.
     */
    public abstract <T> T takePermissions(PrivilegedAction<T> action);

    /**
     * Run with permissions that permitted to caller.
     */
    public abstract <T> T takePermissions(Predicate<Permission> filter, PrivilegedAction<T> action);

    public abstract <T> T runAsCurrent(PrivilegedAction<T> action);
}
