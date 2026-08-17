package net.islandcore.plugin.commands;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /myisland
 *
 * Combines the old /day and /myrating into one dashboard command.
 * Shows island day, visit toggle status, community star rating and Island Score.
 */
public class MyIslandCommand implements CommandExecutor {

    private final RatingManager ratings;
    private final DataStore data;

    public MyIslandCommand(RatingManager ratings, DataStore data) {
        this.ratings = ratings;
        this.data = data;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.myisland")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        World world = WorldUtil.getIslandWorld(player.getUniqueId());
        if (world == null) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Your island isn't loaded right now.");
            return true;
        }

        long day = (world.getFullTime() / 24000L) + 1L;

        double avg = ratings.getAverage(player.getUniqueId());
        int votes = ratings.getVoteCount(player.getUniqueId());
        int autoScore = ratings.getAutoScore(player.getUniqueId());
        int uniqueVisitors = ratings.getUniqueVisitorCount(player.getUniqueId());
        int totalVisits = ratings.getTotalVisitCount(player.getUniqueId());
        boolean visitable = data.isVisitable(player.getUniqueId());
        String badge = ratings.ownerStarBadge(player.getUniqueId());

        String starsLine = votes > 0
                ? "&6" + ratings.formatStars(avg) + badge
                        + " &7(" + votes + (votes == 1 ? " rating, " : " ratings, ")
                        + String.format("%.1f", avg) + "/5)"
                : "&7No ratings yet" + badge;

        String visitStatus = visitable
                ? "&aOpen &7(players can visit)"
                : "&cClosed &7(visits disabled)";

        Msg.send(player, "&8&m----------&r &b&lMy Island &8&m----------");
        Msg.send(player, "&7Day: &e" + day);
        Msg.send(player, "&7Visits: " + visitStatus);
        Msg.send(player, "&7Unique visitors: &b" + uniqueVisitors + " &8(&7" + totalVisits + " total&8)");
        Msg.send(player, "&7Community rating: " + starsLine);
        Msg.send(player, "&7Island Score: &a" + autoScore + " &8/ &a100");
        Msg.send(player, "&8&m--------------------------------------");
        return true;
    }
}
