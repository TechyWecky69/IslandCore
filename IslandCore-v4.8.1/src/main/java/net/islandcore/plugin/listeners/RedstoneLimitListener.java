package net.islandcore.plugin.listeners;

import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Limits pathological redstone/hopper activity per island without disabling normal redstone. */
public class RedstoneLimitListener implements Listener {
    private final int maxRedstoneEventsPerSecond;
    private final int maxPistonEventsPerSecond;
    private final int maxHopperTransfersPerSecond;
    private final Map<UUID, Counter> counters = new HashMap<>();

    public RedstoneLimitListener(JavaPlugin plugin) {
        var c = plugin.getConfig();
        maxRedstoneEventsPerSecond = c.getInt("redstone-limits.max-redstone-events-per-second", 500);
        maxPistonEventsPerSecond = c.getInt("redstone-limits.max-piston-events-per-second", 30);
        maxHopperTransfersPerSecond = c.getInt("redstone-limits.max-hopper-transfers-per-second", 100);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        World world = event.getBlock().getWorld();
        UUID owner = WorldUtil.getIslandOwner(world);
        if (owner == null || maxRedstoneEventsPerSecond <= 0) return;
        if (!increment(owner, Type.REDSTONE, maxRedstoneEventsPerSecond)) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePiston(event.getBlock().getWorld(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePiston(event.getBlock().getWorld(), event);
    }

    private void handlePiston(World world, org.bukkit.event.Cancellable event) {
        UUID owner = WorldUtil.getIslandOwner(world);
        if (owner == null || maxPistonEventsPerSecond <= 0) return;
        if (!increment(owner, Type.PISTON, maxPistonEventsPerSecond)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopper(InventoryMoveItemEvent event) {
        World world = event.getSource().getLocation() == null ? null : event.getSource().getLocation().getWorld();
        UUID owner = WorldUtil.getIslandOwner(world);
        if (owner == null || maxHopperTransfersPerSecond <= 0) return;
        if (!increment(owner, Type.HOPPER, maxHopperTransfersPerSecond)) {
            event.setCancelled(true);
        }
    }

    private boolean increment(UUID owner, Type type, int limit) {
        long now = System.currentTimeMillis();
        Counter counter = counters.computeIfAbsent(owner, ignored -> new Counter());
        if (now - counter.windowStart >= 1000L) {
            counter.windowStart = now;
            counter.redstone = 0;
            counter.piston = 0;
            counter.hopper = 0;
        }
        int current = switch (type) {
            case REDSTONE -> ++counter.redstone;
            case PISTON -> ++counter.piston;
            case HOPPER -> ++counter.hopper;
        };
        return current <= limit;
    }

    private enum Type { REDSTONE, PISTON, HOPPER }

    private static class Counter {
        long windowStart = System.currentTimeMillis();
        int redstone;
        int piston;
        int hopper;
    }
}
