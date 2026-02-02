package com.hyzenkernel.optimization;

import com.hyzenkernel.config.HyzenKernelConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;

public class ViewRadiusAdjuster {

    private final HytaleLogger logger;
    private final HyzenKernelConfig.OptimizationConfig config;
    private final int initialViewRadius;

    public ViewRadiusAdjuster(HytaleLogger logger, HyzenKernelConfig.OptimizationConfig config) {
        this.logger = logger;
        this.config = config;
        this.initialViewRadius = HytaleServer.get().getConfig().getMaxViewRadius();
    }

    public void checkAndAdjust() {
        if (!config.enabled) {
            return;
        }
        if (config.tps == null || !config.tps.enabled) {
            return;
        }

        double currentTps = TpsMonitor.getLowestWorldTps();
        int currentRadius = HytaleServer.get().getConfig().getMaxViewRadius();
        int minRadius = Math.max(config.minViewRadius, 1);
        int maxRadius = Math.max(config.maxViewRadius, minRadius);

        if (currentTps < config.tps.lowTpsThreshold) {
            if (currentRadius > minRadius) {
                int newRadius = Math.max(minRadius, currentRadius - 1);
                applyViewRadius(newRadius, "TPS bajo detectado (" + String.format("%.2f", currentTps) + "). Reduciendo vision.");
            }
        } else if (currentTps > config.tps.recoveryTpsThreshold) {
            if (currentRadius < maxRadius && currentRadius < initialViewRadius) {
                int newRadius = Math.min(initialViewRadius, currentRadius + 1);
                applyViewRadius(newRadius, "TPS estable (" + String.format("%.2f", currentTps) + "). Aumentando vision.");
            }
        }
    }

    private void applyViewRadius(int radius, String reason) {
        HytaleServer.get().getConfig().setMaxViewRadius(radius);
        logger.atWarning().log("%s Nuevo radio: %d chunks.", reason, radius);
    }

    public void restore() {
        HytaleServer.get().getConfig().setMaxViewRadius(initialViewRadius);
    }
}
