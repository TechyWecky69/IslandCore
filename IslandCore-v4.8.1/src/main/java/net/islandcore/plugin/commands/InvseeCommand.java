package net.islandcore.plugin.commands;

import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InvseeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.invsee")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 1) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Please specify a player.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7The specified player is not online.");
            return true;
        }

        player.openInventory(targetPlayer.getInventory());
        Msg.send(player, "&aYou are now viewing &e" + targetPlayer.getName() + "'s &ainventory.");
        return true;
    }
}
