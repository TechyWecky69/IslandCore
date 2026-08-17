package com.islandcore.anticheat.listener;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockListener implements Listener {

    private final IslandCoreAntiCheat plugin;

    public BlockListener(IslandCoreAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        plugin.getCheckManager().handleBlockBreak(event);
    }
}
