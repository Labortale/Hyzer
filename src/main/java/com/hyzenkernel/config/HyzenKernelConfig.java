package com.hyzenkernel.config;

/**
 * HyzenKernel Configuration - Contains all runtime and early plugin settings.
 * 
 * This class is deserialized from config.json using Gson.
 * All fields have default values that match the original hardcoded behavior.
 */
public class HyzenKernelConfig {

    // Sanitizer toggles
    public SanitizersConfig sanitizers = new SanitizersConfig();
    
    // Interaction manager settings
    public InteractionManagerConfig interactionManager = new InteractionManagerConfig();
    
    // Instance tracker settings
    public InstanceTrackerConfig instanceTracker = new InstanceTrackerConfig();
    
    // Monitor settings
    public MonitorConfig monitor = new MonitorConfig();
    
    // Empty archetype settings
    public EmptyArchetypeConfig emptyArchetype = new EmptyArchetypeConfig();
    
    // Logging settings
    public LoggingConfig logging = new LoggingConfig();
    
    // Transformer toggles (for early plugin)
    public TransformersConfig transformers = new TransformersConfig();
    
    // World transformer settings
    public WorldConfig world = new WorldConfig();
    
    // Early plugin logging
    public EarlyConfig early = new EarlyConfig();

    // Interaction timeout settings (for early plugin)
    public InteractionTimeoutConfig interactionTimeout = new InteractionTimeoutConfig();

    // Optimization settings
    public OptimizationConfig optimization = new OptimizationConfig();

    /**
     * Sanitizer toggle configuration
     */
    public static class SanitizersConfig {
        public boolean respawnBlock = true;
        public boolean processingBench = true;
        public boolean craftingManager = true;
        public boolean interactionManager = true;
        public boolean spawnBeacon = true;
        public boolean chunkTracker = true;
        public boolean gatherObjective = true;
        public boolean emptyArchetype = true;
        public boolean instancePositionTracker = true;
        public boolean sharedInstancePersistence = true;
        public boolean defaultWorldRecovery = true;  // Auto-reload default world after crash
    }

    /**
     * Interaction manager configuration
     */
    public static class InteractionManagerConfig {
        public long clientTimeoutMs = 2000;
    }

    /**
     * Instance tracker configuration
     */
    public static class InstanceTrackerConfig {
        public int positionTtlHours = 24;
    }

    /**
     * Monitor configuration
     */
    public static class MonitorConfig {
        public int logIntervalTicks = 6000; // 5 minutes at 20 TPS
    }

    /**
     * Empty archetype configuration
     */
    public static class EmptyArchetypeConfig {
        public int skipFirstN = 1000;
        public int logEveryN = 10000;
    }

    /**
     * Logging configuration
     */
    public static class LoggingConfig {
        public boolean verbose = false;
        public boolean sanitizerActions = true;
    }

    /**
     * Transformer toggle configuration (for early plugin)
     */
    public static class TransformersConfig {
        public boolean interactionChain = true;
        public boolean interactionManager = true;
        public boolean world = true;
        public boolean spawnReferenceSystems = true;
        public boolean beaconSpawnController = true;
        public boolean blockComponentChunk = true;
        public boolean spawnMarkerSystems = true;
        public boolean spawnMarkerEntity = true;
        public boolean trackedPlacement = true;
        public boolean commandBuffer = true;
        public boolean worldMapTracker = true;
        public boolean archetypeChunk = true;
        public boolean interactionTimeout = true;
        public boolean uuidSystem = true;
        public boolean tickingThread = true;
        public boolean universeRemovePlayer = true;
        public boolean worldSpawningSystem = true;
        public boolean staticSharedInstances = true;
        public boolean gamePacketHandler = true;
        public boolean blockHealthSystem = true;
    }

    /**
     * World transformer configuration
     */
    public static class WorldConfig {
        public int retryCount = 5;
        public long retryDelayMs = 20;
    }

    /**
     * Early plugin configuration
     */
    public static class EarlyConfig {
        public EarlyLoggingConfig logging = new EarlyLoggingConfig();
    }

    /**
     * Early plugin logging configuration
     */
    public static class EarlyLoggingConfig {
        public boolean verbose = false;
    }

    /**
     * Interaction timeout configuration
     * Controls how long the server waits for client responses during interactions
     */
    public static class InteractionTimeoutConfig {
        /** Base timeout in milliseconds (added to ping-based calculation) */
        public long baseTimeoutMs = 6000;

        /** Multiplier applied to average ping */
        public double pingMultiplier = 3.0;
    }

    /**
     * Optimization configuration (runtime plugin)
     */
    public static class OptimizationConfig {
        public boolean enabled = true;
        public int minViewRadius = 6;
        public int maxViewRadius = 12;
        public int checkIntervalMillis = 5000;
        public TpsConfig tps = new TpsConfig();
        public TpsAdjusterConfig tpsAdjuster = new TpsAdjusterConfig();
        public ActiveChunkUnloaderConfig chunkUnloader = new ActiveChunkUnloaderConfig();
        public PerPlayerRadiusConfig perPlayerRadius = new PerPlayerRadiusConfig();
        public FluidFixerConfig fluidFixer = new FluidFixerConfig();
    }

    /**
     * TPS thresholds for view radius adjustment
     */
    public static class TpsConfig {
        public boolean enabled = true;
        public double lowTpsThreshold = 18.0;
        public double recoveryTpsThreshold = 19.5;
    }

    /**
     * World TPS adjuster configuration
     */
    public static class TpsAdjusterConfig {
        public boolean enabled = true;
        public int tpsLimit = 20;
        public int tpsLimitEmpty = 5;
        public String[] onlyWorlds = new String[0];
        public int initialDelaySeconds = 30;
        public int checkIntervalSeconds = 5;
        public int emptyLimitDelaySeconds = 300;
    }

    /**
     * Active chunk unloader settings
     */
    public static class ActiveChunkUnloaderConfig {
        public boolean enabled = true;
        public int intervalSeconds = 15;
        public int unloadDistanceOffset = 4;
        public int minLoadedChunks = 100;
        public int unloadDelaySeconds = 30;
        public int maxUnloadsPerRun = 200;
    }

    /**
     * Per-player hot radius settings
     */
    public static class PerPlayerRadiusConfig {
        public boolean enabled = true;
        public int minRadius = 4;
        public int maxRadius = 6;
        public float tpsLow = 15.0f;
        public float tpsHigh = 18.0f;
        public int adjustmentStep = 1;
    }

    /**
     * Fluid pre-process fixer settings
     */
    public static class FluidFixerConfig {
        public boolean enabled = true;
    }


    // ============================================
    // Convenience setter methods for runtime config updates
    // ============================================

    /**
     * Set verbose logging.
     */
    public void setVerbose(boolean verbose) {
        this.logging.verbose = verbose;
    }

    /**
     * Check if verbose logging is enabled.
     */
    public boolean isVerbose() {
        return this.logging.verbose;
    }

    /**
     * Set sanitizer action logging.
     */
    public void setLogSanitizerActions(boolean enabled) {
        this.logging.sanitizerActions = enabled;
    }

    /**
     * Check if sanitizer action logging is enabled.
     */
    public boolean logSanitizerActions() {
        return this.logging.sanitizerActions;
    }

}
