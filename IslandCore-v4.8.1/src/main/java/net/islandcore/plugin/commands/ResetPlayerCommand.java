package net.islandcore.plugin.commands;

import net.islandcore.plugin.IslandCorePlugin;
import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.managers.IslandManager;
import net.islandcore.plugin.skilltree.SkillTreeManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /resetplayer <player>
 *
 * Wipes a player back to a brand-new state: clears their live inventory,
 * deletes their island world, and clears their skill tree progress, then
 * kicks them so the normal join flow recreates everything from scratch.
 *
 * This is destructive and irreversible, so the command must be run twice
 * for the same target within a 10 second window to actually happen. If the
 * second run doesn't come in time, the pending reset is cancelled and the
 * player runs the command again to restart.
 */
public class ResetPlayerCommand implements CommandExecutor, Listener {

    private static final int CONFIRM_SECONDS = 10;

    private final IslandCorePlugin plugin;
    private final DataStore dataStore;
    private final IslandManager islandManager;
    private final SkillTreeManager skillTreeManager;

    /** Keyed by target UUID - only one pending reset per target at a time. */
    private final Map<UUID, PendingReset> pending = new ConcurrentHashMap<>();

    public ResetPlayerCommand(IslandCorePlugin plugin, DataStore dataStore,
                               IslandManager islandManager, SkillTreeManager skillTreeManager) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.islandManager = islandManager;
        this.skillTreeManager = skillTreeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.resetplayer")) return true;

        if (args.length != 1) {
            Msg.send(sender, "&cUsage: /resetplayer <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.send(sender, "&c" + Symbols.WARNING + " &7" + args[0] + " is not online!");
            return true;
        }

        UUID targetId = target.getUniqueId();
        PendingReset existing = pending.remove(targetId);

        if (existing != null) {
            existing.task.cancel();
            Msg.send(sender, "&cConfirmed. Resetting &e" + target.getName() + "&c now...");
            performReset(sender, target);
            return true;
        }

        String targetName = target.getName();
        Msg.send(sender, "&4&lWARNING! &cThis will permanently wipe &e" + targetName
                + "&c's inventory, island world and skill tree, then kick them.");
        Msg.send(sender, "&7Run &f/resetplayer " + targetName + " &7again within &e"
                + CONFIRM_SECONDS + "s &7to confirm, or do nothing to cancel.");

        PendingReset fresh = new PendingReset();
        fresh.secondsLeft = CONFIRM_SECONDS;
        fresh.task = new BukkitRunnable() {
            @Override
            public void run() {
                fresh.secondsLeft--;

                if (fresh.secondsLeft <= 0) {
                    pending.remove(targetId);
                    Msg.send(sender, "&7Reset confirmation for &e" + targetName + "&7 expired. Nothing was changed.");
                    cancel();
                    return;
                }

                Msg.send(sender, "&7Confirm reset for &e" + targetName + "&7 in &c"
                        + fresh.secondsLeft + "s&7...");
            }
        }.runTaskTimer(plugin, 20L, 20L);

        pending.put(targetId, fresh);
        return true;
    }

    private void performReset(CommandSender sender, Player target) {
        UUID targetId = target.getUniqueId();
        String name = target.getName();

        islandManager.cancelPendingUnload(targetId);

        // Get them out of the island world before it gets deleted from under them.
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mvtp " + name + " world");

        // Give the teleport a few ticks to land before we touch inventory/world/data.
        new BukkitRunnable() {
            @Override
            public void run() {
                Player online = Bukkit.getPlayer(targetId);

                if (online != null) {
                    online.getInventory().clear();
                    online.getInventory().setArmorContents(new ItemStack[4]);
                    online.getInventory().setItemInOffHand(null);
                }

                WorldUtil.deleteIsland(targetId);
                skillTreeManager.resetPlayer(targetId);
                dataStore.resetPlayer(targetId);


                Msg.send(sender, "&aReset of &e" + name + "&a complete.");

                if (online != null) {
                    online.kickPlayer(Msg.color("&aYour island has been reset. Rejoin to start fresh!"));
                }
            }
        }.runTaskLater(plugin, 5L);
    }

    /** Cancels any pending reset if the target disconnects before it's confirmed. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PendingReset removed = pending.remove(event.getPlayer().getUniqueId());
        if (removed != null) {
            removed.task.cancel();
        }
    }

    private static class PendingReset {
        int secondsLeft;
        BukkitTask task;
    }
}
