package com.kasukusakura.kimiroyli.core;

import com.kasukusakura.kimiroyli.api.log.Logger;
import com.kasukusakura.kimiroyli.api.perm.Permission;
import com.kasukusakura.kimiroyli.api.perm.PermissionContext;
import com.kasukusakura.kimiroyli.api.perm.StandardPermissions;
import com.kasukusakura.kimiroyli.core.control.ControlServices;
import com.kasukusakura.kimiroyli.core.perm.PermManager;
import io.github.karlatemp.unsafeaccessor.UnsafeAccess;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class JdkRtBridge {
    private static final Logger LOGGER = Logger.getLogger("JdkRtBridge");
    private static final StackWalker WALKER = PermManager.WALKER;
    private static final Class<?> SUN_MISC_UNSAFE = ModuleLayer.boot()
            .findModule("jdk.unsupported")
            .map(it -> Class.forName(it, "sun.misc.Unsafe"))
            .orElse(null);
    private static final Function<Stream<StackWalker.StackFrame>, Boolean>
            IS_JAVA_REFLECT_DELEGATING_CLASSLOADER_INIT,
            IS_JAVA_RUNTIME_INITIALIZATION;

    static {
        class IsRefClassLoaderInit implements
                Function<Stream<StackWalker.StackFrame>, Boolean>,
                Predicate<StackWalker.StackFrame> {
            @Override
            public Boolean apply(Stream<StackWalker.StackFrame> stream) {
                var frameOptional = stream.skip(3)
                        .filter(this) // Skip java.lang.ClassLoader.<init>
                        .findFirst();
                if (frameOptional.isEmpty()) return Boolean.FALSE;
                var frame = frameOptional.get();
                var klass = frame.getDeclaringClass();
                if (klass.getModule() != Object.class.getModule()) return Boolean.FALSE;

                return klass.getName().endsWith(".DelegatingClassLoader");
            }

            @Override
            public boolean test(StackWalker.StackFrame stackFrame) {
                if (stackFrame.getDeclaringClass() == ClassLoader.class) {
                    return !stackFrame.getMethodName().equals("<init>");
                }
                return true;
            }
        }

        class IsJavaRuntimeInitialization implements
                Function<Stream<StackWalker.StackFrame>, Boolean>,
                Predicate<StackWalker.StackFrame> {
            private final ModuleLayer BOOT = ModuleLayer.boot();

            @SuppressWarnings("OptionalIsPresent")
            @Override
            public Boolean apply(Stream<StackWalker.StackFrame> stream) {
                var frameOptional = stream.skip(3).filter(this).findFirst();
                if (frameOptional.isPresent()) {
                    return frameOptional.get().getDeclaringClass().getModule().getLayer() == BOOT;
                }
                return Boolean.FALSE;
            }

            @Override
            public boolean test(StackWalker.StackFrame stackFrame) {
                var c = stackFrame.getDeclaringClass();
                if (c.getModule().getLayer() != BOOT) return true;
                return stackFrame.getMethodName().equals("<clinit>");
            }
        }
        IS_JAVA_REFLECT_DELEGATING_CLASSLOADER_INIT = new IsRefClassLoaderInit();
        IS_JAVA_RUNTIME_INITIALIZATION = new IsJavaRuntimeInitialization();
    }

    public static final Supplier<MethodInsnNode> REFLECTION_GET_CALLER_CLASS = ((Supplier<Supplier<MethodInsnNode>>) () -> {
        try {
            var ua = UnsafeAccess.getInstance();
            var javabase = Object.class.getModule();
            var reflection = Class.forName(javabase, "jdk.internal.reflect.Reflection");
            if (reflection == null) {
                throw new ClassNotFoundException("Can't find Reflection");
            }
            ua.getTrustedIn(reflection).findStatic(reflection, "getCallerClass", MethodType.methodType(Class.class));
            return () -> new MethodInsnNode(Opcodes.INVOKESTATIC, "jdk/internal/reflect/Reflection", "getCallerClass", "()Ljava/lang/Class;", false);
        } catch (Throwable throwable) {
            throw new ExceptionInInitializerError(throwable);
        }
    }).get();

    public static String BRIDGE;

    public static void hi() {
        new Throwable("Hi").printStackTrace(System.out);
    }

    public static void newThreadCheck() {
        if (LOGGER.isDebugEnabled()) {
            var track = new Throwable("New Thread Check: act from " + Thread.currentThread());
            LOGGER.debug(null, track);
        }

        ControlServices.SYSTEM_CONTROL.onNewThread();
    }

    public static void newClassLoaderCheck() {
        if (WALKER.walk(IS_JAVA_RUNTIME_INITIALIZATION)) return;
        if (WALKER.walk(IS_JAVA_REFLECT_DELEGATING_CLASSLOADER_INIT)) {
            // java.lang.reflect MethodAccessors init
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            var track = new Throwable("New ClassLoader: act from " + Thread.currentThread());
            LOGGER.debug(null, track);
        }

        ControlServices.SYSTEM_CONTROL.onNewClassLoader();
    }

    public static void ThreadGroup$checkAccess(ThreadGroup thiz) {
        if (WALKER.walk(IS_JAVA_RUNTIME_INITIALIZATION)) return;

        if (LOGGER.isDebugEnabled()) {
            var track = new Throwable("ThreadGroup.checkAccess(): " + thiz);
            LOGGER.debug(null, track);
        }

        ControlServices.SYSTEM_CONTROL.onThreadGroup_checkAccess(thiz);
    }

    public static void file$read(Object arg) throws IOException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("FS Read: {}", arg);

        ControlServices.FILE_ACCESS_CONTROL.onFileRead(arg);
    }

    public static void file$write(Object arg) throws IOException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("FS Write: {}", arg);

        ControlServices.FILE_ACCESS_CONTROL.onFileWrite(arg);
    }

    public static void file$raf(Object file, String mode) throws IOException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("new RandomAccessFile: {} <- {}", file, mode);

        ControlServices.FILE_ACCESS_CONTROL.onNewRandomAccessFile(file, mode);
    }

    public static void file$niochannel(Object file, Set<?> options) throws IOException {
        var read = false;
        var write = false;
        for (var opt : options) {
            if (opt == StandardOpenOption.READ) read = true;
            if (opt == StandardOpenOption.WRITE) write = true;
            if (opt == StandardOpenOption.APPEND) write = true;
            if (opt == StandardOpenOption.CREATE) write = true;
            if (opt == StandardOpenOption.CREATE_NEW) write = true;
            if (opt == StandardOpenOption.DELETE_ON_CLOSE) write = true;
        }
        if (options.isEmpty()) {
            read = true;
        }
        if (read) {
            file$read(file);
        }
        if (write) {
            file$write(file);
        }
        if (!read && !write) {
            throw new UnsupportedOperationException("Not reading or writing file: " + file + ", " + options);
        }
    }

    public static boolean reflect$checkSetAccessible(AccessibleObject ao, Class<?> caller, Class<?> declared, boolean throwIfException) {
        if (caller.getModule() == Object.class.getModule()) return true;

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[REFLECTION] SetAccessible: {} from {}", ao, caller);

        if (isUnsafeAccess(declared)) {
            var rsp = checkUnsafeAccess(caller, throwIfException, ao);
            if (rsp != null) {
                if (throwIfException) throw new java.lang.reflect.InaccessibleObjectException(rsp.toString());
                return false;
            }
        }

        return true;
    }

    public static void mh$privateLookupIn(Class<?> target, MethodHandles.Lookup caller) throws IllegalAccessException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[REFLECTION] privateLookupIn: {} from {}", target, caller);
        if (!isUnsafeAccess(target)) return;
        var rsp = checkUnsafeAccess(caller.lookupClass(), true, target);
        if (rsp != null) {
            throw new IllegalAccessException(rsp.toString());
        }

    }

    private static boolean isUnsafeAccess(Class<?> target) {
        if (SUN_MISC_UNSAFE == null) return false;
        if (target.getModule() == SUN_MISC_UNSAFE.getModule()) {
            return !PermissionContext.currentContext().hasPermission(StandardPermissions.SUN_MISC_UNSAFE);
        }
        return false;
    }

    private static Object checkUnsafeAccess(Class<?> caller, boolean doError, Object member) {
        if (caller.getModule().getLayer() == ModuleLayer.boot()) return null;
        if (!doError) return Boolean.TRUE;
        var stringBuilder = new StringBuilder();
        stringBuilder.append(caller).append(" cannot access ")
                .append(member)
                .append(" because unsafe access was limited.");
        return stringBuilder;
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    public static void onProxyEscape(Class<?> caller, ClassLoader cl, Class<?>[] classes) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[java.lang.reflect.Proxy] newAccess: {}, {}; from: {}", cl, classes, caller);

        if (classes == null) return; // error in java.base

        if (PermissionContext.currentContext().hasPermission(StandardPermissions.SUN_MISC_UNSAFE))
            return;

        var callerMod = caller.getModule();
        for (var c : classes) {
            if (c == null) continue;
            var targetModule = c.getModule();
            var pkg = c.getPackageName();

            if (Modifier.isPublic(c.getModifiers()) && targetModule.isExported(pkg, callerMod)) continue;

            if (targetModule.isOpen(pkg, callerMod)) continue;

            var msg = new StringBuilder("class access check failed: ");
            msg.append(caller).append(" (in ").append(callerMod).append(") ");
            msg.append("cannot access ").append(c).append(" (in ").append(targetModule).append(") ");
            msg.append("because ").append(targetModule).append(" does not ");
            msg.append(Modifier.isPublic(c.getModifiers()) ? "export " : "open ");
            msg.append(pkg).append(" to ");
            msg.append(callerMod);
            throw new InaccessibleObjectException(msg.toString());
        }
    }

    public static Class<?> tryResolveApi(String name) {
        if (name.startsWith("com.kasukusakura.kimiroyli.api.")) {
            return Class.forName(Permission.class.getModule(), name);
        }
        return null;
    }

    /*
     * @param isLoadLibrary true:  System.loadLibrary
     * @param isLoadLibrary false: System.load
     */
    public static void onCLibLink(Class<?> caller, String lib, boolean isLoadLibrary) {
        if (WALKER.walk(IS_JAVA_RUNTIME_INITIALIZATION)) return;

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[System] onCLibLink: caller: {}, lib: {}, loadLib: {}", caller, lib, isLoadLibrary);

        ControlServices.SYSTEM_CONTROL.onNativeLink(caller, isLoadLibrary, lib);
    }

    public static void onShutdown(int code, boolean isHalt) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[System] onExit: {}, isHalt={}", code, isHalt);

        ControlServices.SYSTEM_CONTROL.onInitiativeShutdown(code, isHalt);
    }

    public static void net$beforeTcpConnect(
            FileDescriptor fdObj,
            InetAddress address,
            int port
    ) throws IOException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[NETWORK] [TCP] [before connect] {}:{}", address, port);

        ControlServices.NETWORK_CONTROL.onTcpConnect(address, port);
    }

    public static void net$beforeTcpBind(
            FileDescriptor fdObj,
            InetAddress address,
            int port
    ) throws IOException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[NETWORK] [TCP] [before bind   ] {}:{}", address, port);

        ControlServices.NETWORK_CONTROL.onTcpBind(address, port);
    }

    public static void net$udpBind(SocketAddress local) throws IOException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[NETWORK] [UDP] [bind   ] {}", local);

        ControlServices.NETWORK_CONTROL.onUdpBind(local);
    }

    public static void net$udpConnect(InetSocketAddress address) throws IOException {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("[NETWORK] [UDP] [connect] {}", address);

        ControlServices.NETWORK_CONTROL.onUdpConnect(address);
    }
}
