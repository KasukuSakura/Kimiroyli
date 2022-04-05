package com.kasukusakura.kimiroyli.test;

import com.kasukusakura.kimiroyli.api.perm.PermissionContext;
import com.kasukusakura.kimiroyli.api.perm.StandardPermissions;
import org.junit.jupiter.api.Assertions;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;

@SuppressWarnings({"all"})
public class Testing {
    private static <T extends Throwable> void sneaklyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }

    private static <T> PrivilegedAction<T> ew(PrivilegedExceptionAction<T> action) {
        return () -> {
            try {
                return action.run();
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable throwable) {
                sneaklyThrow(throwable);
                throw new RuntimeException(throwable);
            }
        };
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("[TESTING] Executed");
        var testingModule = Testing.class.getModule();
        System.setErr(System.out);
        /*var allStackTraces = Thread.getAllStackTraces();
        for (var thread : allStackTraces.keySet()) {
            System.out.println(thread);
        }*/

        Testing.class.getDeclaredMethod("a").setAccessible(true);


        PermissionContext.permit(Testing.class.getModule(), StandardPermissions.PERMISSION_MANAGER);
        PermissionContext.permit(Testing.class.getModule(), StandardPermissions.ROOT);

        PermissionContext.currentContext().runWith(() -> {
            System.out.println(PermissionContext.currentContext());
            return null;
        });

        PermissionContext.currentContext().runAs(List.of(), ew(() -> {

            Assertions.assertThrowsExactly(
                    UnsatisfiedLinkError.class,
                    () -> System.load(new File("A").getAbsolutePath()),
                    "Permission denied: Missing permission `NATIVE_LIBRARY_LINK`"
            );
            Assertions.assertThrowsExactly(
                    UnsatisfiedLinkError.class,
                    () -> System.loadLibrary("A"),
                    "Permission denied: Missing permission `NATIVE_LIBRARY_LINK`"
            );

            Assertions.assertThrowsExactly(
                    IllegalAccessException.class,
                    () -> MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup()),
                    "class com.kasukusakura.kimiroyli.test.Testing cannot access class sun.misc.Unsafe because unsafe access was limited."
            );
            Assertions.assertThrowsExactly(
                    InaccessibleObjectException.class,
                    () -> Unsafe.class.getDeclaredField("theUnsafe").setAccessible(true),
                    "class com.kasukusakura.kimiroyli.test.Testing cannot access private static final sun.misc.Unsafe sun.misc.Unsafe.theUnsafe because unsafe access was limited."
            );
            Assertions.assertFalse(
                    Unsafe.class.getDeclaredField("theUnsafe").trySetAccessible()
            );

            Assertions.assertThrowsExactly(
                    InaccessibleObjectException.class,
                    () -> Proxy.getProxyClass(Testing.class.getClassLoader(), Class.forName("jdk.internal.access.JavaLangAccess")),
                    "class access check failed: class com.kasukusakura.kimiroyli.test.Testing (in " + testingModule + ") cannot access interface jdk.internal.access.JavaLangAccess (in module java.base) because module java.base does not export jdk.internal.access to " + testingModule
            );
            Assertions.assertThrowsExactly(
                    SecurityException.class,
                    () -> Runtime.getRuntime().exit(9),
                    "Permission denied: Missing permission `SHUTDOWN`"
            );
            Assertions.assertThrowsExactly(
                    SecurityException.class,
                    () -> Runtime.getRuntime().halt(9),
                    "Permission denied: Missing permission `SHUTDOWN`"
            );
            Assertions.assertThrowsExactly(
                    SecurityException.class,
                    () -> System.exit(9),
                    "Permission denied: Missing permission `SHUTDOWN`"
            );
            return null;
        }));

        Unsafe.class.getDeclaredField("theUnsafe").setAccessible(true);
        MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup());

        { // Network
            PermissionContext.currentContext().runAs(List.of(), ew(() -> {
                Assertions.assertThrowsExactly(
                        ConnectException.class,
                        () -> new Socket("::1", 9154),
                        "Can't connect /[0:0:0:0:0:0:0:1]:9154 because current context don't have network permission"
                );

                Assertions.assertThrowsExactly(
                        ConnectException.class,
                        () -> new URL("http://[::1]:80").openConnection().connect(),
                        "Can't connect /[0:0:0:0:0:0:0:1]:80 because current context don't have network permission"
                );
                Assertions.assertThrowsExactly(
                        BindException.class,
                        () -> new DatagramSocket(9484),
                        "Can't bind 0.0.0.0/0.0.0.0:9484 because current context don't have network permission"
                );
                System.out.println(PermissionContext.myPermissions());
                Assertions.assertThrowsExactly(
                        UncheckedIOException.class,
                        () -> PermissionContext.myPermissions().runAsCurrent(ew(DatagramSocket::new)).connect(
                                InetAddress.getByAddress("localhost", new byte[]{(byte) 127, 0, 0, 1}),
                                1578
                        ),
                        "java.net.ConnectException: Can't connect localhost/127.0.0.1:1578 because current context don't have network permission"
                );
                return null;
            }));
            var exec = Executors.newFixedThreadPool(1);
            try {
                var httpC = HttpClient.newBuilder()
                        .executor(exec)
                        .build();
                Assertions.assertThrows(
                        IOException.class,
                        () -> httpC.send(
                                HttpRequest.newBuilder()
                                        .GET()
                                        .uri(URI.create("http://[::1]"))
                                        .build(),
                                HttpResponse.BodyHandlers.discarding()
                        ),
                        "Can't connect /[0:0:0:0:0:0:0:1]:80 because current context don't have network permission"
                );
            } finally {
                exec.shutdown();
            }
        }

        Runtime.getRuntime().exit(0);
    }

    public static void a() {
        var a = new Random().nextBoolean();
        if (a) {
            System.out.println("true");
        } else {
            System.out.println("false");
        }
    }
}
