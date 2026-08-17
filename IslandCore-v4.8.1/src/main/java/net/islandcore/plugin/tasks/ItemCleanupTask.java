package net.islandcore.plugin.tasks;

import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitRunnable;

/** Periodically removes old dropped items from loaded island worlds. */
public class ItemCleanupTask extends BukkitRunnable {
    private final int maxAgeTicks;

    public ItemCleanupTask(int maxAgeSeconds) {
        this.maxAgeTicks = Math.max(20, maxAgeSeconds * 20);
    }

    @Override
    public void run() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            if (WorldUtil.getIslandOwner(world) == null) continue;
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item && item.getTicksLived() >= maxAgeTicks) {
                    item.remove();
                    removed++;
                }
            }
        }
    }
}
