package net.islandcore.plugin.tasks;

import net.islandcore.plugin.managers.IslandManager;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

/** Safety-net maintenance for loaded islands that no longer have players. */
public class IslandMaintenanceTask extends BukkitRunnable {
    private final IslandManager islands;

    public IslandMaintenanceTask(IslandManager islands) {
        this.islands = islands;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            if (WorldUtil.getIslandOwner(world) == null) continue;
            if (world.getPlayers().isEmpty()) {
                islands.scheduleUnload(world);
            }
        }
    }
}
