package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that replaces World.execute(Runnable) to avoid throwing
 * when the world is already shut down. If acceptingTasks is false and alive
 * is false, the task is dropped silently; otherwise we preserve the original
 * IllegalThreadStateException behavior.
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
        Label throwException = new Label();

        target.visitCode();

        // if (!this.acceptingTasks.get()) { ... }
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "acceptingTasks", "Ljava/util/concurrent/atomic/AtomicBoolean;");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicBoolean", "get", "()Z", false);
        target.visitJumpInsn(Opcodes.IFNE, acceptTasks);

        // if (!this.alive.get()) return;
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "alive", "Ljava/util/concurrent/atomic/AtomicBoolean;");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicBoolean", "get", "()Z", false);
        target.visitJumpInsn(Opcodes.IFNE, throwException);
        target.visitInsn(Opcodes.RETURN);

        // throw new SkipSentryException(new IllegalThreadStateException("World thread is not accepting tasks: " + name + ", " + getThread()))
        target.visitLabel(throwException);
        target.visitTypeInsn(Opcodes.NEW, "com/hypixel/hytale/logger/sentry/SkipSentryException");
        target.visitInsn(Opcodes.DUP);
        target.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalThreadStateException");
        target.visitInsn(Opcodes.DUP);
        target.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        target.visitInsn(Opcodes.DUP);
        target.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        target.visitLdcInsn("World thread is not accepting tasks: ");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "name", "Ljava/lang/String;");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        target.visitLdcInsn(", ");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, className, "getThread", "()Ljava/lang/Thread;", false);
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        target.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/IllegalThreadStateException",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        );
        target.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "com/hypixel/hytale/logger/sentry/SkipSentryException",
            "<init>",
            "(Ljava/lang/Throwable;)V",
            false
        );
        target.visitInsn(Opcodes.ATHROW);

        // this.taskQueue.offer(command);
        target.visitLabel(acceptTasks);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "taskQueue", "Ljava/util/Deque;");
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Deque", "offer", "(Ljava/lang/Object;)Z", true);
        target.visitInsn(Opcodes.POP);
        target.visitInsn(Opcodes.RETURN);

        target.visitMaxs(6, 2);
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
