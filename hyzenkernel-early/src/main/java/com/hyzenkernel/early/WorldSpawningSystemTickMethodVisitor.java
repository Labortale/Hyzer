package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that wraps WorldSpawningSystem.tick() in a try-catch
 * to handle NullPointerException/IllegalStateException during chunk unload.
 */
public class WorldSpawningSystemTickMethodVisitor extends MethodVisitor {

    private final Label tryStart = new Label();
    private final Label tryEnd = new Label();
    private final Label catchHandler = new Label();
    private boolean visitedCode = false;

    public WorldSpawningSystemTickMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM9, mv);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        visitedCode = true;

        // Catch expected races during chunk unload/shutdown
        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/NullPointerException");
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

            // EarlyLogger.verbose("WorldSpawningSystem.tick failed - skipping spawn tick");
            mv.visitLdcInsn("WorldSpawningSystem.tick failed - skipping spawn tick");
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/hyzenkernel/early/EarlyLogger",
                    "verbose",
                    "(Ljava/lang/String;)V",
                    false
            );

            // Return void to continue ticking safely
            mv.visitInsn(Opcodes.RETURN);
        }

        super.visitMaxs(Math.max(maxStack, 2), maxLocals);
    }
}
