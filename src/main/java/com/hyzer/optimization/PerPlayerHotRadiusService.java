package com.hyzer.optimization;

import com.hyzer.config.HyzerConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.List;

public class PerPlayerHotRadiusService {

    private final HytaleLogger logger;
    private final HyzerConfig.PerPlayerRadiusConfig config;

    private static volatile Method mGetChunkTracker;
    private static volatile Method mSetHotRadius;
    private static volatile Method mGetHotRadius;

    private int currentTargetRadius;

    public PerPlayerHotRadiusService(@Nonnull HytaleLogger logger, @Nonnull HyzerConfig.PerPlayerRadiusConfig config) {
        this.logger = logger.getSubLogger("PerPlayerRadius");
        this.config = config;
        this.currentTargetRadius = config.maxRadius;
    }

    public void checkAndAdjust() {
        if (!config.enabled) {
            return;
        }

        float currentTps = (float) TpsMonitor.getLowestWorldTps();
        int targetRadius = calculateTargetRadius(currentTps);

        if (targetRadius != currentTargetRadius) {
            int applied = applyToAllPlayers(targetRadius);
            if (applied > 0) {
                logger.atInfo().log("Adjusted per-player hot radius: %d -> %d (TPS: %.1f, players: %d)",
                        currentTargetRadius, targetRadius, currentTps, applied);
            }
            currentTargetRadius = targetRadius;
        }
    }

    private int calculateTargetRadius(float tps) {
        if (tps <= config.tpsLow) {
            return config.minRadius;
        } else if (tps >= config.tpsHigh) {
            return config.maxRadius;
        } else {
            float ratio = (tps - config.tpsLow) / (config.tpsHigh - config.tpsLow);
            int range = config.maxRadius - config.minRadius;
            return config.minRadius + (int) (range * ratio);
        }
    }

    private int applyToAllPlayers(int targetRadius) {
        List<PlayerRef> players = Universe.get().getPlayers();
        if (players == null || players.isEmpty()) {
            return 0;
        }

        int applied = 0;
        int clamped = clamp(targetRadius, config.minRadius, config.maxRadius);

        for (PlayerRef playerRef : players) {
            if (playerRef != null && trySetHotRadius(playerRef, clamped)) {
                applied++;
            }
        }

        return applied;
    }

    private boolean trySetHotRadius(PlayerRef playerRef, int value) {
        try {
            Object tracker = getChunkTracker(playerRef);
            if (tracker == null) {
                return false;
            }

            if (mSetHotRadius == null) {
                mSetHotRadius = tracker.getClass().getMethod("setMaxHotLoadedChunksRadius", int.class);
                mSetHotRadius.setAccessible(true);
            }

            if (mGetHotRadius == null) {
                mGetHotRadius = tracker.getClass().getMethod("getMaxHotLoadedChunksRadius");
                mGetHotRadius.setAccessible(true);
            }

            int current = ((Number) mGetHotRadius.invoke(tracker)).intValue();
            if (current == value) {
                return false;
            }

            mSetHotRadius.invoke(tracker, value);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private Object getChunkTracker(PlayerRef playerRef) {
        try {
            if (mGetChunkTracker == null) {
                mGetChunkTracker = playerRef.getClass().getMethod("getChunkTracker");
                mGetChunkTracker.setAccessible(true);
            }
            return mGetChunkTracker.invoke(playerRef);
        } catch (Throwable e) {
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getCurrentTargetRadius() {
        return currentTargetRadius;
    }
}
