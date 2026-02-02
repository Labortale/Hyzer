package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that wraps WorldMapTracker.unloadImages() in a try-catch
 * to handle the iterator corruption NPE that occurs under high load.
 *
 * The FastUtil LongOpenHashSet iterator can have its internal 'wrapped' field
 * become null during iteration with remove() when rehashing occurs. This causes
 * a NullPointerException in nextLong(). We catch this and gracefully return.
 */
public class UnloadImagesMethodVisitor extends MethodVisitor {

    private final String className;
    private final Label tryStart = new Label();
    private final Label tryEnd = new Label();
    private final Label catchHandler = new Label();
    private boolean visitedCode = false;

    public UnloadImagesMethodVisitor(MethodVisitor mv, String className) {
        super(Opcodes.ASM9, mv);
        this.className = className;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        visitedCode = true;

        // Register the try-catch block for NullPointerException
        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/NullPointerException");

        // Emit the try block start label
        mv.visitLabel(tryStart);
    }

    @Override
    public void visitInsn(int opcode) {
        // Intercept RETURN instructions to end the try block and add handler
        if (visitedCode && opcode == Opcodes.RETURN) {
            // End the try block before the return
            mv.visitLabel(tryEnd);

            // Jump over the catch handler to the actual return
            Label afterCatch = new Label();
            mv.visitJumpInsn(Opcodes.GOTO, afterCatch);

            // Emit the catch handler
            mv.visitLabel(catchHandler);
            // Stack has the exception on it, we need to handle it

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
            mv.visitLdcInsn("WorldMapTracker.unloadImages() failed - recovered gracefully (Issue #16)");
            mv.visitMethodInsn(
                    Opcodes.INVOKEINTERFACE,
                    "com/google/common/flogger/LoggingApi",
                    "log",
                    "(Ljava/lang/String;)V",
                    true
            );
            mv.visitInsn(Opcodes.POP);

            // Return normally (void method)
            mv.visitInsn(Opcodes.RETURN);

            // Label for normal flow after try block
            mv.visitLabel(afterCatch);
        }

        super.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        // Increase stack size to accommodate our additions
        super.visitMaxs(Math.max(maxStack, 4), maxLocals);
    }
}
