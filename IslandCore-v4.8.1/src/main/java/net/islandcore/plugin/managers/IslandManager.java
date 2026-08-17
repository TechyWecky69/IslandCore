package net.islandcore.plugin.managers;

import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls island loading/unloading without repeatedly loading and unloading
 * the same world when players teleport between islands.
 *
 * <p>Unloading uses a delayed task so brief island hops (e.g. a visitor
 * teleporting home then back) don't cause rapid load/unload cycles.
 * Once a pending unload is scheduled for a world it is NOT rescheduled on
 * subsequent calls — this prevents the maintenance task from perpetually
 * resetting the timer, which was causing worlds to never actually unload.
 */
public class IslandManager {

    private final JavaPlugin plugin;
    private final long unloadDelayTicks;
    /** ownerUUID -> scheduler task ID. Presence means an unload is already queued. */
    private final Map<UUID, Integer> pendingUnloads = new ConcurrentHashMap<>();

    public IslandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.unloadDelayTicks = Math.max(1L, plugin.getConfig().getLong("island-unload-delay-seconds", 60L)) * 20L;
    }

    public boolean load(UUID ownerId) {
        cancelPendingUnload(ownerId);
        return WorldUtil.loadIsland(ownerId);
    }

    /**
     * Queues an unload for the world's owner island after the configured delay.
     * If an unload is already pending for this owner, this call is a no-op —
     * the existing timer is left running so it can't be reset indefinitely by
     * the maintenance task.
     */
    public void scheduleUnload(World world) {
        if (world == null) return;
        UUID ownerId = WorldUtil.getIslandOwner(world);
        if (ownerId == null) return;

        // Already queued — don't reset the timer.
        if (pendingUnloads.containsKey(ownerId)) return;

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingUnloads.remove(ownerId);
            World current = WorldUtil.getIslandWorld(ownerId);
            if (current != null) {
                WorldUtil.unloadIslandIfEmpty(current);
            }
        }, unloadDelayTicks).getTaskId();
        pendingUnloads.put(ownerId, taskId);
    }

    public void scheduleUnload(UUID ownerId) {
        scheduleUnload(WorldUtil.getIslandWorld(ownerId));
    }

    /**
     * Cancels any pending unload for this owner (called when a player
     * re-joins or a visitor arrives, preventing an unload mid-visit).
     */
    public void cancelPendingUnload(UUID ownerId) {
        Integer taskId = pendingUnloads.remove(ownerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void shutdown() {
        for (Integer taskId : pendingUnloads.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        pendingUnloads.clear();
    }
}
