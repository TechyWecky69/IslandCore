package net.islandcore.plugin.listeners;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.managers.IslandManager;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DisconnectListener implements Listener {

    private final JavaPlugin plugin;
    private final DataStore data;
    private final IslandManager islandManager;
    private final RatingManager ratings;

    public DisconnectListener(JavaPlugin plugin, DataStore data, IslandManager islandManager, RatingManager ratings) {
        this.plugin = plugin;
        this.data = data;
        this.islandManager = islandManager;
        this.ratings = ratings;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        org.bukkit.World world = player.getWorld();
        event.setQuitMessage(null);
        ratings.endVisitSession(player.getUniqueId());

        // Re-check one tick later, after Bukkit has removed the quitter.
        // This works for both owners and visitors: if the player was the last
        // person on an island, that island can now be unloaded.
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> islandManager.scheduleUnload(world),
                1L
        );

        if (data.isLootingActive(player.getUniqueId())) {
            data.setLootingActive(player.getUniqueId(), false);
        }
    }
}
