package net.islandcore.plugin.commands;

import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.ReplyManager;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ReplyCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.reply")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 1) {
            Msg.send(player, "&cUsage: /reply <message>");
            return true;
        }

        UUID targetUuid = ReplyManager.getReplyTarget(player.getUniqueId());
        if (targetUuid == null) {
            Msg.send(player, "&4You have nobody to reply to!");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetUuid);
        if (targetPlayer == null) {
            Msg.send(player, "&c" + Symbols.WARNING + " &fThat player is offline!");
            return true;
        }

        String text = String.join(" ", args);
        String formatted = "&c[&e" + player.getName() + " &c=> &e" + targetPlayer.getName() + "&c] &b" + text;
        Msg.send(player, formatted);
        Msg.send(targetPlayer, formatted);
        return true;
    }
}
