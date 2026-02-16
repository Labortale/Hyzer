package com.hyzer.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that wraps WorldSpawningSystem.pickRandomChunk() in a try-catch
 * to handle Invalid entity reference errors caused by chunk unload races.
 */
public class PickRandomChunkMethodVisitor extends MethodVisitor {

    private final Label tryStart = new Label();
    private final Label tryEnd = new Label();
    private final Label catchHandler = new Label();
    private boolean visitedCode = false;

    public PickRandomChunkMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM9, mv);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        visitedCode = true;

        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/IllegalStateException");
        mv.visitLabel(tryStart);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (visitedCode) {
            mv.visitLabel(tryEnd);
            mv.visitLabel(catchHandler);

            // Drop the exception instance
            mv.visitInsn(Opcodes.POP);

            // HytaleLogger.getLogger().at(Level.FINE).log("WorldSpawningSystem.pickRandomChunk invalid ref - spawn skipped");
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/hypixel/hytale/logger/HytaleLogger",
                    "getLogger",
                    "()Lcom/hypixel/hytale/logger/HytaleLogger;",
                    false
            );
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/util/logging/Level", "FINE", "Ljava/util/logging/Level;");
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "com/hypixel/hytale/logger/HytaleLogger",
                    "at",
                    "(Ljava/util/logging/Level;)Lcom/hypixel/hytale/logger/HytaleLogger$Api;",
                    false
            );
            mv.visitLdcInsn("WorldSpawningSystem.pickRandomChunk invalid ref - spawn skipped");
            mv.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    "com/hypixel/hytale/logger/HytaleLogger$Api",
                    "log",
                    "(Ljava/lang/String;)V",
                    true
            );

            // Return null to skip this spawn job safely
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitInsn(Opcodes.ARETURN);
        }

        super.visitMaxs(Math.max(maxStack, 2), maxLocals);
    }
}
