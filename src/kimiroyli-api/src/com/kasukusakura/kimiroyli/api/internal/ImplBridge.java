package com.kasukusakura.kimiroyli.api.internal;

import com.kasukusakura.kimiroyli.api.perm.Permission;
import com.kasukusakura.kimiroyli.api.perm.PermissionContext;

import java.util.ServiceLoader;

public abstract class ImplBridge {

    public static final ImplBridge INSTANCE = ServiceLoader.load(ImplBridge.class.getModule().getLayer(), ImplBridge.class).iterator().next();

    public abstract PermissionContext currentPermissionContext();

    public abstract void permit(Object obj, Permission perm);
    public abstract void revoke(Object obj, Permission perm);

    public abstract PermissionContext myPermissions();
}
