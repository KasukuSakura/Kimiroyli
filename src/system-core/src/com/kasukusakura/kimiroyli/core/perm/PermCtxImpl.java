package com.kasukusakura.kimiroyli.core.perm;

import com.kasukusakura.kimiroyli.api.perm.Permission;
import com.kasukusakura.kimiroyli.api.perm.PermissionContext;
import com.kasukusakura.kimiroyli.api.perm.StandardPermissions;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.kasukusakura.kimiroyli.core.perm.PermManager.CTX;

@SuppressWarnings("unchecked")
public class PermCtxImpl extends PermissionContext {
    private final ArrayList<Permission> permissions;

    public PermCtxImpl(ArrayList<Permission> permissions) {
        this.permissions = permissions;
    }

    public PermCtxImpl() {
        this(new ArrayList<>());
    }

    @Override
    public void checkPermission(Permission permission) {
        if (!hasPermission(permission))
            throw new SecurityException("Missing permission " + permission);
    }

    @Override
    public boolean hasPermission(Permission permission) {
        if (permissions.contains(StandardPermissions.ROOT)) return true;
        return permissions.contains(permission);
    }

    @Override
    public Iterator<Permission> permissions() {
        return Collections.unmodifiableCollection(permissions).iterator();
    }

    @Override
    public String toString() {
        return "PermissionContext{" +
                permissions +
                '}';
    }

    @Override
    public <T> T runWithout(Predicate<Permission> filter, PrivilegedAction<T> action) {
        var newPerms = (ArrayList<Permission>) permissions.clone();
        newPerms.removeIf(filter);
        var old = CTX.get();
        try {
            CTX.set(new PermCtxImpl(newPerms));
            return action.run();
        } finally {
            CTX.set(old);
        }
    }

    @Override
    public <T> T runWith(PrivilegedAction<T> action) {
        var newPerms = PermManager.calcCallerPermissions();
        newPerms.addAll(0, permissions);
        var old = CTX.get();
        try {
            CTX.set(new PermCtxImpl(newPerms));
            return action.run();
        } finally {
            CTX.set(old);
        }
    }

    @Override
    public <T> T runWith(Predicate<Permission> filter, PrivilegedAction<T> action) {
        var newPerms = PermManager.calcCallerPermissions();
        newPerms.removeIf(filter.negate());
        newPerms.addAll(0, permissions);
        var old = CTX.get();
        try {
            CTX.set(new PermCtxImpl(newPerms));
            return action.run();
        } finally {
            CTX.set(old);
        }
    }

    @Override
    public <T> T runWith(Collection<Permission> perms, PrivilegedAction<T> action) {
        if (perms == null) return action.run();
        if (permissions.contains(StandardPermissions.ROOT)) {
            var nw = (ArrayList<Permission>) permissions.clone();
            nw.addAll(perms);

            var old = CTX.get();
            try {
                CTX.set(new PermCtxImpl(nw));
                return action.run();
            } finally {
                CTX.set(old);
            }
        }
        return runWith(perms::contains, action);
    }

    @Override
    public <T> T runAsCurrent(PrivilegedAction<T> action) {
        PermCtxImpl old;
        if ((old = CTX.get()) == this) {
            return action.run();
        }
        CTX.set(this);
        try {
            return action.run();
        } finally {
            CTX.set(old);
        }
    }

    @Override
    public <T> T runAs(Predicate<Permission> filter, PrivilegedAction<T> action) {
        var newPerms = PermManager.calcCallerPermissions();
        newPerms.removeIf(filter.negate());
        var old = CTX.get();
        try {
            CTX.set(new PermCtxImpl(newPerms));
            return action.run();
        } finally {
            CTX.set(old);
        }
    }

    @Override
    public <T> T runAs(Collection<Permission> permissions, PrivilegedAction<T> action) {
        if (permissions.contains(StandardPermissions.ROOT)) {
            var old = CTX.get();
            try {
                CTX.set(new PermCtxImpl(new ArrayList<>(permissions)));
                return action.run();
            } finally {
                CTX.set(old);
            }
        }
        return runAs(permissions::contains, action);
    }
}
