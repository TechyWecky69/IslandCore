package net.islandcore.plugin.util;

import net.islandcore.plugin.ranks.RankManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import net.islandcore.plugin.ranks.Rank;

import java.util.UUID;

public final class IslandVisitorUtil {

    private IslandVisitorUtil() {}

    /**
     * Sends every player currently inside the owner's island world back to
     * their own island. The owner is never treated as a visitor.
     */
    public static int kickVisitors(UUID ownerId, RankManager ranks) {
        World island = WorldUtil.getIslandWorld(ownerId);
        if (island == null) return 0;

        int kicked = 0;
        for (Player visitor : Bukkit.getOnlinePlayers()) {
            if (!visitor.getWorld().equals(island)) continue;
            if (visitor.getUniqueId().equals(ownerId)) continue;
            if (isStaffOrHigher(visitor, ranks)) continue;

            World ownWorld = WorldUtil.getIslandWorld(visitor.getUniqueId());
            if (ownWorld != null) {
                visitor.teleport(ownWorld.getSpawnLocation());
                visitor.setAllowFlight(false);
            } else if (!Bukkit.getWorlds().isEmpty()) {
                visitor.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                visitor.setAllowFlight(false);
            }

            TeleportCooldownManager.clear(visitor.getUniqueId());
            Msg.send(visitor, "&c⚠ &7You have been removed because island visits were disabled.");
            kicked++;
        }
        return kicked;
    }

    /** Removes the visitor and clears their teleport cooldown immediately. */
    public static boolean kickVisitor(Player visitor, String ownerName, RankManager ranks) {
        if (isStaffOrHigher(visitor, ranks)) {
            Msg.send(visitor, "&e⚠ &7You cannot be kicked from an island because you are &b"
                    + ranks.getRankOrDefault(visitor.getUniqueId()).name() + "&7.");
            return false;
        }
        World ownWorld = WorldUtil.getIslandWorld(visitor.getUniqueId());
        if (ownWorld != null) {
            visitor.teleport(ownWorld.getSpawnLocation());
            visitor.setAllowFlight(false);
        } else if (!Bukkit.getWorlds().isEmpty()) {
            visitor.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            visitor.setAllowFlight(false);
        }
        TeleportCooldownManager.clear(visitor.getUniqueId());
        Msg.send(visitor, "&c⚠ &7You have been removed from " + ownerName + "'s island.");
        return true;
    }

    private static boolean isStaffOrHigher(Player player, RankManager ranks) {
        if (player.isOp()) return true;
        Rank rank = ranks.getRankOrDefault(player.getUniqueId());
        return rank == Rank.HELPER || rank == Rank.ADMIN || rank == Rank.OWNER;
    }

}
