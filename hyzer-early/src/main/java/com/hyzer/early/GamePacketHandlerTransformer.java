package com.hyzer.early;

import com.hyzer.early.config.EarlyConfigManager;
import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzer.early.EarlyLogger.*;

/**
 * Hyzer Early Plugin - GamePacketHandler Bytecode Transformer
 *
 * Mitigates NPE crashes when stale player refs cause getComponent() to return null.
 * We guard the ClientMovement lambda to skip the task instead of crashing the world.
 */
public class GamePacketHandlerTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler";

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public byte[] transform(String className, String packageName, byte[] classBytes) {
        if (!className.equals(TARGET_CLASS)) {
            return classBytes;
        }

        if (!EarlyConfigManager.getInstance().isTransformerEnabled("gamePacketHandler")) {
            info("GamePacketHandlerTransformer DISABLED by config");
            return classBytes;
        }

        separator();
        info("Transforming GamePacketHandler...");
        verbose("Adding ClientMovement lambda NPE guard");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new GamePacketHandlerVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("GamePacketHandler transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform GamePacketHandler!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}
