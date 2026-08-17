package net.islandcore.plugin.commands;

import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.util.Colors;
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
 * /ownerrate <player>
 *
 * Owner-only command that awards a light blue owner star badge, shown
 * right next to the island's normal 1-5 community star rating everywhere
 * that rating is displayed (top islands, visit menu, player context menu,
 * sidebar), and after the player's name in the tab list (never in chat).
 * See {@link RemoveOwnerRateCommand} to take it back.
 */
public class OwnerRateCommand implements CommandExecutor {

    private final RatingManager ratings;
    private final RankManager ranks;

    public OwnerRateCommand(RatingManager ratings, RankManager ranks) {
        this.ratings = ratings;
        this.ranks = ranks;
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.ownerrate")) return true;

        if (args.length != 1) {
            Msg.send(sender, "&cUsage: /ownerrate <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null) {
            Msg.send(sender, "&c" + Symbols.WARNING + " &7That player has never joined before.");
            return true;
        }

        if (ratings.hasOwnerStar(target.getUniqueId())) {
            Msg.send(sender, "&7" + target.getName() + "'s island already has the owner star.");
            return true;
        }

        ratings.setOwnerStar(target.getUniqueId(), true);

        // Refresh their tab list entry immediately if they're online, rather
        // than waiting for the next rank change or symbol switch to do it.
        Player online = target.getPlayer();
        if (online != null) {
            ranks.updateTabName(online);
        }

        Msg.send(sender, "&a" + Symbols.CHECK + " &7Awarded the " + Colors.LIGHT_BLUE + Symbols.STAR_FULL
                + " &7owner star to &b" + target.getName() + "&7's island.");
        return true;
    }
}
