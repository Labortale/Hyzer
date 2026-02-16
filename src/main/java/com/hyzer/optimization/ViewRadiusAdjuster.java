package com.hyzer.optimization;

import com.hyzer.config.HyzerConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;

public class ViewRadiusAdjuster {

    private final HytaleLogger logger;
    private final HyzerConfig.OptimizationConfig config;
    private final int initialViewRadius;

    public ViewRadiusAdjuster(HytaleLogger logger, HyzerConfig.OptimizationConfig config) {
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
        if (Universe.get().getPlayerCount() <= 0) {
            return;
        }

        double currentTps = TpsMonitor.getLowestWorldTps();
        int currentRadius = HytaleServer.get().getConfig().getMaxViewRadius();
        int minRadius = Math.max(config.minViewRadius, 1);
        int maxRadius = Math.max(config.maxViewRadius, minRadius);

        if (currentTps < config.tps.lowTpsThreshold) {
            if (currentRadius > minRadius) {
                int newRadius = Math.max(minRadius, currentRadius - 1);
                applyViewRadius(newRadius, "Low TPS detected (" + String.format("%.2f", currentTps) + "). Reducing view radius.");
            }
        } else if (currentTps > config.tps.recoveryTpsThreshold) {
            if (currentRadius < maxRadius && currentRadius < initialViewRadius) {
                int newRadius = Math.min(initialViewRadius, currentRadius + 1);
                applyViewRadius(newRadius, "TPS stable (" + String.format("%.2f", currentTps) + "). Increasing view radius.");
            }
        }
    }

    private void applyViewRadius(int radius, String reason) {
        HytaleServer.get().getConfig().setMaxViewRadius(radius);
        logger.atWarning().log("%s New radius: %d chunks.", reason, radius);
    }

    public void restore() {
        HytaleServer.get().getConfig().setMaxViewRadius(initialViewRadius);
    }
}
