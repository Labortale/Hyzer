package com.hyzenkernel.early;

import com.hyzenkernel.early.config.EarlyConfigManager;
import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * HyzenKernel Early Plugin - BlockHealthSystem Bytecode Transformer
 *
 * Mitigates NPE crashes when ChunkTracker is null for a player ref.
 * We guard the tick method to skip the update instead of crashing the world.
 */
public class BlockHealthSystemTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.server.core.modules.blockhealth.BlockHealthModule$BlockHealthSystem";

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public byte[] transform(String className, String packageName, byte[] classBytes) {
        if (!className.equals(TARGET_CLASS)) {
            return classBytes;
        }

        if (!EarlyConfigManager.getInstance().isTransformerEnabled("blockHealthSystem")) {
            info("BlockHealthSystemTransformer DISABLED by config");
            return classBytes;
        }

        separator();
        info("Transforming BlockHealthSystem...");
        verbose("Adding NPE guard in tick()");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new BlockHealthSystemVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("BlockHealthSystem transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform BlockHealthSystem!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}
