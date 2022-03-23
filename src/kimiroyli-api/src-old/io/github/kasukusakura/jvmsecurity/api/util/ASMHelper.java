package io.github.kasukusakura.jvmsecurity.api.util;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class ASMHelper {
    public static MethodNode findMethod(
            ClassNode node,
            int exceptedModifiers,
            int unexpectedModifiers,
            String name,
            String desc
    ) {
        if (node == null || node.methods == null) return null;
        for (var method : node.methods) {
            if ((method.access & exceptedModifiers) != exceptedModifiers) continue;
            if ((method.access & unexpectedModifiers) != 0) continue;
            if (!method.name.equals(name)) continue;
            if (!method.desc.equals(desc)) continue;
            return method;
        }
        return null;
    }

    public static boolean hasAnnotation(MethodNode met, Class<?> annotation) {
        return hasAnnotation(met, annotation.getName());
    }

    public static boolean hasAnnotation(MethodNode met, String annotation) {
        return hasAnnotation(met.visibleAnnotations, annotation) || hasAnnotation(met.invisibleAnnotations, annotation);
    }

    private static boolean hasAnnotation(List<AnnotationNode> annotations, String annotation) {
        if (annotations == null) return false;
        if (annotation == null) return false;
        var internalName = 'L' + annotation.replace('.', '/') + ';';
        for (var a : annotations) {
            if (a.desc.equals(internalName)) return true;
        }
        return false;
    }
}
