package com.hyzenkernel.optimization;

import com.hypixel.hytale.metrics.metric.HistoricMetric;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.commands.world.perf.WorldPerfCommand;

public final class TpsMonitor {
    private TpsMonitor() {
    }

    public static double getLowestWorldTps() {
        double minTps = 20.0;
        boolean hasWorlds = false;

        for (World world : Universe.get().getWorlds().values()) {
            double worldTps = getWorldTps(world);
            if (worldTps < minTps) {
                minTps = worldTps;
            }
            hasWorlds = true;
        }

        return hasWorlds ? minTps : 20.0;
    }

    public static double getWorldTps(World world) {
        long tickStepNanos = world.getTickStepNanos();
        HistoricMetric metrics = world.getBufferedTickLengthMetricSet();
        return WorldPerfCommand.tpsFromDelta(metrics.getAverage(0), tickStepNanos);
    }
}
