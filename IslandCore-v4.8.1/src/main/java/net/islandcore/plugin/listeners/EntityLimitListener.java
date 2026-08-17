package net.islandcore.plugin.listeners;

import net.islandcore.plugin.managers.PerformanceManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.entity.Player;

public class EntityLimitListener implements Listener {
    private final int maxTotal;
    private final int maxMobs;
    private final int maxVillagers;
    private final int maxMinecarts;
    private final int maxItems;
    private final boolean enabled;

    public EntityLimitListener(org.bukkit.plugin.java.JavaPlugin plugin) {
        var c = plugin.getConfig();
        enabled = c.getBoolean("entity-limits.enabled", true);
        maxTotal = c.getInt("entity-limits.max-total", 150);
        maxMobs = c.getInt("entity-limits.max-mobs", 80);
        maxVillagers = c.getInt("entity-limits.max-villagers", 20);
        maxMinecarts = c.getInt("entity-limits.max-minecarts", 20);
        maxItems = c.getInt("entity-limits.max-item-entities", 100);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(EntitySpawnEvent event) {
        if (!enabled) return;
        World world = event.getLocation().getWorld();
        if (WorldUtil.getIslandOwner(world) == null) return;

        Entity entity = event.getEntity();
        if (entity instanceof Player) return;

        if (maxTotal > 0 && PerformanceManager.countEntities(world) >= maxTotal) {
            cancel(event, "&cThis island has reached its entity limit.");
            return;
        }
        if (entity instanceof Villager && maxVillagers > 0 && PerformanceManager.countVillagers(world) >= maxVillagers) {
            cancel(event, "&cThis island has reached its villager limit.");
            return;
        }
        if (entity instanceof Mob && maxMobs > 0 && PerformanceManager.countMobs(world) >= maxMobs) {
            cancel(event, "&cThis island has reached its mob limit.");
            return;
        }
        if (entity instanceof Item && maxItems > 0 && PerformanceManager.countItems(world) >= maxItems) {
            cancel(event, "&cThis island has too many dropped items.");
        }
    }

    private void cancel(EntitySpawnEvent event, String message) {
        event.setCancelled(true);
        // No player message here: mob spawning can happen repeatedly and spam everyone.
    }
}
