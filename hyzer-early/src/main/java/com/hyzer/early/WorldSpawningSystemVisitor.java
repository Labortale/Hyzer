package com.hyzer.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.hyzer.early.EarlyLogger.verbose;

/**
 * ASM ClassVisitor for WorldSpawningSystem transformation.
 * Wraps pickRandomChunk() in a try-catch for IllegalStateException.
 */
public class WorldSpawningSystemVisitor extends ClassVisitor {

    private static final String PICK_RANDOM_CHUNK_METHOD = "pickRandomChunk";
    private static final String TICK_METHOD = "tick";
    private static final String PICK_RANDOM_CHUNK_DESCRIPTOR =
            "(Lcom/hypixel/hytale/server/spawning/world/WorldEnvironmentSpawnData;" +
            "Lcom/hypixel/hytale/server/spawning/world/WorldNPCSpawnStat;" +
            "Lcom/hypixel/hytale/server/spawning/world/WorldSpawnData;" +
            "Lcom/hypixel/hytale/component/Store;)" +
            "Lcom/hypixel/hytale/component/Ref;";
    private static final String TICK_DESCRIPTOR =
            "(FILcom/hypixel/hytale/component/Store;)V";

    public WorldSpawningSystemVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.equals(PICK_RANDOM_CHUNK_METHOD) && descriptor.equals(PICK_RANDOM_CHUNK_DESCRIPTOR)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying InvalidRef protection...");
            return new PickRandomChunkMethodVisitor(mv);
        }

        if (name.equals(TICK_METHOD) && descriptor.equals(TICK_DESCRIPTOR)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying spawn tick null guard...");
            return new WorldSpawningSystemTickMethodVisitor(mv);
        }

        return mv;
    }
}
