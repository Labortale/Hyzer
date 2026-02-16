package com.hyzer.early;

import com.hyzer.early.config.EarlyConfigManager;
import com.hypixel.hytale.plugin.early.ClassTransformer;

import static com.hyzer.early.EarlyLogger.*;

/**
 * Hyzer Early Plugin - RemovalSystem Transformer
 *
 * Reserved for legacy behavior. Currently no-ops to preserve vanilla portal removal timing.
 */
public class RemovalSystemTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.builtin.instances.removal.RemovalSystem";

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public byte[] transform(String className, String packageName, byte[] classBytes) {
        if (!className.equals(TARGET_CLASS)) {
            return classBytes;
        }

        if (!EarlyConfigManager.getInstance().isTransformerEnabled("staticSharedInstances")) {
            info("RemovalSystemTransformer DISABLED by config");
            return classBytes;
        }

        // Preserve vanilla removal behavior for portal instances.
        // Shared-world reuse is handled elsewhere; we don't want to block normal closure timers.
        info("RemovalSystemTransformer SKIPPED to preserve vanilla portal removal behavior");
        return classBytes;
    }
}
