package com.hyzer.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that replaces World.execute(Runnable) to avoid throwing
 * when the world stops accepting tasks. If acceptingTasks is false, the task
 * is dropped silently to prevent async spam during shutdown windows.
 */
public class WorldExecuteMethodVisitor extends MethodVisitor {

    private final String className;
    private final MethodVisitor target;

    public WorldExecuteMethodVisitor(MethodVisitor methodVisitor, String className) {
        super(Opcodes.ASM9, null);
        this.target = methodVisitor;
        this.className = className;
    }

    @Override
    public void visitCode() {
        generateFixedMethod();
    }

    private void generateFixedMethod() {
        Label acceptTasks = new Label();

        target.visitCode();

        // if (!this.acceptingTasks.get()) return;
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "acceptingTasks", "Ljava/util/concurrent/atomic/AtomicBoolean;");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicBoolean", "get", "()Z", false);
        target.visitJumpInsn(Opcodes.IFNE, acceptTasks);
        // Log dropped task for diagnostics
        target.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "com/hypixel/hytale/logger/HytaleLogger",
            "getLogger",
            "()Lcom/hypixel/hytale/logger/HytaleLogger;",
            false
        );
        target.visitFieldInsn(Opcodes.GETSTATIC, "java/util/logging/Level", "WARNING", "Ljava/util/logging/Level;");
        target.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "com/hypixel/hytale/logger/HytaleLogger",
            "at",
            "(Ljava/util/logging/Level;)Lcom/hypixel/hytale/logger/HytaleLogger$Api;",
            false
        );
        target.visitLdcInsn("[Hyzer] World.execute dropped task because acceptingTasks=false");
        target.visitMethodInsn(
            Opcodes.INVOKEINTERFACE,
            "com/google/common/flogger/LoggingApi",
            "log",
            "(Ljava/lang/String;)V",
            true
        );
        target.visitInsn(Opcodes.RETURN);

        // this.taskQueue.offer(command);
        target.visitLabel(acceptTasks);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "taskQueue", "Ljava/util/Deque;");
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Deque", "offer", "(Ljava/lang/Object;)Z", true);
        target.visitInsn(Opcodes.POP);
        target.visitInsn(Opcodes.RETURN);

        target.visitMaxs(8, 2);
        target.visitEnd();
    }

    // Override all visit methods to ignore original bytecode
    @Override
    public void visitInsn(int opcode) {
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
    }

    @Override
    public void visitLabel(Label label) {
    }

    @Override
    public void visitLdcInsn(Object value) {
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor,
            org.objectweb.asm.Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
    }

    @Override
    public void visitLineNumber(int line, Label start) {
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature,
            Label start, Label end, int index) {
    }
}
