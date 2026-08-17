package net.islandcore.plugin.commands;

import net.islandcore.plugin.util.*;
import net.islandcore.plugin.managers.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements CommandExecutor {
    private final IslandManager islandManager;

    public HomeCommand(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.home")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        long remaining = TeleportCooldownManager.remainingSeconds(player.getUniqueId());
        if (remaining > 0) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Please wait &e" + remaining + "s &7before teleporting again.");
            return true;
        }

        if (!WorldUtil.islandExists(player.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You don't have an island yet!");
            return true;
        }

        if (!islandManager.load(player.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Your island could not be loaded right now.");
            return true;
        }

        // Multiverse may need a few ticks to finish loading the world.
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("IslandCore"),
                () -> {
                    if (!player.isOnline()) return;
                    World world = WorldUtil.getIslandWorld(player.getUniqueId());
                    if (world == null) {
                        Msg.send(player, "&c" + Symbols.WARNING + " &7Your island could not be loaded right now.");
                        return;
                    }

                    Location spawn = world.getSpawnLocation();
                    player.teleport(spawn);
                    player.setAllowFlight(false);
                    TeleportCooldownManager.markTeleport(player.getUniqueId());
                },
                10L
        );
        return true;
    }
}
