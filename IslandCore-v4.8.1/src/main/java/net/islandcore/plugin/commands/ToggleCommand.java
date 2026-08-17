package net.islandcore.plugin.commands;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.util.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCommand implements CommandExecutor {

    private final DataStore data;

    public ToggleCommand(DataStore data) {
        this.data = data;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.toggle")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!WorldUtil.isInOwnWorld(player)) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You can't run that here!");
            return true;
        }

        boolean nowActive = !data.isLootingActive(player.getUniqueId());
        data.setLootingActive(player.getUniqueId(), nowActive);

        if (nowActive) {
            Msg.send(player, "&dLooting started");
            CountdownManager.set(player.getUniqueId(), 10);
        } else {
            Msg.send(player, "&dLooting ended");
        }
        return true;
    }
}
