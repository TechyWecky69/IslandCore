package net.islandcore.plugin.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks who each player should /reply to.
 *
 * Note: the original Skript stored this in a variable named
 * "{_%player's uuid%.reply}" - the leading underscore makes it a *local*
 * variable in Skript, meaning it would actually have been wiped the moment
 * each trigger finished and /reply would never have worked across separate
 * command executions. That looks like a bug in the source script. This
 * plugin keeps the clearly-intended behaviour (a working /reply) using a
 * real in-memory map instead.
 */
public final class ReplyManager {

    private static final Map<UUID, UUID> LAST_MESSAGED = new ConcurrentHashMap<>();

    private ReplyManager() {}

    public static void setReplyTarget(UUID player, UUID target) {
        LAST_MESSAGED.put(player, target);
    }

    public static UUID getReplyTarget(UUID player) {
        return LAST_MESSAGED.get(player);
    }
}
