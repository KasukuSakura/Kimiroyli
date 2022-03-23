package com.kasukusakura.kimiroyli.core;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@SuppressWarnings({"SameParameterValue", "CodeBlock2Expr"})
class Hook_NioFileSystem {
    @SuppressWarnings("RedundantArrayCreation")
    static RunAnyLambda<ClassNode> reg() {
        var newFileChannel = Type.getMethodDescriptor(
                Type.getType(FileChannel.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(Set.class),
                        Type.getType(FileAttribute[].class),
                }
        );
        var newAsynchronousFileChannel = Type.getMethodDescriptor(
                Type.getType(AsynchronousFileChannel.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(Set.class),
                        Type.getType(ExecutorService.class),
                        Type.getType(FileAttribute[].class),
                }
        );
        var newByteChannel = Type.getMethodDescriptor(
                Type.getType(SeekableByteChannel.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(Set.class),
                        Type.getType(FileAttribute[].class),
                }
        );
        var implDelete = Type.getMethodDescriptor(
                Type.getType(boolean.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(boolean.class),
                }
        );
        var copy = Type.getMethodDescriptor(
                Type.getType(void.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(Path.class),
                        Type.getType(CopyOption[].class),
                }
        );
        var move = Type.getMethodDescriptor(
                Type.getType(void.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(Path.class),
                        Type.getType(CopyOption[].class),
                }
        );
        var getFileStore = Type.getMethodDescriptor(
                Type.getType(FileStore.class),
                new Type[]{
                        Type.getType(Path.class),
                }
        );
        var createDirectory = Type.getMethodDescriptor(
                Type.getType(void.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(FileAttribute[].class),
                }
        );
        var createSymbolicLink = Type.getMethodDescriptor(
                Type.getType(void.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(Path.class),
                        Type.getType(FileAttribute[].class),
                }
        );
        var createLink = Type.getMethodDescriptor(
                Type.getType(void.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(Path.class),
                }
        );
        var readSymbolicLink = Type.getMethodDescriptor(
                Type.getType(Path.class),
                new Type[]{
                        Type.getType(Path.class),
                }
        );
        var delete = Type.getMethodDescriptor(
                Type.getType(void.class),
                new Type[]{
                        Type.getType(Path.class),
                }
        );
        var deleteIfExists = Type.getMethodDescriptor(
                Type.getType(boolean.class),
                new Type[]{
                        Type.getType(Path.class),
                }
        );
        var readAttributes = Type.getMethodDescriptor(
                Type.getType(Map.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(String.class),
                        Type.getType(LinkOption[].class),
                }
        );
        var setAttribute = Type.getMethodDescriptor(
                Type.getType(void.class),
                new Type[]{
                        Type.getType(Path.class),
                        Type.getType(String.class),
                        Type.getType(Object.class),
                        Type.getType(LinkOption[].class),
                }
        );
        return node -> {
            ASMModify.editMethod(node, "newFileChannel", newFileChannel, met -> {
                checkChannel(met, 1, 2);
            });
            ASMModify.editMethod(node, "newAsynchronousFileChannel", newAsynchronousFileChannel, met -> {
                checkChannel(met, 1, 2);
            });
            ASMModify.editMethod(node, "newByteChannel", newByteChannel, met -> {
                checkChannel(met, 1, 2);
            });
            ASMModify.editMethod(node, "implDelete", implDelete, met -> {
                checkWrite(met, 1);
            });
            ASMModify.editMethod(node, "copy", copy, met -> {
                checkRead(met, 1);
                checkWrite(met, 2);
            });
            ASMModify.editMethod(node, "move", move, met -> {
                checkRead(met, 1);
                checkWrite(met, 2);
            });
            ASMModify.editMethod(node, "getFileStore", getFileStore, met -> {
                checkRead(met, 1);
            });
            ASMModify.editMethod(node, "createDirectory", createDirectory, met -> {
                checkRead(met, 1);
                checkWrite(met, 1);
            });
            ASMModify.editMethod(node, "createSymbolicLink", createSymbolicLink, met -> {
                checkRead(met, 1);
                checkWrite(met, 2);
            });
            ASMModify.editMethod(node, "createLink", createLink, met -> {
                checkRead(met, 1);
                checkWrite(met, 2);
            });
            ASMModify.editMethod(node, "readSymbolicLink", readSymbolicLink, met -> {
                checkRead(met, 1);
            });
            ASMModify.editMethod(node, "delete", delete, met -> {
                checkWrite(met, 1);
            });
            ASMModify.editMethod(node, "deleteIfExists", deleteIfExists, met -> {
                checkWrite(met, 1);
            });
            ASMModify.editMethod(node, "readAttributes", readAttributes, met -> {
                checkRead(met, 1);
            });
            ASMModify.editMethod(node, "setAttribute", setAttribute, met -> {
                checkWrite(met, 1);
            });
        };
    }

    static void checkRead(MethodNode m0, int arg) {
        if ((m0.access & Opcodes.ACC_ABSTRACT) != 0) return;

        var nl = new InsnList();
        nl.add(new VarInsnNode(Opcodes.ALOAD, arg));
        nl.add(Bootstrap.executeBridge("file$read", "(Ljava/lang/Object;)V"));
        push(m0, nl);

        //System.out.println("<check read: " + arg + "> -> " + m0.name + "." + m0.desc);
    }

    static void checkWrite(MethodNode m0, int arg) {
        if ((m0.access & Opcodes.ACC_ABSTRACT) != 0) return;

        var nl = new InsnList();
        nl.add(new VarInsnNode(Opcodes.ALOAD, arg));
        nl.add(Bootstrap.executeBridge("file$write", "(Ljava/lang/Object;)V"));
        push(m0, nl);

        //System.out.println("<check write: " + arg + "> -> " + m0.name + "." + m0.desc);
    }

    static void checkChannel(MethodNode m0, int arg, int argx) {
        if ((m0.access & Opcodes.ACC_ABSTRACT) != 0) return;

        var nl = new InsnList();
        nl.add(new VarInsnNode(Opcodes.ALOAD, arg));
        nl.add(new VarInsnNode(Opcodes.ALOAD, argx));
        nl.add(Bootstrap.executeBridge("file$niochannel", "(Ljava/lang/Object;Ljava/util/Set;)V"));
        push(m0, nl);

        //System.out.println("<check channel: " + arg + ", " + argx + "> -> " + m0.name + "." + m0.desc);

    }

    private static void push(MethodNode m0, InsnList nl) {
        if ((m0.access & Opcodes.ACC_ABSTRACT) != 0) return;
        m0.instructions.insertBefore(m0.instructions.getFirst(), nl);
    }
}
