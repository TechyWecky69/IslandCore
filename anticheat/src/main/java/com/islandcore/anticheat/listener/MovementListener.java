package com.islandcore.anticheat.listener;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class MovementListener implements Listener {

    private final IslandCoreAntiCheat plugin;

    public MovementListener(IslandCoreAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        plugin.getCheckManager().handlePlayerMove(event);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        // Give the client a moment to catch up after any teleport (setback,
        // warp, ender pearl, etc.) so we don't false-flag the resulting jump.
        Player player = event.getPlayer();
        plugin.getPlayerDataManager().get(player).setExemptUntil(System.currentTimeMillis() + 1500);
    }
}
