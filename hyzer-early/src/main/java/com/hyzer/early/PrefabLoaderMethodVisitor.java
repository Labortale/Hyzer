package com.hyzer.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that replaces PrefabLoader.loadPrefabBufferAt(Path)
 * to add a Files.exists() guard before reading the prefab file.
 */
public class PrefabLoaderMethodVisitor extends MethodVisitor {

    private final String className;
    private final MethodVisitor target;

    private static final String PREFAB_BUFFER =
            "com/hypixel/hytale/server/core/prefab/selection/buffer/impl/PrefabBuffer";
    private static final String BSON_UTIL = "com/hypixel/hytale/server/core/util/BsonUtil";
    private static final String BSON_DOC = "org/bson/BsonDocument";
    private static final String BSON_DESERIALIZER =
            "com/hypixel/hytale/server/core/prefab/selection/buffer/BsonPrefabBufferDeserializer";
    private static final String LOGGER_UTIL = "com/hypixel/hytale/builtin/hytalegenerator/LoggerUtil";
    private static final String EXCEPTION_UTIL = "com/hypixel/hytale/common/util/ExceptionUtil";

    public PrefabLoaderMethodVisitor(MethodVisitor mv, String className) {
        super(Opcodes.ASM9, null);
        this.target = mv;
        this.className = className;
    }

    @Override
    public void visitCode() {
        generateFixedMethod();
    }

    private void generateFixedMethod() {
        Label hasJson = new Label();
        Label exists = new Label();
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchHandler = new Label();
        Label afterTry = new Label();

        target.visitCode();

        // if (!hasJsonExtension(filePath)) return null;
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            className,
            "hasJsonExtension",
            "(Ljava/nio/file/Path;)Z",
            false
        );
        target.visitJumpInsn(Opcodes.IFNE, hasJson);
        target.visitInsn(Opcodes.ACONST_NULL);
        target.visitInsn(Opcodes.ARETURN);

        // if (!Files.exists(filePath)) return null;
        target.visitLabel(hasJson);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitInsn(Opcodes.ICONST_0);
        target.visitTypeInsn(Opcodes.ANEWARRAY, "java/nio/file/LinkOption");
        target.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/nio/file/Files",
            "exists",
            "(Ljava/nio/file/Path;[Ljava/nio/file/LinkOption;)Z",
            false
        );
        target.visitJumpInsn(Opcodes.IFNE, exists);
        target.visitInsn(Opcodes.ACONST_NULL);
        target.visitInsn(Opcodes.ARETURN);

        // try { ... } catch (Exception e) { ... }
        target.visitLabel(exists);
        target.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/Exception");
        target.visitLabel(tryStart);

        // BsonDocument prefabAsBson = BsonUtil.readDocumentNow(filePath);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            BSON_UTIL,
            "readDocumentNow",
            "(Ljava/nio/file/Path;)L" + BSON_DOC + ";",
            false
        );
        target.visitVarInsn(Opcodes.ASTORE, 1);

        // if (prefabAsBson == null) return null;
        target.visitVarInsn(Opcodes.ALOAD, 1);
        Label notNull = new Label();
        target.visitJumpInsn(Opcodes.IFNONNULL, notNull);
        target.visitInsn(Opcodes.ACONST_NULL);
        target.visitInsn(Opcodes.ARETURN);

        // return BsonPrefabBufferDeserializer.INSTANCE.deserialize(filePath, prefabAsBson);
        target.visitLabel(notNull);
        target.visitFieldInsn(
            Opcodes.GETSTATIC,
            BSON_DESERIALIZER,
            "INSTANCE",
            "L" + BSON_DESERIALIZER + ";"
        );
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            BSON_DESERIALIZER,
            "deserialize",
            "(Ljava/nio/file/Path;L" + BSON_DOC + ";)L" + PREFAB_BUFFER + ";",
            false
        );
        target.visitInsn(Opcodes.ARETURN);

        target.visitLabel(tryEnd);
        target.visitJumpInsn(Opcodes.GOTO, afterTry);

        // catch (Exception e) { log severe; return null; }
        target.visitLabel(catchHandler);
        target.visitVarInsn(Opcodes.ASTORE, 1);

        // String msg = "Exception thrown by HytaleGenerator while loading a PrefabBuffer for " + filePath + ":\n" + ExceptionUtil.toStringWithStack(e);
        target.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        target.visitInsn(Opcodes.DUP);
        target.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        target.visitLdcInsn("Exception thrown by HytaleGenerator while loading a PrefabBuffer for ");
        target.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            false
        );
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(Ljava/lang/Object;)Ljava/lang/StringBuilder;",
            false
        );
        target.visitLdcInsn(":\n");
        target.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            false
        );
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            EXCEPTION_UTIL,
            "toStringWithStack",
            "(Ljava/lang/Throwable;)Ljava/lang/String;",
            false
        );
        target.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            false
        );
        target.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "toString",
            "()Ljava/lang/String;",
            false
        );
        target.visitVarInsn(Opcodes.ASTORE, 2);

        target.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            LOGGER_UTIL,
            "getLogger",
            "()Ljava/util/logging/Logger;",
            false
        );
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/util/logging/Logger",
            "severe",
            "(Ljava/lang/String;)V",
            false
        );

        target.visitInsn(Opcodes.ACONST_NULL);
        target.visitInsn(Opcodes.ARETURN);

        target.visitLabel(afterTry);
        target.visitInsn(Opcodes.ACONST_NULL);
        target.visitInsn(Opcodes.ARETURN);

        target.visitMaxs(6, 3);
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
