package io.github.kasukusakura.jvmsecurity.api.asm;

import org.objectweb.asm.tree.ClassNode;

import java.security.ProtectionDomain;

public interface ClassTransformer {
    ClassNode doTransform(
            Module module,
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer,
            ClassNode currentNode
    ) throws Exception;
}
