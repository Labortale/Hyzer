package com.hyzenkernel.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.hyzenkernel.early.EarlyLogger.verbose;

/**
 * ASM ClassVisitor for PrefabLoader transformation.
 * Intercepts loadPrefabBufferAt to add missing-prefab guard.
 */
public class PrefabLoaderVisitor extends ClassVisitor {

    private static final String LOAD_METHOD = "loadPrefabBufferAt";
    private static final String LOAD_DESC =
            "(Ljava/nio/file/Path;)Lcom/hypixel/hytale/server/core/prefab/selection/buffer/impl/PrefabBuffer;";

    public PrefabLoaderVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.equals(LOAD_METHOD) && descriptor.equals(LOAD_DESC)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying missing-prefab guard...");
            return new PrefabLoaderMethodVisitor(mv, "com/hypixel/hytale/builtin/hytalegenerator/assets/props/prefabprop/PrefabLoader");
        }

        return mv;
    }
}
