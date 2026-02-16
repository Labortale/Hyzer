package com.hyzer.optimization;

import com.hyzer.config.HyzerConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box2D;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkFlag;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ecs.ChunkUnloadEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveChunkUnloader {

    private final HytaleLogger logger;
    private final HyzerConfig.OptimizationConfig config;
    private final Map<String, Long2LongOpenHashMap> outOfRangeSinceByWorld = new ConcurrentHashMap<>();

    public ActiveChunkUnloader(HytaleLogger logger, HyzerConfig.OptimizationConfig config) {
        this.logger = logger.getSubLogger("ActiveChunkUnloader");
        this.config = config;
    }

    public void execute() {
        if (config == null || !config.enabled || config.chunkUnloader == null || !config.chunkUnloader.enabled) {
            return;
        }

        // Clean cached state for worlds that no longer exist
        var worldsByName = Universe.get().getWorlds();
        outOfRangeSinceByWorld.keySet().removeIf(name -> !worldsByName.containsKey(name));

        int baseViewRadius = Math.max(config.maxViewRadius, 1);
        int offset = Math.max(config.chunkUnloader.unloadDistanceOffset, 0);
        int safeRadius = Math.max(baseViewRadius + offset, 2);

        for (World world : worldsByName.values()) {
            world.execute(() -> unloadWorld(world, safeRadius));
        }
    }

    private void unloadWorld(World world, int safeRadius) {
        if (!world.getWorldConfig().canUnloadChunks()) {
            return;
        }

        ChunkStore chunkStore = world.getChunkStore();
        if (chunkStore.getLoadedChunksCount() < config.chunkUnloader.minLoadedChunks) {
            return;
        }

        List<Long> playerChunkIndexes = collectPlayerChunkIndexes(world.getPlayerRefs());
        Long2LongOpenHashMap outOfRangeSince = outOfRangeSinceByWorld.computeIfAbsent(world.getName(), key -> {
            Long2LongOpenHashMap map = new Long2LongOpenHashMap();
            map.defaultReturnValue(0L);
            return map;
        });

        long now = System.nanoTime();
        int unloaded = 0;
        LongSet chunkIndexes = chunkStore.getChunkIndexes();
        LongIterator iterator = chunkIndexes.iterator();

        while (iterator.hasNext()) {
            long chunkIndex = iterator.nextLong();
            Ref<ChunkStore> chunkRef = chunkStore.getChunkReference(chunkIndex);
            if (chunkRef == null || !chunkRef.isValid()) {
                outOfRangeSince.remove(chunkIndex);
                continue;
            }

            WorldChunk worldChunk = chunkStore.getStore().getComponent(chunkRef, WorldChunk.getComponentType());
            if (worldChunk == null) {
                outOfRangeSince.remove(chunkIndex);
                continue;
            }

            if (worldChunk.shouldKeepLoaded() || worldChunk.getNeedsSaving() || isInKeepLoadedRegion(world, worldChunk)) {
                outOfRangeSince.remove(chunkIndex);
                continue;
            }

            if (isChunkNeeded(playerChunkIndexes, chunkIndex, safeRadius)) {
                outOfRangeSince.remove(chunkIndex);
                continue;
            }

            long firstOut = outOfRangeSince.get(chunkIndex);
            if (firstOut == 0L) {
                outOfRangeSince.put(chunkIndex, now);
                continue;
            }

            long delayNanos = Math.max(config.chunkUnloader.unloadDelaySeconds, 1) * 1_000_000_000L;
            if (now - firstOut < delayNanos) {
                continue;
            }

            if (worldChunk.is(ChunkFlag.TICKING)) {
                worldChunk.setFlag(ChunkFlag.TICKING, false);
                outOfRangeSince.put(chunkIndex, now);
                continue;
            }

            ChunkUnloadEvent event = new ChunkUnloadEvent(worldChunk);
            chunkStore.getStore().invoke(chunkRef, event);
            if (event.isCancelled()) {
                if (event.willResetKeepAlive()) {
                    worldChunk.resetKeepAlive();
                }
                outOfRangeSince.remove(chunkIndex);
                continue;
            }

            chunkStore.remove(chunkRef, RemoveReason.UNLOAD);
            outOfRangeSince.remove(chunkIndex);
            unloaded++;

            if (unloaded >= config.chunkUnloader.maxUnloadsPerRun) {
                break;
            }
        }

        if (unloaded > 0) {
            logger.atInfo().log("[World %s] Optimization: Unloaded %d inactive chunks.", world.getName(), unloaded);
        }
    }

    private List<Long> collectPlayerChunkIndexes(java.util.Collection<PlayerRef> players) {
        List<Long> playerChunkIndexes = new ArrayList<>();
        if (players == null || players.isEmpty()) {
            return playerChunkIndexes;
        }

        for (PlayerRef player : players) {
            if (player == null) {
                continue;
            }
            Transform transform = player.getTransform();
            if (transform == null) {
                continue;
            }
            int chunkX = ChunkUtil.chunkCoordinate(transform.getPosition().getX());
            int chunkZ = ChunkUtil.chunkCoordinate(transform.getPosition().getZ());
            playerChunkIndexes.add(ChunkUtil.indexChunk(chunkX, chunkZ));
        }

        return playerChunkIndexes;
    }

    private boolean isChunkNeeded(List<Long> playerChunkIndexes, long chunkIndex, int safeRadius) {
        for (long playerChunkIndex : playerChunkIndexes) {
            if (getChebyshevDistance(chunkIndex, playerChunkIndex) <= safeRadius) {
                return true;
            }
        }
        return false;
    }

    private int getChebyshevDistance(long index1, long index2) {
        int x1 = ChunkUtil.xOfChunkIndex(index1);
        int z1 = ChunkUtil.zOfChunkIndex(index1);
        int x2 = ChunkUtil.xOfChunkIndex(index2);
        int z2 = ChunkUtil.zOfChunkIndex(index2);

        return Math.max(Math.abs(x1 - x2), Math.abs(z1 - z2));
    }

    private boolean isInKeepLoadedRegion(World world, WorldChunk worldChunk) {
        Box2D keepLoaded = world.getWorldConfig().getChunkConfig().getKeepLoadedRegion();
        if (keepLoaded == null) {
            return false;
        }

        int minX = ChunkUtil.minBlock(worldChunk.getX());
        int minZ = ChunkUtil.minBlock(worldChunk.getZ());
        int maxX = ChunkUtil.maxBlock(worldChunk.getX());
        int maxZ = ChunkUtil.maxBlock(worldChunk.getZ());
        return maxX >= keepLoaded.min.x && minX <= keepLoaded.max.x && maxZ >= keepLoaded.min.y && minZ <= keepLoaded.max.y;
    }
}
