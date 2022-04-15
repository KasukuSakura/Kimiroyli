package com.kasukusakura.kimiroyli.core.control;

import com.kasukusakura.kimiroyli.api.control.FileAccessControl;
import com.kasukusakura.kimiroyli.api.control.NetworkControl;
import com.kasukusakura.kimiroyli.api.control.SystemControl;
import com.kasukusakura.kimiroyli.api.perm.PermissionContext;
import com.kasukusakura.kimiroyli.api.perm.StandardPermissions;
import com.kasukusakura.kimiroyli.api.utils.StringFormatable;
import com.kasukusakura.kimiroyli.core.MiscKit;
import com.kasukusakura.kimiroyli.core.perm.PermCtxImpl;
import com.kasukusakura.kimiroyli.core.perm.PermManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.*;

public class CSBuiltIn {
    public static PermCtxImpl ctx() {
        return PermManager.CTX.get();
    }

    public static void init() {
        ControlServices.reg(FileAccessControl.class, new FileAccessControl() {
            @Override
            public void onFileRead(Object file) throws IOException {
                if (!ctx().hasPermission(StandardPermissions.FILE_SYSTEM_ACCESS)) {
                    throw new FileNotFoundException("No perm to read " + file);
                }
            }

            @Override
            public void onFileWrite(Object file) throws IOException {
                if (!ctx().hasPermission(StandardPermissions.FILE_SYSTEM_ACCESS)) {
                    throw new FileNotFoundException("No perm to write " + file);
                }
            }

            @Override
            public void onNewRandomAccessFile(Object file, String mode) throws IOException {
                if (!ctx().hasPermission(StandardPermissions.FILE_SYSTEM_ACCESS)) {
                    throw new FileNotFoundException("No perm to " + (mode.contains("w") ? "write " : "read ") + file);
                }
            }
        });
        ControlServices.reg(SystemControl.class, new SystemControl() {
            @Override
            public void onNativeLink(Class<?> caller, boolean isLoadLibrary, String lib) {

                if (ctx().hasPermission(StandardPermissions.NATIVE_LIBRARY_LINK))
                    return;

                throw new UnsatisfiedLinkError("Permission denied: Missing permission `NATIVE_LIBRARY_LINK`");

            }

            @Override
            public void onInitiativeShutdown(int code, boolean isHalt) {
                if (ctx().hasPermission(StandardPermissions.SHUTDOWN))
                    return;

                throw new SecurityException("Permission denied: Missing permission `SHUTDOWN`");

            }
        });
        ControlServices.reg(NetworkControl.class, new NetworkControl() {
            @Override
            public void onTcpConnect(InetAddress address, int port) throws IOException {
                if (!ctx().hasPermission(StandardPermissions.NETWORK)) {
                    throw new ConnectException(StringFormatable.format(
                            "Can't connect {}:{} because current context don't have network permission",
                            MiscKit.ipv6format(address), port
                    ));
                }
            }

            @Override
            public void onTcpBind(InetAddress address, int port) throws IOException {
                if (!ctx().hasPermission(StandardPermissions.NETWORK)) {
                    throw new BindException(StringFormatable.format(
                            "Can't bind {}:{} because current context don't have network permission",
                            MiscKit.ipv6format(address), port
                    ));
                }
            }

            @Override
            public void onUdpBind(SocketAddress local) throws IOException {
                if (!ctx().hasPermission(StandardPermissions.NETWORK)) {
                    throw new BindException(StringFormatable.format(
                            "Can't bind {} because current context don't have network permission",
                            local == null ? "0.0.0.0:0" : local
                    ));
                }
            }

            @Override
            public void onUdpConnect(InetSocketAddress address) throws IOException {
                if (!ctx().hasPermission(StandardPermissions.NETWORK)) {
                    throw new ConnectException(StringFormatable.format(
                            "Can't connect {} because current context don't have network permission",
                            address
                    ));
                }
            }
        });
    }
}
