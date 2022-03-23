package com.kasukusakura.kimiroyli.core;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.List;

public class ASMModify {
    private static class AnalyzeValue extends BasicValue {
        public AnalyzeValue(Type type) {
            super(type);
        }
    }

    static class AnalyzerInterpreter extends SourceInterpreter {
        MethodInsnNode init;

        protected AnalyzerInterpreter(int api) {
            super(api);
        }

        @Override
        public SourceValue naryOperation(AbstractInsnNode insn, List<? extends SourceValue> values) {
            if (insn.getOpcode() == INVOKESPECIAL && init == null) {
                var mh = (MethodInsnNode) insn;
                if (!values.isEmpty() && mh.name.equals("<init>")) {
                    var fst = values.get(0);
                    if (!fst.insns.isEmpty()) {
                        var firstInsn = fst.insns.iterator().next();
                        if (firstInsn instanceof VarInsnNode) {
                            var vin = (VarInsnNode) firstInsn;
                            if (vin.getOpcode() == ALOAD && vin.var == 0) {
                                init = mh;
                            }
                        }
                    }
                }
            }
            return super.naryOperation(insn, values);
        }
    }

    public static void injectToInit(ClassNode classNode, RunAnyRsp<InsnList> code) throws Throwable {
        for (var met : classNode.methods) {
            if (!"<init>".equals(met.name)) continue;
            var interpreter = new AnalyzerInterpreter(Opcodes.ASM9);
            var analyzer = new Analyzer<>(interpreter);
            analyzer.analyze(classNode.name, met);
            if (interpreter.init.owner.equals(classNode.superName)) {
                met.instructions.insert(interpreter.init, code.execute());
            }
        }
    }

    public static void injectToInit(
            ClassNode classNode,
            RunAnyLambda2<AbstractInsnNode, MethodNode> action
    ) throws Throwable {
        for (var met : classNode.methods) {
            if (!"<init>".equals(met.name)) continue;
            var interpreter = new AnalyzerInterpreter(Opcodes.ASM9);
            var analyzer = new Analyzer<>(interpreter);
            analyzer.analyze(classNode.name, met);
            if (interpreter.init.owner.equals(classNode.superName)) {
                action.execute(interpreter.init, met);
            }
        }
    }

    public static void editMethodRE(ClassNode node, String name, String desc, RunAnyLambda<MethodNode> action) throws Throwable {
        editMethod(node, name, desc, action, true);
    }

    public static void editMethod(ClassNode node, String name, String desc, RunAnyLambda<MethodNode> action) throws Throwable {
        editMethod(node, name, desc, action, false);
    }

    public static void editMethod(ClassNode node, String name, String desc, RunAnyLambda<MethodNode> action, boolean reqExists) throws Throwable {
        for (var met : node.methods) {
            if (name != null && !name.equals(met.name)) continue;
            if (desc != null && !desc.equals(met.desc)) continue;
            action.execute(met);
            reqExists = false;
        }
        if (reqExists) {
            throw new NoSuchMethodException(name + " " + desc + " in " + node.name);
        }
    }
}
