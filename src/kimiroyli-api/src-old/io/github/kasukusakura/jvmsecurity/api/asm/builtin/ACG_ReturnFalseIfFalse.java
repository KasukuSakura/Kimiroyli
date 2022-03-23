package io.github.kasukusakura.jvmsecurity.api.asm.builtin;

import io.github.kasukusakura.jvmsecurity.api.asm.AsmCodeGenerator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.invoke.MethodHandle;

public class ACG_ReturnFalseIfFalse implements AsmCodeGenerator {
    /*
      // access flags 0x8
  static a()V
   L0
    LINENUMBER 19 L0
    GETSTATIC io/github/kasukusakura/jvmsecurity/javaagent/TestLauncher.a : Z
    IFNE L1
   L2
    LINENUMBER 20 L2
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "If false"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
   L1
    LINENUMBER 22 L1
   FRAME SAME
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    LDC "Exit"
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
   L3
    LINENUMBER 23 L3
    RETURN
    MAXSTACK = 2
    MAXLOCALS = 0
}

     */
    @Override
    public boolean generateAsmCode(InsnList insnNodes, MethodHandle targetHandle, MethodNode targetMethod, ClassNode targetNode) {
        var jump = new LabelNode();
        insnNodes.add(new JumpInsnNode(Opcodes.IFNE, new LabelNode()));
        insnNodes.add(new InsnNode(Opcodes.ICONST_0));
        insnNodes.add(new InsnNode(Opcodes.IRETURN));
        insnNodes.add(jump);
        return false;
    }
}
