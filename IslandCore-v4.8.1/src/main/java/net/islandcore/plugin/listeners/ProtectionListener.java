package net.islandcore.plugin.listeners;

import net.islandcore.plugin.util.BypassUtil;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;

/** Protects island-owned blocks/entities from visitors while allowing them to explore. */
public class ProtectionListener implements Listener {

    private static boolean isProtectedSpawnBlock(Location loc) {
        if (loc.getWorld() == null) return false;
        Location spawn = loc.getWorld().getSpawnLocation();
        return loc.getBlockX() == spawn.getBlockX()
                && loc.getBlockZ() == spawn.getBlockZ()
                && (loc.getBlockY() == spawn.getBlockY() || loc.getBlockY() == spawn.getBlockY() + 1);
    }

    private static boolean canModify(Player player, org.bukkit.World world) {
        if (BypassUtil.bypasses(player)) return true;
        return WorldUtil.isInOwnWorld(player) && WorldUtil.getIslandOwner(world) != null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && !BypassUtil.bypasses(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!canModify(player, event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!canModify(player, event.getBlock().getWorld())) {
            event.setCancelled(true);
            return;
        }
        if (isProtectedSpawnBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage(Msg.color("&c⚠ &7You cannot place that here."));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player
                && WorldUtil.getIslandOwner(player.getWorld()) != null
                && !WorldUtil.isInOwnWorld(player)
                && !BypassUtil.bypasses(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (event.getClickedBlock() != null
                && WorldUtil.getIslandOwner(event.getClickedBlock().getWorld()) != null
                && !canModify(player, event.getClickedBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (WorldUtil.getIslandOwner(player.getWorld()) != null
                && !canModify(player, player.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (WorldUtil.getIslandOwner(player.getWorld()) != null
                && !canModify(player, player.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (WorldUtil.getIslandOwner(player.getWorld()) != null
                && !canModify(player, player.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (WorldUtil.getIslandOwner(player.getWorld()) != null
                && !canModify(player, player.getWorld())
                && event.getCaught() != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUnleash(PlayerUnleashEntityEvent event) {
        Player player = event.getPlayer();
        if (WorldUtil.getIslandOwner(player.getWorld()) != null
                && !canModify(player, player.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!(event.getRemover() instanceof Player player)) return;
        if (WorldUtil.getIslandOwner(player.getWorld()) != null
                && !canModify(player, player.getWorld())) {
            event.setCancelled(true);
        }
    }

}