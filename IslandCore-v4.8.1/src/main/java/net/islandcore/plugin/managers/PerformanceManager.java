package net.islandcore.plugin.managers;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Villager;


public final class PerformanceManager {
    private PerformanceManager() {}

    public static int countEntities(World world) {
        return world == null ? 0 : world.getEntities().size();
    }

    public static int countMobs(World world) {
        if (world == null) return 0;
        int count = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Mob) count++;
        }
        return count;
    }

    public static int countVillagers(World world) {
        if (world == null) return 0;
        int count = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Villager) count++;
        }
        return count;
    }


    public static int countItems(World world) {
        if (world == null) return 0;
        int count = 0;
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Item) count++;
        }
        return count;
    }
}
