package com.islandcore.anticheat.listener;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {

    private final IslandCoreAntiCheat plugin;

    public CombatListener(IslandCoreAntiCheat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        plugin.getCheckManager().handleEntityDamageByEntity(event);
    }
}
