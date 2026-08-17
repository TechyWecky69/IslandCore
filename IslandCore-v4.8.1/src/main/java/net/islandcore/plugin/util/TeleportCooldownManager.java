package net.islandcore.plugin.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks the ten-second /home and /visit cooldown. */
public final class TeleportCooldownManager {

    private static final Map<UUID, Long> LAST_TELEPORT = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MILLIS = 10_000L;

    private TeleportCooldownManager() {}

    /** Returns remaining cooldown in whole seconds, or 0 if ready. */
    public static long remainingSeconds(UUID uuid) {
        Long last = LAST_TELEPORT.get(uuid);
        if (last == null) return 0;

        long remaining = COOLDOWN_MILLIS - (System.currentTimeMillis() - last);
        if (remaining <= 0) {
            LAST_TELEPORT.remove(uuid);
            return 0;
        }
        return (remaining + 999L) / 1000L;
    }

    public static boolean isOnCooldown(UUID uuid) {
        return remainingSeconds(uuid) > 0;
    }

    public static void markTeleport(UUID uuid) {
        LAST_TELEPORT.put(uuid, System.currentTimeMillis());
    }

    /** Kicking a visitor clears their cooldown so /home works immediately. */
    public static void clear(UUID uuid) {
        LAST_TELEPORT.remove(uuid);
    }

    public static void remove(UUID uuid) {
        LAST_TELEPORT.remove(uuid);
    }
}
