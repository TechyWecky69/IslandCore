package net.islandcore.plugin.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public final class WorldUtil {

    private WorldUtil() {}

    private static String prefix = "worlds/";
    private static double borderSize = 200.0;

    public static void setPrefix(String p) {
        prefix = p;
    }

    public static void setBorderSize(double size) {
        borderSize = size > 0 ? size : 200.0;
    }

    public static String islandWorldName(UUID uuid) {
        return prefix + uuid;
    }

    /** Returns the loaded Bukkit world, or null when the island is unloaded. */
    public static World getIslandWorld(UUID uuid) {
        return Bukkit.getWorld(islandWorldName(uuid));
    }

    public static boolean hasIslandWorld(UUID uuid) {
        return getIslandWorld(uuid) != null;
    }

    /** True when the island world exists on disk, even if Multiverse has unloaded it. */
    public static boolean islandExists(UUID uuid) {
        return new File(Bukkit.getWorldContainer(), islandWorldName(uuid)).isDirectory();
    }

    /**
     * Loads an existing island through Multiverse. The caller should wait a few
     * ticks before trying to teleport, because the load command is not
     * guaranteed to finish during the same tick.
     */
    public static boolean loadIsland(UUID uuid) {
        if (getIslandWorld(uuid) != null) return true;
        if (!islandExists(uuid)) return false;

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv load " + islandWorldName(uuid)
        );
        return true;
    }

    /**
     * Permanently deletes an island world, loaded or not - Multiverse unloads
     * it first if necessary, then removes the folder from disk. There is no
     * undo, so callers must make sure the owner (and anyone else) is out of
     * the world before calling this.
     */
    public static void deleteIsland(UUID uuid) {
        if (!islandExists(uuid) && getIslandWorld(uuid) == null) return;

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv delete " + islandWorldName(uuid)
        );

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv confirm"
        );
    }

    /**
     * Finds the island owner from a loaded island world's name.
     * Returns null for non-island worlds.
     */
    public static UUID getIslandOwner(World world) {
        if (world == null) return null;

        String name = world.getName();
        if (!name.startsWith(prefix)) return null;

        String uuidText = name.substring(prefix.length());
        try {
            return UUID.fromString(uuidText);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Unloads an island only when nobody is using it and its owner is offline.
     * This is deliberately checked again immediately before unloading.
     */
    public static void unloadIslandIfEmpty(World world) {
        if (world == null) return;

        UUID ownerId = getIslandOwner(world);
        if (ownerId == null) return;

        Player owner = Bukkit.getPlayer(ownerId);
        if (owner != null && owner.isOnline()) return;

        if (!world.getPlayers().isEmpty()) return;

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mv unload " + world.getName()
        );
    }

    public static void unloadIslandIfEmpty(UUID ownerId) {
        unloadIslandIfEmpty(getIslandWorld(ownerId));
    }

    public static boolean isInOwnWorld(Player player) {
        World own = getIslandWorld(player.getUniqueId());
        return own != null && player.getWorld().equals(own);
    }

    /** The only valid home/visit destination for an island is that island's world spawn. */
    public static org.bukkit.Location getIslandSpawn(UUID uuid) {
        World world = getIslandWorld(uuid);
        return world == null ? null : world.getSpawnLocation();
    }

    /**
     * Keeps each island at exactly 200x200 blocks and centres the border on
     * the island's own spawn point.
     */
    public static void configureIslandWorld(World world) {
        if (world == null) return;
        org.bukkit.Location spawn = world.getSpawnLocation();
        world.getWorldBorder().setCenter(spawn.getX(), spawn.getZ());
        world.getWorldBorder().setSize(borderSize);
        // Island worlds do not need permanently retained spawn chunks; this saves
        // memory while the world is loaded without changing normal player chunk loading.
        world.setKeepSpawnInMemory(false);
    }

    /** Apply island-world settings when a player joins or a world is created. */
    public static void configureIslandWorld(UUID uuid) {
        configureIslandWorld(getIslandWorld(uuid));
    }
}
