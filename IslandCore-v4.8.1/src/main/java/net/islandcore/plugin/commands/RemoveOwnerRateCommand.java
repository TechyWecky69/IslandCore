package net.islandcore.plugin.commands;

import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /removeownerrate <player>
 *
 * Owner-only command that takes back a previously-awarded owner star
 * badge. See {@link OwnerRateCommand} to award it.
 */
public class RemoveOwnerRateCommand implements CommandExecutor {

    private final RatingManager ratings;
    private final RankManager ranks;

    public RemoveOwnerRateCommand(RatingManager ratings, RankManager ranks) {
        this.ratings = ratings;
        this.ranks = ranks;
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.ownerrate")) return true;

        if (args.length != 1) {
            Msg.send(sender, "&cUsage: /removeownerrate <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null) {
            Msg.send(sender, "&c" + Symbols.WARNING + " &7That player has never joined before.");
            return true;
        }

        if (!ratings.hasOwnerStar(target.getUniqueId())) {
            Msg.send(sender, "&7" + target.getName() + "'s island doesn't have the owner star.");
            return true;
        }

        ratings.setOwnerStar(target.getUniqueId(), false);

        Player online = target.getPlayer();
        if (online != null) {
            ranks.updateTabName(online);
        }

        Msg.send(sender, "&a" + Symbols.CHECK + " &7Removed the owner star from &b"
                + target.getName() + "&7's island.");
        return true;
    }
}
