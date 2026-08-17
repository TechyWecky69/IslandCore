package com.islandcore.anticheat.listener;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryListener implements Listener {

    private final IslandCoreAntiCheat plugin;

    public InventoryListener(IslandCoreAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        plugin.getCheckManager().handleInventoryClick(event);
    }
}
