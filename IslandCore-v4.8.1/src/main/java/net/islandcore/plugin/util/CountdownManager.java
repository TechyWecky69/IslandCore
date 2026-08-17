package net.islandcore.plugin.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks the "next loot item in N seconds" countdown per player (transient, like the original's local variable). */
public final class CountdownManager {

    private static final Map<UUID, Integer> COUNTDOWNS = new ConcurrentHashMap<>();

    private CountdownManager() {}

    public static void set(UUID uuid, int seconds) {
        COUNTDOWNS.put(uuid, seconds);
    }

    public static int get(UUID uuid) {
        return COUNTDOWNS.getOrDefault(uuid, 0);
    }

    public static void decrement(UUID uuid) {
        COUNTDOWNS.merge(uuid, -1, Integer::sum);
    }

    public static void remove(UUID uuid) {
        COUNTDOWNS.remove(uuid);
    }
}
