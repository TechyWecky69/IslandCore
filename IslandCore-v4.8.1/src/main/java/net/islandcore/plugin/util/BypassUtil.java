package net.islandcore.plugin.util;

import org.bukkit.entity.Player;

public final class BypassUtil {

    private BypassUtil() {}

    private static String legacyName = "ILiveOffCaffine";

    public static void setLegacyName(String name) {
        legacyName = name;
    }

    /**
     * True if this player should bypass island protection - either via the
     * modern "islandcore.bypass" permission, or the original script's
     * hard-coded display name check.
     */
    public static boolean bypasses(Player player) {
        if (player.hasPermission("islandcore.bypass")) return true;
        return legacyName != null && legacyName.equals(player.getDisplayName());
    }
}
