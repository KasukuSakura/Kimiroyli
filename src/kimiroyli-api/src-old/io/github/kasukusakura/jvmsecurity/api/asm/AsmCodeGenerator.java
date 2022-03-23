package io.github.kasukusakura.jvmsecurity.api.asm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.lang.invoke.MethodHandle;

public interface AsmCodeGenerator {
    /**
     * @return return true if want skip framework code edit
     */
    boolean generateAsmCode(
            InsnList insnNodes,
            MethodHandle targetHandle,
            MethodNode targetMethod,
            ClassNode targetNode
    );
}
