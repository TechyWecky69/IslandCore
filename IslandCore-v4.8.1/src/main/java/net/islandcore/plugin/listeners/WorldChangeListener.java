package net.islandcore.plugin.listeners;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.managers.IslandManager;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.ratings.RatingPrompt;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WorldChangeListener implements Listener {

    private final JavaPlugin plugin;
    private final DataStore data;
    private final IslandManager islandManager;
    private final RatingManager ratings;

    public WorldChangeListener(JavaPlugin plugin, DataStore data, IslandManager islandManager, RatingManager ratings) {
        this.plugin = plugin;
        this.data = data;
        this.islandManager = islandManager;
        this.ratings = ratings;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        data.setLootingActive(player.getUniqueId(), false);

        promptRatingIfLeavingVisit(player);

        // The player may have just been the last person on an island.
        // Wait one tick so Bukkit has finished moving them before checking
        // whether the old world is empty.
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> islandManager.scheduleUnload(event.getFrom()),
                1L
        );
    }

    /** If the player just left an island they were tracked as visiting, offer a clickable star prompt. */
    private void promptRatingIfLeavingVisit(Player player) {
        UUID visitorId = player.getUniqueId();
        UUID owner = ratings.getActiveVisitOwner(visitorId);

        if (owner != null && !owner.equals(visitorId)
                && ratings.canRate(visitorId, owner) == RatingManager.RateResult.SUCCESS) {
            OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(owner);
            String name = ownerPlayer.getName() != null ? ownerPlayer.getName() : "that player";
            RatingPrompt.send(player, name);
        }

        ratings.endVisitSession(visitorId);
    }
}
