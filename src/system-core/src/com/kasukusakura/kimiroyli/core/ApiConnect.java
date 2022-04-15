package com.kasukusakura.kimiroyli.core;

import com.kasukusakura.kimiroyli.api.internal.ImplBridge;
import com.kasukusakura.kimiroyli.api.perm.Permission;
import com.kasukusakura.kimiroyli.api.perm.PermissionContext;
import com.kasukusakura.kimiroyli.api.perm.StandardPermissions;
import com.kasukusakura.kimiroyli.core.control.ControlServices;
import com.kasukusakura.kimiroyli.core.perm.PermCtxImpl;
import com.kasukusakura.kimiroyli.core.perm.PermManager;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiConnect extends ImplBridge {
    @Override
    public PermissionContext currentPermissionContext() {
        return PermManager.CTX.get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<Object, List<Permission>> findPermMap(Object obj) {
        if (obj instanceof ThreadGroup) {
            return (Map) PermManager.PERMITTED_TO_THREAD_GROUP;
        } else if (obj instanceof ClassLoader) {
            return (Map) PermManager.PERMITTED_TO_CLASS_LOADER;
        } else if (obj instanceof Module) {
            return (Map) PermManager.PERMITTED_TO_MODULES;
        } else if (obj instanceof ProtectionDomain) {
            return (Map) PermManager.PERMITTED_TO_DOMAIN;
        } else {
            throw new IllegalArgumentException("No perm map for " + obj);
        }
    }

    @Override
    public void permit(Object obj, Permission perm) {
        PermManager.CTX.get().checkPermission(StandardPermissions.PERMISSION_MANAGER);

        findPermMap(obj).computeIfAbsent(obj, $ -> new ArrayList<>()).add(perm);
    }

    @Override
    public void revoke(Object obj, Permission perm) {
        PermManager.CTX.get().checkPermission(StandardPermissions.PERMISSION_MANAGER);

        var pms = findPermMap(obj).get(obj);
        if (pms == null) return;
        pms.remove(perm);
    }

    @Override
    public PermissionContext myPermissions() {
        var perms = PermManager.calcCallerPermissions();
        return new PermCtxImpl(perms);
    }

    @Override
    public void regCtrService(Class<?> klass, Object instance) {
        PermManager.CTX.get().checkPermission(StandardPermissions.ROOT);
        ControlServices.reg(klass, instance);
    }

    @Override
    public Object getService(Class<?> klass) {
        PermManager.CTX.get().checkPermission(StandardPermissions.ROOT);
        return ControlServices.get(klass);
    }
}
