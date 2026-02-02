package com.hyzenkernel.optimization;

import com.hyzenkernel.config.HyzenKernelConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.thread.TickingThread;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class TpsAdjuster {

    public static final String DEFAULT_WORLD = "__DEFAULT";

    private final HytaleLogger logger;
    private final HyzenKernelConfig.TpsAdjusterConfig config;
    private long lastPlayerSeenAt;

    public TpsAdjuster(HytaleLogger logger, HyzenKernelConfig.TpsAdjusterConfig config) {
        this.logger = logger.getSubLogger("TpsAdjuster");
        this.config = config;
    }

    public boolean execute() {
        long now = System.nanoTime();

        if (getPlayerCount() > 0) {
            this.lastPlayerSeenAt = now;
        }

        int targetTps = config.tpsLimit;
        if (now - lastPlayerSeenAt > config.emptyLimitDelaySeconds * 1_000_000_000L) {
            targetTps = config.tpsLimitEmpty;
        }

        return setTps(targetTps);
    }

    private boolean setTps(int tps) {
        Set<String> onlyWorlds = new HashSet<>();
        if (config.onlyWorlds != null) {
            for (String name : config.onlyWorlds) {
                if (name != null && !name.isBlank()) {
                    onlyWorlds.add(name);
                }
            }
        }

        if (onlyWorlds.contains(DEFAULT_WORLD)) {
            onlyWorlds.add(Universe.get().getDefaultWorld().getName());
        }

        boolean change = false;
        for (var entry : Universe.get().getWorlds().entrySet()) {
            if (!onlyWorlds.isEmpty() && !onlyWorlds.contains(entry.getKey())) {
                continue;
            }

            World world = entry.getValue();
            if (world.getTps() != tps) {
                change = true;
                logger.atInfo().log("Setting TPS of world %s to %d", world.getName(), tps);
                CompletableFuture.runAsync(() -> world.setTps(tps), world);
            }
        }

        return change;
    }

    public boolean restore() {
        return setTps(TickingThread.TPS);
    }

    private int getPlayerCount() {
        int universePlayerCount = Universe.get().getPlayerCount();
        int worldSumPlayerCount = 0;
        for (var worldEntry : Universe.get().getWorlds().entrySet()) {
            worldSumPlayerCount += worldEntry.getValue().getPlayerCount();
        }
        return Math.max(universePlayerCount, worldSumPlayerCount);
    }
}
