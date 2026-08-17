package net.islandcore.plugin.listeners;

import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * Bug fix: visitors are granted flight (see VisitConfirmGUI) while exploring
 * someone else's island, but Bukkit resets a player's allow-flight flag back
 * to their gamemode default whenever they respawn. Since dying to the void
 * respawns the player back in the same island world (no bed set), that reset
 * silently took their flight away. This listener re-grants flight right
 * after respawn whenever the player is still standing in an island world
 * that isn't their own.
 */
public class VisitorRespawnListener implements Listener {

    private final JavaPlugin plugin;

    public VisitorRespawnListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        World respawnWorld = event.getRespawnLocation().getWorld();

        UUID ownerId = WorldUtil.getIslandOwner(respawnWorld);
        if (ownerId == null) return; // not an island world at all
        if (ownerId.equals(player.getUniqueId())) return; // their own island, nothing to fix

        // Ability changes made during PlayerRespawnEvent can be silently
        // overwritten by the respawn packet that follows it, so apply this
        // a tick later once the respawn has actually completed.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (!player.getWorld().equals(respawnWorld)) return;

            player.setAllowFlight(true);
        });
    }
}
