package com.hyzer.early;

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

            // Log warning with stack trace
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "com/hypixel/hytale/logger/HytaleLogger",
                    "getLogger",
                    "()Lcom/hypixel/hytale/logger/HytaleLogger;",
                    false
            );
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/util/logging/Level", "WARNING", "Ljava/util/logging/Level;");
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "com/hypixel/hytale/logger/HytaleLogger",
                    "at",
                    "(Ljava/util/logging/Level;)Lcom/hypixel/hytale/logger/HytaleLogger$Api;",
                    false
            );
            mv.visitInsn(Opcodes.SWAP);
            mv.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    "com/google/common/flogger/LoggingApi",
                    "withCause",
                    "(Ljava/lang/Throwable;)Lcom/google/common/flogger/LoggingApi;",
                    true
            );
            mv.visitLdcInsn("WorldSpawningSystem.tick failed - skipping spawn tick");
            mv.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    "com/google/common/flogger/LoggingApi",
                    "log",
                    "(Ljava/lang/String;)V",
                    true
            );
            mv.visitInsn(Opcodes.POP);

            // Return void to continue ticking safely
            mv.visitInsn(Opcodes.RETURN);
        }

        super.visitMaxs(Math.max(maxStack, 4), maxLocals);
    }
}
