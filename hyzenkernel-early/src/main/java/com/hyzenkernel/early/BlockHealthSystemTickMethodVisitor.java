package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that wraps BlockHealthSystem.tick() in a try-catch
 * to prevent NullPointerException crashes from stale player refs.
 */
public class BlockHealthSystemTickMethodVisitor extends MethodVisitor {

    private final Label tryStart = new Label();
    private final Label tryEnd = new Label();
    private final Label catchHandler = new Label();
    private boolean visitedCode = false;

    public BlockHealthSystemTickMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM9, mv);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        visitedCode = true;

        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/NullPointerException");
        mv.visitLabel(tryStart);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (visitedCode) {
            mv.visitLabel(tryEnd);
            mv.visitLabel(catchHandler);

            // Drop the exception instance
            mv.visitInsn(Opcodes.POP);

            // EarlyLogger.verbose("BlockHealthSystem.tick failed - skipping");
            mv.visitLdcInsn("BlockHealthSystem.tick failed - skipping");
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/hyzenkernel/early/EarlyLogger",
                    "verbose",
                    "(Ljava/lang/String;)V",
                    false
            );

            // Return void to continue safely
            mv.visitInsn(Opcodes.RETURN);
        }

        super.visitMaxs(Math.max(maxStack, 2), maxLocals);
    }
}
