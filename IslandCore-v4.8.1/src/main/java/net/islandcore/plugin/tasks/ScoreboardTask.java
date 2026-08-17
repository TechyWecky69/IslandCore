package net.islandcore.plugin.tasks;

import net.islandcore.plugin.IslandCorePlugin;
import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.ranks.Rank;
import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight sidebar scoreboard.
 *
 * Each player gets one persistent scoreboard rather than a brand-new
 * scoreboard every refresh. This is important because rank teams are
 * attached to the player's scoreboard.
 */
public class ScoreboardTask extends BukkitRunnable {

    private final IslandCorePlugin plugin;
    private final DataStore data;
    private final RankManager rankManager;
    private final RatingManager ratings;

    private final String serverName;
    private final List<String> colours;

    private final Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    private int colourIndex;

    public ScoreboardTask(
            DataStore data,
            IslandCorePlugin plugin,
            RankManager rankManager,
            RatingManager ratings
    ) {
        this.plugin = plugin;
        this.data = data;
        this.rankManager = rankManager;
        this.ratings = ratings;

        this.serverName = plugin.getConfig()
                .getString("scoreboard.server-name", "Block Bound");

        this.colours = plugin.getConfig()
                .getStringList("scoreboard.flash-colours");
    }


    @Override
    public void run() {
        String title = formatTitle();

        if (!colours.isEmpty()) {
            colourIndex = (colourIndex + 1) % colours.size();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player, title);
        }

        // Remove scoreboards belonging to players who have left.
        scoreboards.keySet().removeIf(uuid ->
                Bukkit.getPlayer(uuid) == null
        );
    }

    private void updatePlayer(Player player, String title) {
        Scoreboard board = getOrCreateScoreboard(player);

        /*
         * Another plugin/server system may have replaced the player's
         * scoreboard. If that happens, give them IslandCore's scoreboard
         * again and rebuild the rank team.
         */
        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }

        /*
         * Reapply the player's rank to this scoreboard.
         *
         * This is what fixes ranks disappearing when the scoreboard changes.
         */
        Rank rank = rankManager.getRankOrDefault(player.getUniqueId());
        rankManager.setPrefix(player, rank);

        /*
         * Remove the old sidebar objective before recreating it.
         * The scoreboard itself is NOT recreated, so rank teams survive.
         */
        Objective oldObjective = board.getObjective("islandcore");
        if (oldObjective != null) {
            oldObjective.unregister();
        }

        Objective objective = board.registerNewObjective(
                "islandcore",
                "dummy",
                title
        );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        World ownIsland = WorldUtil.getIslandWorld(player.getUniqueId());
        World current = player.getWorld();
        UUID owner = WorldUtil.getIslandOwner(current);

        World island = owner != null ? current : ownIsland;

        String islandLine;

        if (owner == null) {
            islandLine = "&7No island";
        } else if (owner.equals(player.getUniqueId())) {
            islandLine = "&aYour island";
        } else {
            Player ownerPlayer = Bukkit.getPlayer(owner);
            String name = ownerPlayer != null
                    ? ownerPlayer.getName()
                    : "Player";

            islandLine = "&b" + name + "'s island";
        }

        long day = island == null
                ? 0L
                : (island.getFullTime() / 24000L) + 1L;

        int pulls = data.getLootPulls(player.getUniqueId());

        // Whichever island's rating is relevant right now: the island being
        // visited, or the player's own island if they're not on anyone's.
        UUID ratingTarget = owner != null ? owner : player.getUniqueId();
        int autoScore = ratings.getAutoScore(ratingTarget);
        int votes = ratings.getVoteCount(ratingTarget);
        String ownerBadge = ratings.ownerStarBadge(ratingTarget);
        String ratingLine = votes > 0
                ? "&fRating: &6" + ratings.formatStars(ratings.getAverage(ratingTarget)) + "&b" + ownerBadge + " &7(" + autoScore + ")"
                : "&fScore: &a" + autoScore + "&b" + ownerBadge;

        setLine(board, objective, "&7&m----------------", 8);
        setLine(board, objective, islandLine, 7);
        setLine(board, objective, ratingLine, 6);
        setLine(board, objective, "&fDay: &e" + day, 5);
        setLine(board, objective, "&fRandom pulls: &d" + pulls, 4);
        setLine(board, objective, "&7", 3);
        setLine(
                board,
                objective,
                "&fOnline: &a" + Bukkit.getOnlinePlayers().size(),
                2
        );
        setLine(board, objective, "&7&m----------------", 1);
    }

    private Scoreboard getOrCreateScoreboard(Player player) {
        UUID uuid = player.getUniqueId();

        Scoreboard board = scoreboards.get(uuid);

        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            scoreboards.put(uuid, board);
            player.setScoreboard(board);
        }

        return board;
    }

    private String formatTitle() {
        String colour;

        if (colours.isEmpty()) {
            colour = "&b";
        } else {
            colour = colours.get(colourIndex);
        }

        return Msg.color(colour + "&l" + serverName);
    }

    private void setLine(
            Scoreboard board,
            Objective objective,
            String raw,
            int score
    ) {
        String line = Msg.color(raw);

        /*
         * Reuse the existing team rather than registering a new team every
         * scoreboard refresh.
         */
        String teamName = "line" + score;

        Team team = board.getTeam(teamName);

        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        /*
         * Remove the old entry from this line's team.
         */
        for (String oldEntry : team.getEntries()) {
            team.removeEntry(oldEntry);
            board.resetScores(oldEntry);
        }

        String entry = line + "§" + Integer.toHexString(score);

        if (entry.length() > 40) {
            entry = entry.substring(0, 40);
        }

        team.addEntry(entry);
        objective.getScore(entry).setScore(score);
    }

    /**
     * Removes a player's cached scoreboard.
     * Called when a player leaves.
     */
    public void removePlayer(Player player) {
        scoreboards.remove(player.getUniqueId());
    }

    /**
     * Removes all cached scoreboards.
     */
    public void shutdown() {
        scoreboards.clear();
    }
}