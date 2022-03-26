package com.kasukusakura.kimiroyli.core;

import com.kasukusakura.kimiroyli.api.internal.Threads;
import com.kasukusakura.kimiroyli.api.log.LogAdapter;
import com.kasukusakura.kimiroyli.api.perm.StandardPermissions;
import com.kasukusakura.kimiroyli.core.log.DefLogAdapter;
import com.kasukusakura.kimiroyli.core.perm.PermCtxImpl;
import com.kasukusakura.kimiroyli.core.perm.PermManager;
import io.github.karlatemp.unsafeaccessor.Root;
import io.github.karlatemp.unsafeaccessor.Unsafe;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Bootstrap {
    @SuppressWarnings({"unused", "CodeBlock2Expr"})
    public static void premain(
            String opts,
            Instrumentation instrumentation,
            ModuleLayer.Controller controller,
            Class<?> BootLoader
    ) throws Throwable {
        final var boostThread = Thread.currentThread();
        final var boostThreadClassLoader = boostThread.getContextClassLoader();
        final var bootstrapClassLoader = Bootstrap.class.getClassLoader();
        boostThread.setContextClassLoader(bootstrapClassLoader);

        Unsafe.getUnsafe();
        LogAdapter.setAdapter(new DefLogAdapter());

        // Step. 1. Cleanup premain functions
        {
            var ccl = new ClassLoader() {
                Class<?> def(byte[] a) {
                    return defineClass(null, a, 0, a.length);
                }
            };
            var trans = new ClassFileTransformer() {
                @Override
                public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                    if (classBeingRedefined == null) return null;
                    var cr = new ClassReader(classfileBuffer);
                    var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                        @Override
                        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            var acc = super.visitMethod(access, name, descriptor, signature, exceptions);
                            boolean isPremain;
                            if ((isPremain = name.equals("premain")) || name.equals("findClass")) {
                                var cl = isPremain ? "java/lang/IllegalStateException" : "java/lang/ClassNotFoundException";
                                acc.visitTypeInsn(Opcodes.NEW, cl);
                                acc.visitInsn(Opcodes.DUP);
                                if (isPremain) {
                                    acc.visitLdcInsn("Cannot restart system.");
                                } else {
                                    acc.visitVarInsn(Opcodes.ALOAD, 1);
                                }
                                acc.visitMethodInsn(Opcodes.INVOKESPECIAL, cl, "<init>", "(Ljava/lang/String;)V", false);
                                acc.visitInsn(Opcodes.ATHROW);
                                acc.visitMaxs(0, 0);
                                return null;
                            }
                            return acc;
                        }
                    }, 0);
                    return cw.toByteArray();
                }
            };
            var launcherCL = BootLoader.getClassLoader();
            var launcherCLC = launcherCL.getClass();
            for (var field : launcherCLC.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    var map = (Map<?, ?>) field.get(launcherCL);
                    for (var k : map.keySet()) {
                        Class.forName(k.toString().replace('/', '.'), false, launcherCL);
                    }
                }
            }
            instrumentation.addTransformer(trans, true);
            instrumentation.retransformClasses(
                    BootLoader,
                    launcherCLC,
                    launcherCLC.getClassLoader().loadClass("com.kasukusakura.kimiroyli.javaagent.Launcher")
            );
            instrumentation.removeTransformer(trans);
        }

        // Step. 2. Link bridges to jdk internal
        {
            var javaBase = Object.class.getModule();
            var emitPkgs = javaBase.getPackages().stream()
                    .filter(it -> it.contains(".internal."))
                    .filter(it -> !javaBase.isExported(it))
                    .filter(it -> !javaBase.isOpen(it))
                    .collect(Collectors.toSet());
            var internalPkg = emitPkgs.iterator().next();
            Root.getModuleAccess().addExports(javaBase, internalPkg, Bootstrap.class.getModule());

            var frontendName = internalPkg.replace('.', '/') + "/KimiroyliFT";
            var backendName = "com/kasukusakura/kimiroyli/core/KimiroyliBT$" + UUID.randomUUID();
            var frontendNameL = "L" + frontendName + ";";
            var frontendWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            var backendWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

            frontendWriter.visit(Opcodes.V11, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, frontendName, null, "java/lang/Object", null);
            backendWriter.visit(Opcodes.V11, Opcodes.ACC_FINAL, backendName, null, frontendName, null);
            {
                var init0 = frontendWriter.visitMethod(Opcodes.ACC_PROTECTED, "<init>", "()V", null, null);
                init0.visitVarInsn(Opcodes.ALOAD, 0);
                init0.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                init0.visitInsn(Opcodes.RETURN);
                init0.visitMaxs(0, 0);

                init0 = backendWriter.visitMethod(Opcodes.ACC_PROTECTED, "<init>", "()V", null, null);
                init0.visitVarInsn(Opcodes.ALOAD, 0);
                init0.visitMethodInsn(Opcodes.INVOKESPECIAL, frontendName, "<init>", "()V", false);
                init0.visitInsn(Opcodes.RETURN);
                init0.visitMaxs(0, 0);
            }

            frontendWriter.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "I", frontendNameL, null, null);
            {
                var clinit = frontendWriter.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                clinit.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
                clinit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "getContextClassLoader", "()Ljava/lang/ClassLoader;", false);
                clinit.visitLdcInsn(backendName.replace('/', '.'));
                clinit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);

                clinit.visitInsn(Opcodes.ICONST_0);
                clinit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
                clinit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredConstructor", "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;", false);

                clinit.visitInsn(Opcodes.DUP);
                clinit.visitInsn(Opcodes.ICONST_1);
                clinit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Constructor", "setAccessible", "(Z)V", false);

                clinit.visitInsn(Opcodes.ICONST_0);
                clinit.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
                clinit.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Constructor", "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;", false);

                clinit.visitTypeInsn(Opcodes.CHECKCAST, frontendName);
                clinit.visitFieldInsn(Opcodes.PUTSTATIC, frontendName, "I", frontendNameL);
                clinit.visitInsn(Opcodes.RETURN);
                clinit.visitMaxs(0, 0);
            }

            var bridgeName = Type.getInternalName(JdkRtBridge.class);
            for (var method : JdkRtBridge.class.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())) continue;
                if (!Modifier.isStatic(method.getModifiers())) continue;
                if (method.isBridge()) continue;
                if (method.isSynthetic()) continue;

                var methodDesc = Type.getMethodDescriptor(method);

                var staticMethod = frontendWriter.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, method.getName(), methodDesc, null, null);
                var absMethod = frontendWriter.visitMethod(Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT, "$abs$$" + method.getName(), methodDesc, null, null);
                var implMethod = backendWriter.visitMethod(Opcodes.ACC_PROTECTED, "$abs$$" + method.getName(), methodDesc, null, null);

                staticMethod.visitFieldInsn(Opcodes.GETSTATIC, frontendName, "I", frontendNameL);
                var offset = 0;
                for (var argx : Type.getArgumentTypes(method)) {
                    staticMethod.visitVarInsn(argx.getOpcode(Opcodes.ILOAD), offset);
                    implMethod.visitVarInsn(argx.getOpcode(Opcodes.ILOAD), offset + 1);

                    offset += argx.getSize();
                }
                staticMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, frontendName, "$abs$$" + method.getName(), methodDesc, false);
                implMethod.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeName, method.getName(), methodDesc, false);

                var ret = Type.getReturnType(method);
                staticMethod.visitInsn(ret.getOpcode(Opcodes.IRETURN));
                implMethod.visitInsn(ret.getOpcode(Opcodes.IRETURN));

                staticMethod.visitMaxs(0, 0);
                implMethod.visitMaxs(0, 0);
            }

            {
                var usf = Unsafe.getUnsafe();
                var code = frontendWriter.toByteArray();
                var ft = usf.defineClass(null, code, 0, code.length, null, null);
                code = backendWriter.toByteArray();
                usf.defineClass(null, code, 0, code.length, bootstrapClassLoader, null);
                usf.ensureClassInitialized(ft);
                JdkRtBridge.BRIDGE = frontendName;
            }

        }

        // Step. 3. Permit jvm-security & main all permissions
        {
            PermManager.PERMITTED_TO_THREAD_GROUP.put(Threads.JVM_SECURITY, List.of(StandardPermissions.ROOT));
            PermManager.PERMITTED_TO_THREAD_GROUP.put(Threads.ROOT, List.of(StandardPermissions.ROOT));
            PermManager.CTX.set(new PermCtxImpl(new ArrayList<>(List.of(StandardPermissions.ROOT))));
        }

        // Step. 4. Hook jvm.
        var trans = new ClassFileTransformer() {
            volatile Class<?> crtAct;
            volatile RunAnyLambda<ClassNode> modifier;
            volatile boolean computeMax = false;

            @Override
            public byte[] transform(
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer
            ) throws IllegalClassFormatException {
                if (classBeingRedefined == null) return null;
                if (classBeingRedefined != crtAct) return null;
                var node = new ClassNode();
                new ClassReader(classfileBuffer).accept(node, 0);
                try {
                    modifier.execute(node);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    throw (ThreadDeath) new ThreadDeath().initCause(throwable);
                }
                var cw = new ClassWriter(computeMax ? ClassWriter.COMPUTE_MAXS : 0);
                node.accept(cw);
                return cw.toByteArray();
            }

            @SuppressWarnings("SameParameterValue")
            void modify(Class<?> c, boolean max, RunAnyLambda<ClassNode> modifier) throws UnmodifiableClassException {
                if (c == null) return;
                this.crtAct = c;
                this.modifier = modifier;
                this.computeMax = max;
                instrumentation.retransformClasses(c);
                this.crtAct = null;
                this.modifier = null;
            }
        };
        instrumentation.addTransformer(trans, true);

        {
            // region java.lang.Thread
            trans.modify(Thread.class, false, node -> ASMModify.injectToInit(node, () -> {
                var code = new InsnList();
                code.add(executeBridge("newThreadCheck", "()V"));
                return code;
            }));
            trans.modify(ThreadGroup.class, false, node -> ASMModify.editMethodRE(node, "checkAccess", "()V", met -> {
                var il = new InsnList();
                il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                il.add(executeBridge("ThreadGroup$checkAccess", "(Ljava/lang/ThreadGroup;)V"));
                met.instructions.insertBefore(met.instructions.getFirst(), il);
            }));
            // endregion

            // region java.lang.ClassLoader
            trans.modify(ClassLoader.class, false, node -> ASMModify.injectToInit(node, () -> {
                var code = new InsnList();
                code.add(executeBridge("newClassLoaderCheck", "()V"));
                return code;
            }));
            // endregion

            // region System IO
            trans.modify(FileInputStream.class, false, node -> ASMModify.injectToInit(node, () -> {
                var code = new InsnList();
                code.add(new VarInsnNode(Opcodes.ALOAD, 1));
                code.add(executeBridge("file$read", "(Ljava/lang/Object;)V"));
                return code;
            }));
            trans.modify(FileOutputStream.class, false, node -> ASMModify.injectToInit(node, () -> {
                var code = new InsnList();
                code.add(new VarInsnNode(Opcodes.ALOAD, 1));
                code.add(executeBridge("file$write", "(Ljava/lang/Object;)V"));
                return code;
            }));
            trans.modify(RandomAccessFile.class, false, node -> ASMModify.injectToInit(node, () -> {
                var code = new InsnList();
                code.add(new VarInsnNode(Opcodes.ALOAD, 1));
                code.add(new VarInsnNode(Opcodes.ALOAD, 2));
                code.add(executeBridge("file$raf", "(Ljava/lang/Object;Ljava/lang/String;)V"));
                return code;
            }));
            {
                RunAnyLambda<ClassNode> edit = Hook_NioFileSystem.reg();
                Class<?> c = Paths.get(".").getFileSystem().provider().getClass();
                while (c != FileSystemProvider.class) {
                    trans.modify(c, false, edit);
                    c = c.getSuperclass();
                }
            }
            // endregion

            //region Reflection
            trans.modify(AccessibleObject.class, false, node -> ASMModify.editMethodRE(node, "checkCanSetAccessible", "(Ljava/lang/Class;Ljava/lang/Class;Z)Z", met -> {
                var isn = new InsnList();
                isn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                isn.add(new VarInsnNode(Opcodes.ALOAD, 1));
                isn.add(new VarInsnNode(Opcodes.ALOAD, 2));
                isn.add(new VarInsnNode(Opcodes.ILOAD, 3));
                isn.add(executeBridge("reflect$checkSetAccessible", "(Ljava/lang/reflect/AccessibleObject;Ljava/lang/Class;Ljava/lang/Class;Z)Z"));

                var label_continue = new Label();
                var label_returnFalse = new Label();
                isn.add(new JumpInsnNode(Opcodes.IFNE, new LabelNode(label_continue)));

                isn.add(new LabelNode(label_returnFalse));
                isn.add(new InsnNode(Opcodes.ICONST_0));
                isn.add(new InsnNode(Opcodes.IRETURN));

                isn.add(new LabelNode(label_continue));

                met.instructions.insertBefore(met.instructions.getFirst(), isn);
            }));
            //endregion

            //region Network (WIP)
            //endregion

            // region System.loadLibrary (WIP)
            trans.modify(Runtime.class, false, node -> {
                ASMModify.editMethodRE(node, "loadLibrary0", "(Ljava/lang/Class;Ljava/lang/String;)V", met -> {
                });
                ASMModify.editMethodRE(node, "load0", "(Ljava/lang/Class;Ljava/lang/String;)V", met -> {
                });
            });
            // endregion

            //region Link jvm-security-api to ClassLoader.getSystemClassLoader()
            trans.modify(ClassLoader.getSystemClassLoader().getClass(), false, node -> {
                ASMModify.editMethodRE(node, "loadClass", "(Ljava/lang/String;Z)Ljava/lang/Class;", met -> {
                    var insnList = new InsnList();
                    insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    insnList.add(executeBridge("tryResolveApi", "(Ljava/lang/String;)Ljava/lang/Class;"));
                    insnList.add(new InsnNode(Opcodes.DUP));

                    var continue_ = new Label();
                    insnList.add(new JumpInsnNode(Opcodes.IFNULL, new LabelNode(continue_)));
                    insnList.add(new InsnNode(Opcodes.ARETURN));

                    insnList.add(new LabelNode(continue_));
                    insnList.add(new InsnNode(Opcodes.POP));

                    met.instructions.insertBefore(met.instructions.getFirst(), insnList);

                });
            });
            //endregion

        }

        // Step. 5. Prohibit unsafe accessing
        {
            // region Limit sun.misc.Unsafe access (WIP)
            // endregion

            // region Prohibit java.lang.reflect.Proxy escape (WIP)
            // endregion
        }

        instrumentation.removeTransformer(trans);

        boostThread.setContextClassLoader(boostThreadClassLoader);
    }

    private static AbstractInsnNode threadDump() {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Thread", "dumpStack", "()V");
    }

    static MethodInsnNode executeBridge(String name, String desc) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, JdkRtBridge.BRIDGE, name, desc, false);
    }
}
