package net.islandcore.plugin.commands;

import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;

public class TopIslandsCommand implements CommandExecutor {

    private static final int LEADERBOARD_SIZE = 10;

    private final RatingManager ratings;

    public TopIslandsCommand(RatingManager ratings) {
        this.ratings = ratings;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.topislands")) return true;

        List<UUID> top = List.copyOf(ratings.topIslandsByAutoScore(LEADERBOARD_SIZE));

        if (top.isEmpty()) {
            Msg.send(sender, "&7No islands to rank yet.");
            return true;
        }

        Msg.send(sender, "&8&m----------&r &b&lTop Islands &8&m----------");

        int place = 1;
        for (UUID uuid : top) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(uuid);
            String name = owner.getName() != null ? owner.getName() : "Unknown";

            int autoScore = ratings.getAutoScore(uuid);
            double avg = ratings.getAverage(uuid);
            int votes = ratings.getVoteCount(uuid);

            String badge = ratings.ownerStarBadge(uuid);
            String stars = votes > 0
                    ? "&6" + ratings.formatStars(avg) + badge + " &7(" + votes + ")"
                    : "&7Not yet rated" + badge;

            Msg.send(sender, "&e#" + place + " &b" + name + " &7- &fScore: &a" + autoScore + " &7| " + stars);
            place++;
        }

        Msg.send(sender, "&8&m--------------------------------");
        return true;
    }
}
