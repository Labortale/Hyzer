# Hyzer

Essential bug fixes for Hytale Early Access servers. Prevents crashes, player kicks, and desync issues caused by known bugs in Hytale's core systems.

Hyzer is a fork of HyFixes for Hyzen.net (A Hytale server), improved, cleaner, more organized, and optimized. Credits: https://github.com/John-Willikers/hyfixes

---

## Two Plugins, One Solution

Hyzer consists of **two complementary plugins** that work together to fix different types of bugs:

| Plugin | File | Purpose |
|--------|------|---------|
| **Runtime Plugin** | `Hyzer-X.X.X.jar` | Fixes bugs at runtime using sanitizers and event hooks |
| **Early Plugin** | `Hyzer-early-X.X.X.jar` | Fixes deep core bugs via bytecode transformation at class load |

### Why Two Plugins?

Some Hytale bugs occur in code paths that cannot be intercepted at runtime. The **Early Plugin** uses Java bytecode transformation (ASM) to rewrite buggy methods *before* they're loaded, allowing us to fix issues deep in Hytale's networking and interaction systems.

---

## Quick Start

### Runtime Plugin (Required)

1. Download `Hyzer-X.X.X.jar` from [Releases](https://github.com/Labortale/Hyzer/releases)
2. Place in your server's `mods/` directory
3. Restart the server

### Early Plugin (Recommended)

1. Download `Hyzer-early-X.X.X.jar` from [Releases](https://github.com/Labortale/Hyzer/releases)
2. Place in your server's `earlyplugins/` directory
3. Start the server with early plugins enabled:
   - Set `ACCEPT_EARLY_PLUGINS=1` environment variable, OR
   - Press Enter when prompted at startup

---

## What Gets Fixed

### Runtime Plugin Fixes

| Bug | Severity | What Happens |
|-----|----------|--------------|
| Pickup Item Crash | Critical | World thread crashes, ALL players kicked |
| RespawnBlock Crash | Critical | Player kicked when breaking bed |
| ProcessingBench Crash | Critical | Player kicked when bench is destroyed |
| Instance Exit Crash | Critical | Player kicked when exiting dungeon |
| Shared Instance Persistence | Medium | Keeps shared portal terrain on disk between runs |
| CraftingManager Crash | Critical | Player kicked when opening bench |
| InteractionManager Crash | Critical | Player kicked during interactions |
| Quest Objective Crash | Critical | Quest system crashes |
| SpawnMarker Crash | Critical | World thread crashes during spawning |

### Early Plugin Fixes (Bytecode)

| Bug | Severity | What Happens |
|-----|----------|--------------|
| Sync Buffer Overflow | Critical | Combat/food/tool desync, 400-2500 errors/session |
| Sync Position Gap | Critical | Player kicked with "out of order" exception |
| Instance Portal Race | Critical | Player kicked when entering instance portals (retry loop fix) |
| Static Shared Instances | Medium | Reuses instance-shared worlds; only new chunks are saved (terrain persists) |
| SpawnProvider Persistence | Medium | Prevents return portal drift by persisting spawn providers |
| Shared Instance Removal Guard | Medium | Prevents shared instance worlds from being auto-removed |
| Return Portal Stack Guard | Medium | Prevents multiple return portals stacking in shared instances |
| Null SpawnController | Critical | World crashes when spawn beacons load |
| Null Spawn Parameters | Critical | World crashes in volcanic/cave biomes |
| WorldSpawningSystem Invalid Ref | Critical | World thread crash during spawn job creation (invalid chunk ref) |
| Duplicate Block Components | Critical | Player kicked when using teleporters |
| Null npcReferences (Removal) | Critical | World crashes when spawn markers are removed |
| Null npcReferences (Constructor) | Critical | ROOT CAUSE: SpawnMarkerEntity never initializes array |
| SetMemoriesCapacity Interaction | Critical | Interaction tick crashes when PlayerMemories component is unavailable |
| World.execute Shutdown Guard | Medium | Task submissions during world shutdown throw exceptions/spam logs |
| Prefab Missing Asset Guard | Medium | Prevents exceptions/spam when a prefab file is missing |
| BlockCounter Not Decrementing | Medium | Teleporter limit stuck at 5, can't place new ones |
| WorldMapTracker Iterator Crash | Critical | Server crashes every ~30 min on high-pop servers |
| ArchetypeChunk Stale Entity | Critical | IndexOutOfBoundsException when NPC systems access removed entities |
| Operation Timeout | Critical | Player kicked from network packet timeouts |
| Null UUID on Entity Remove | Critical | Crash when removing entities with null UUIDs |
| Universe Player Remove | Critical | Crash when removing players from universe |
| TickingThread Stop | Medium | Server shutdown issues causing hangs |
| CommandBuffer Component Access | Critical | Crash when accessing components through command buffers |

---

## How It Works

### Runtime Plugin

The runtime plugin registers **sanitizers** that run each server tick:

```
Server Tick
    |
    v
[PickupItemSanitizer] --> Check for null targetRef --> Mark as finished
[CraftingManagerSanitizer] --> Check for stale bench refs --> Clear them
[InteractionManagerSanitizer] --> Check for null contexts --> Remove chain
    |
    v
Hytale's Systems Run (safely, with corrupted data already cleaned up)
```

It also uses **RefSystems** that hook into entity lifecycle events to catch crashes during removal/unload operations.

### Early Plugin

The early plugin uses ASM bytecode transformation to rewrite methods at class load time:

```
Server Startup
    |
    v
JVM loads InteractionChain.class
    |
    v
[InteractionChainTransformer] intercepts class bytes
    |
    v
[PutSyncDataMethodVisitor] rewrites putInteractionSyncData()
[UpdateSyncPositionMethodVisitor] rewrites updateSyncPosition()
    |
    v
Fixed class is loaded into JVM
```

**Original buggy code:**
```java
if (adjustedIndex < 0) {
    LOGGER.severe("Attempted to store sync data...");
    return;  // DATA DROPPED!
}
```

**Transformed fixed code:**
```java
if (adjustedIndex < 0) {
    // Expand buffer backwards instead of dropping
    int expansion = -adjustedIndex;
    for (int i = 0; i < expansion; i++) {
        tempSyncData.add(0, null);
    }
    tempSyncDataOffset += adjustedIndex;
    adjustedIndex = 0;
}
// Continue processing...
```

---

## Configuration

### Optimization (Runtime Plugin)

Hyzer includes optional performance optimizations that can replace third-party plugins.
All options live under `optimization` in `mods/Hyzer/config.json`.

**Features**
- FluidFixer: disables FluidPlugin pre-process on new chunks to avoid long generation stalls.
- PerPlayerHotRadius: dynamically reduces hot/ticking chunk radius per player based on TPS.
- ViewRadiusAdjuster: gently adjusts server view radius (1 step at a time) based on TPS.
- TpsAdjuster: targets stable world TPS (defaults to 20, 5 when empty).
- ActiveChunkUnloader: safely unloads distant chunks with delay/limits and unload events.

**Example config**
```json
{
  "optimization": {
    "enabled": true,
    "minViewRadius": 2,
    "maxViewRadius": 16,
    "checkIntervalMillis": 5000,
    "tps": {
      "enabled": true,
      "lowTpsThreshold": 18.0,
      "recoveryTpsThreshold": 19.5
    },
    "tpsAdjuster": {
      "enabled": true,
      "tpsLimit": 20,
      "tpsLimitEmpty": 5,
      "emptyLimitDelaySeconds": 300,
      "checkIntervalSeconds": 5,
      "initialDelaySeconds": 30,
      "onlyWorlds": []
    },
    "perPlayerRadius": {
      "enabled": true,
      "minRadius": 2,
      "maxRadius": 8,
      "tpsLow": 15.0,
      "tpsHigh": 18.0
    },
    "fluidFixer": {
      "enabled": true
    },
    "chunkUnloader": {
      "enabled": true,
      "intervalSeconds": 15,
      "unloadDistanceOffset": 4,
      "minLoadedChunks": 100,
      "unloadDelaySeconds": 30,
      "maxUnloadsPerRun": 200
    }
  }
}
```

Notes:
- `tpsAdjuster.onlyWorlds` can target specific worlds; use `__DEFAULT` for the default world.
- If `onlyWorlds` is empty, TPS adjustments apply to all worlds.

### Persistent Shared Instances

To disable the static shared instance system and revert to vanilla behavior, set:

```json
{
  "sanitizers": {
    "sharedInstancePersistence": false
  },
  "transformers": {
    "staticSharedInstances": false
  }
}
```

Note: if you already created `instance-shared-*` worlds, they will remain on disk.
To fully return to vanilla instance behavior, delete those folders under your server `worlds/` directory.

Boot behavior: after all worlds finish loading, Hyzer will automatically unload any
`instance-shared-*` worlds that have zero players. This prevents them from counting toward
the portal fragment limit on a fresh server boot while keeping the terrain persistent on disk.

## Admin Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/interactionstatus` | `/hyfixstatus`, `/hfs` | Show Hyzer statistics and status |
| `/cleaninteractions` | `/ci`, `/cleanint`, `/fixinteractions` | Scan/remove orphaned interaction zones |
| `/cleanwarps` | `/cw`, `/fixwarps`, `/warpclean` | Scan/remove orphaned warp entries |
| `/fixcounter` | `/fc`, `/blockcounter`, `/teleporterlimit` | Fix/view teleporter BlockCounter values |
| `/who` | | List online players |

---

## Verification

### Runtime Plugin Loaded

Look for these log messages at startup:
```
[Hyzer] Plugin enabled - Hyzer vX.X.X
[Hyzer] [PickupItemSanitizer] Active - monitoring for corrupted pickup items
```

### Early Plugin Loaded

Look for these log messages at startup (24 transformers):
```
[Hyzer-Early] InteractionChain transformation COMPLETE!
[Hyzer-Early] ArchetypeChunk transformation COMPLETE!
[Hyzer-Early] BlockComponentChunk transformation COMPLETE!
[Hyzer-Early] BlockHealthSystem transformation COMPLETE!
[Hyzer-Early] CommandBuffer transformation COMPLETE!
[Hyzer-Early] InteractionManager transformation COMPLETE!
[Hyzer-Early] PacketHandler transformation COMPLETE!
[Hyzer-Early] GamePacketHandler transformation COMPLETE!
[Hyzer-Early] PrefabLoader transformation COMPLETE!
[Hyzer-Early] SetMemoriesCapacityInteraction transformation COMPLETE!
[Hyzer-Early] SpawnMarkerSystems transformation COMPLETE!
[Hyzer-Early] SpawnReferenceSystems transformation COMPLETE!
[Hyzer-Early] TickingThread transformation COMPLETE!
[Hyzer-Early] Universe transformation COMPLETE!
[Hyzer-Early] World transformation COMPLETE!
[Hyzer-Early] InstancesPlugin transformation COMPLETE!
[Hyzer-Early] ChunkSavingSystems transformation COMPLETE!
[Hyzer-Early] WorldConfig SpawnProvider transformation COMPLETE!
[Hyzer-Early] RemovalSystem transformation COMPLETE!
[Hyzer-Early] PortalDeviceSummonPage transformation COMPLETE!
[Hyzer-Early] WorldMapTracker transformation COMPLETE!
[Hyzer-Early] WorldSpawningSystem transformation COMPLETE!
[Hyzer-Early] Successfully transformed UUIDSystem.onEntityRemove()
```

---

## Support

**Found a bug?** Please report it on [GitHub Issues](https://github.com/Labortale/Hyzer/issues) with:
- Server logs showing the error
- Steps to reproduce (if known)
- Hyzer version

---

## Building from Source

Requires Java 25 and access to `HytaleServer.jar`.

```bash
# Clone the repo
git clone https://github.com/Labortale/Hyzer.git
cd Hyzer

# Place HytaleServer.jar in libs/ directory
mkdir -p libs
cp /path/to/HytaleServer.jar libs/

# Build runtime plugin
./gradlew build
# Output: build/libs/Hyzer.jar

# Build early plugin
cd Hyzer-early
./gradlew build
# Output: build/libs/Hyzer-early-1.0.0.jar
```

---

## License

This project is provided as-is for the Hytale community. Use at your own risk.

---

## Contributing

Found another Hytale bug that needs patching? We'd love your help!

- Open an issue or pull request.
