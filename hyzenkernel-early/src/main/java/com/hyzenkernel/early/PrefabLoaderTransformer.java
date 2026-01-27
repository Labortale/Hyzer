package com.hyzenkernel.early;

import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * HyzenKernel Early Plugin - PrefabLoader Bytecode Transformer
 *
 * Avoids noisy stacktraces when a prefab path is missing (vanilla assets).
 * We add an existence check before reading the file so missing prefabs
 * return null quietly and don't spam logs for every new instance seed.
 */
public class PrefabLoaderTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.builtin.hytalegenerator.assets.props.prefabprop.PrefabLoader";

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public byte[] transform(String className, String packageName, byte[] classBytes) {
        if (!className.equals(TARGET_CLASS)) {
            return classBytes;
        }

        separator();
        info("Transforming PrefabLoader...");
        verbose("Adding missing-prefab existence guard in loadPrefabBufferAt()");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new PrefabLoaderVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("PrefabLoader transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform PrefabLoader!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}
