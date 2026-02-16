package com.hyzer.systems;

import com.hyzer.Hyzer;
import com.hyzer.config.ConfigManager;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * SharedInstancePersistenceSystem
 *
 * Minimal runtime guard for shared portal instances (instance-shared-* and instance-Endgame_*):
 * - Forces DeleteOnRemove/DeleteOnUniverseStart to false
 * - Marks config changed so it persists to disk
 *
 * This keeps vanilla instance behavior while ensuring terrain is not deleted.
 */
public class SharedInstancePersistenceSystem extends TickingSystem<ChunkStore> {
    private static final String SHARED_PREFIX = "instance-shared-";
    private static final String ENDGAME_PREFIX = "instance-Endgame_";
    private final Hyzer plugin;
    private final Set<String> loggedWorlds = ConcurrentHashMap.newKeySet();
    private boolean loggedOnce = false;
    private static final int LOGGED_WORLD_CLEANUP_THRESHOLD = 512;

    public SharedInstancePersistenceSystem(Hyzer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void tick(float dt, int systemIndex, Store<ChunkStore> store) {
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        String worldName = world.getName();
        if (worldName == null) {
            return;
        }
        if (!worldName.startsWith(SHARED_PREFIX) && !worldName.startsWith(ENDGAME_PREFIX)) {
            return;
        }

        if (!loggedOnce) {
            plugin.getLogger().at(Level.INFO).log(
                "[SharedInstancePersistence] Active - preserving shared portal instance terrain"
            );
            loggedOnce = true;
        }

        WorldConfig config = world.getWorldConfig();
        if (config == null) {
            return;
        }

        boolean changed = false;
        if (config.isDeleteOnRemove()) {
            config.setDeleteOnRemove(false);
            changed = true;
        }
        if (config.isDeleteOnUniverseStart()) {
            config.setDeleteOnUniverseStart(false);
            changed = true;
        }

        if (changed) {
            config.markChanged();
            if (ConfigManager.getInstance().logSanitizerActions() && loggedWorlds.add(worldName)) {
                plugin.getLogger().at(Level.INFO).log(
                    "[SharedInstancePersistence] Set persistent flags for " + worldName
                );
            }
        }

        if (loggedWorlds.size() > LOGGED_WORLD_CLEANUP_THRESHOLD) {
            var worldsByName = Universe.get().getWorlds();
            loggedWorlds.removeIf(name -> !worldsByName.containsKey(name));
        }
    }
}
