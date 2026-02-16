package com.hyzer.optimization;

import com.hyzer.config.HyzerConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FluidFixerService {

    private final HytaleLogger logger;
    private final HyzerConfig.FluidFixerConfig config;
    private boolean fixApplied = false;

    public FluidFixerService(HytaleLogger logger, HyzerConfig.FluidFixerConfig config) {
        this.logger = logger.getSubLogger("FluidFixer");
        this.config = config;
    }

    public void apply(Object eventRegistry) {
        if (config == null || !config.enabled) {
            logger.atInfo().log("[FluidFixer] Disabled by configuration");
            return;
        }

        if (fixApplied) {
            logger.atInfo().log("[FluidFixer] Already applied");
            return;
        }

        logger.atInfo().log("[FluidFixer] Attempting to neutralize FluidPlugin pre-process handler...");

        try {
            int neutralized = neutralizeFluidListeners(eventRegistry);
            if (neutralized > 0) {
                logger.atInfo().log(
                        "[FluidFixer] SUCCESS: Neutralized %d fluid listener(s). Chunk generation will be faster!",
                        neutralized);
                fixApplied = true;
            } else {
                logger.atWarning().log("[FluidFixer] WARNING: Could not find FluidPlugin listener to neutralize");
            }
        } catch (Exception e) {
            logger.atSevere().withCause(e).log("[FluidFixer] Error during neutralization");
        }
    }

    public boolean isFixApplied() {
        return fixApplied;
    }

    private int neutralizeFluidListeners(Object eventRegistry) {
        int count = 0;

        try {
            Object registry = eventRegistry;
            Object rootRegistry = null;

            for (int i = 0; i < 5; i++) {
                try {
                    Field parentField = findField(registry.getClass(), "parent");
                    if (parentField == null) {
                        break;
                    }

                    parentField.setAccessible(true);
                    Object parent = parentField.get(registry);
                    if (parent != null) {
                        registry = parent;
                        if (findField(parent.getClass(), "registryMap") != null) {
                            rootRegistry = parent;
                            break;
                        }
                    }
                } catch (Exception e) {
                    break;
                }
            }

            if (rootRegistry == null) {
                logger.atWarning().log("[FluidFixer] Could not find root EventBus");
                return 0;
            }

            Field registryMapField = findField(rootRegistry.getClass(), "registryMap");
            if (registryMapField == null) {
                return 0;
            }

            registryMapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Class<?>, Object> registryMap = (Map<Class<?>, Object>) registryMapField.get(rootRegistry);

            Object chunkEventRegistry = registryMap.get(ChunkPreLoadProcessEvent.class);
            if (chunkEventRegistry == null) {
                for (Object reg : registryMap.values()) {
                    count += scanAndNeutralize(reg);
                }
            } else {
                count += scanAndNeutralize(chunkEventRegistry);
            }

        } catch (Exception e) {
            logger.atSevere().withCause(e).log("[FluidFixer] Error during reflection");
        }

        return count;
    }

    private int scanAndNeutralize(Object registry) throws Exception {
        int count = 0;

        for (Class<?> clazz = registry.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (field.getType().getName().contains("EventConsumerMap")) {
                    Object consumerMap = field.get(registry);
                    if (consumerMap != null) {
                        count += processConsumerMap(consumerMap);
                    }
                }
            }
        }

        return count;
    }

    private int processConsumerMap(Object consumerMap) throws Exception {
        int count = 0;
        Field mapField = findField(consumerMap.getClass(), "map");
        if (mapField == null) {
            return 0;
        }

        mapField.setAccessible(true);
        Object map = mapField.get(consumerMap);
        if (map == null) {
            return 0;
        }

        Method valuesMethod = map.getClass().getMethod("values");
        valuesMethod.setAccessible(true);
        Object values = valuesMethod.invoke(map);

        if (values instanceof Collection<?> collection) {
            for (Object value : collection) {
                if (value instanceof List<?> list) {
                    for (Object item : list) {
                        if (isFluidPluginConsumer(item)) {
                            logger.atInfo().log("[FluidFixer] Found FluidPlugin listener: %s", item.toString());
                            if (lobotomizeConsumer(item)) {
                                count++;
                            }
                        }
                    }
                }
            }
        }

        return count;
    }

    private boolean lobotomizeConsumer(Object consumer) {
        try {
            Consumer<Object> dummyConsumer = event -> {
            };
            boolean success = false;

            Field consumerField = findField(consumer.getClass(), "consumer");
            if (consumerField != null) {
                consumerField.setAccessible(true);
                consumerField.set(consumer, dummyConsumer);
                logger.atInfo().log("[FluidFixer] Replaced 'consumer' with dummy");
                success = true;
            }

            Field timedConsumerField = findField(consumer.getClass(), "timedConsumer");
            if (timedConsumerField != null) {
                timedConsumerField.setAccessible(true);
                timedConsumerField.set(consumer, dummyConsumer);
                logger.atInfo().log("[FluidFixer] Replaced 'timedConsumer' with dummy");
                success = true;
            }

            return success;
        } catch (Exception e) {
            logger.atWarning().log("[FluidFixer] Failed to neutralize: %s", e.getMessage());
            return false;
        }
    }

    private boolean isFluidPluginConsumer(Object item) {
        try {
            Field consumerStringField = findField(item.getClass(), "consumerString");
            if (consumerStringField != null) {
                consumerStringField.setAccessible(true);
                Object value = consumerStringField.get(item);
                if (value != null && value.toString().contains("FluidPlugin")) {
                    return true;
                }
            }

            return item.toString().contains("FluidPlugin");
        } catch (Exception e) {
            return false;
        }
    }

    private Field findField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
