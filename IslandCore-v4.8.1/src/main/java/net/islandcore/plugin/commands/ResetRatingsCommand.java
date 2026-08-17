package net.islandcore.plugin.commands;

import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /resetratings <player>
 *
 * Staff (Admin and Owner) command that wipes every community vote an
 * island has received, putting its star rating back to 0. Does not touch
 * the automatic Island Score or the owner star badge - those aren't
 * community votes.
 */
public class ResetRatingsCommand implements CommandExecutor {

    private final RatingManager ratings;

    public ResetRatingsCommand(RatingManager ratings) {
        this.ratings = ratings;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.resetratings")) return true;

        if (args.length != 1) {
            Msg.send(sender, "&cUsage: /resetratings <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null) {
            Msg.send(sender, "&c" + Symbols.WARNING + " &7That player has never joined before.");
            return true;
        }

        int removed = ratings.clearRatings(target.getUniqueId());

        if (removed == 0) {
            Msg.send(sender, "&7" + target.getName() + "'s island had no ratings to clear.");
            return true;
        }

        Msg.send(sender, "&a" + Symbols.CHECK + " &7Cleared &e" + removed + " &7rating"
                + (removed == 1 ? "" : "s") + " for &b" + target.getName()
                + "&7's island. Their star rating is back to &60&7.");
        return true;
    }
}
