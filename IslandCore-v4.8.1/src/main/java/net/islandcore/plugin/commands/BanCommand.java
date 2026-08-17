package net.islandcore.plugin.commands;

import net.islandcore.plugin.util.DurationUtil;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * Preserves the original script's duration tokens (1day, 1week, 2weeks, 1month,
 * 3months, 6months, 9months, 1year) plus a permanent ban when no token is given.
 *
 * Unlike the original Skript (which used "wait X" + "unban" - an in-memory
 * delay that would be lost on a server restart), this uses Bukkit's native
 * expiring ban entries, so bans still lift on schedule even after a reboot.
 */
public class BanCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.ban")) return true;
        if (args.length < 1) {
            Msg.send(sender, "&c‼ &7Please specify a player!");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            Msg.send(sender, "&4" + args[0] + " is not online!");
            return true;
        }

        if (args.length < 2) {
            Msg.send(sender, "&c" + Symbols.WARNING + " &7Please give a reason!");
            return true;
        }
        String reason = args[1];
        String durationToken = args.length >= 3 ? args[2] : null;
        String source = sender.getName();

        if (durationToken != null) {
            if (!DurationUtil.isValidToken(durationToken)) {
                Msg.send(sender, "&c" + Symbols.WARNING + " &7Unknown duration. Use one of: 1day, 1week, 2weeks, 1month, 3months, 6months, 9months, 1year");
                return true;
            }
            Duration duration = Duration.ofMillis(DurationUtil.millis(durationToken));
            String durationLabel = DurationUtil.label(durationToken);
            targetPlayer.kickPlayer(Msg.color("&4Banned for " + durationLabel + "\n&f" + reason));
            targetPlayer.ban(Msg.color("&4Banned for " + durationLabel + "\n&f" + reason), duration, source, false);
            Msg.send(sender, "&cBanned &e" + targetPlayer.getName() + "&c for &e" + durationLabel);
        } else {
            targetPlayer.kickPlayer(Msg.color("&4Banned Perminantely\n&f" + reason));
            targetPlayer.ban(Msg.color("&4Banned Perminantely\n&f" + reason), (Duration) null, source, false);
            Msg.send(sender, "&cBanned &e" + targetPlayer.getName());
        }
        return true;
    }
}
