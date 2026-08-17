package net.islandcore.plugin.commands;

import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.kick")) return true;
        if (args.length < 1) {
            Msg.send(sender, "&c" + Symbols.WARNING + " &7Please specify a player!");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            Msg.send(sender, "&c" + Symbols.WARNING + " &7" + args[0] + " is not online!");
            return true;
        }

        if (args.length >= 2) {
            String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            targetPlayer.kickPlayer(Msg.color(reason));
            Msg.send(sender, "&cKicked &e" + targetPlayer.getName() + "&c for &e" + reason);
        } else {
            targetPlayer.kickPlayer(null);
            Msg.send(sender, "&cKicked &e" + targetPlayer.getName());
        }
        return true;
    }
}
