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

public class MsgCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.msg")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 2) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Usage: /msg <player> <message>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7" + args[0] + " is offline!");
            return true;
        }

        String text = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        String formatted = "&c[&e" + player.getName() + " &c=> &e" + targetPlayer.getName() + "&c] &b" + text;
        Msg.send(player, formatted);
        Msg.send(targetPlayer, formatted);

        ReplyManager.setReplyTarget(player.getUniqueId(), targetPlayer.getUniqueId());
        ReplyManager.setReplyTarget(targetPlayer.getUniqueId(), player.getUniqueId());
        return true;
    }
}
