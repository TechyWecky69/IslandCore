package net.islandcore.plugin.ranks;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.skilltree.SkillTreeManager;
import net.islandcore.plugin.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RankManager {

    private final JavaPlugin plugin;
    private final DataStore data;
    private SkillTreeManager skillTreeManager;
    private RatingManager ratingManager;

    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public RankManager(JavaPlugin plugin, DataStore data) {
        this.plugin = plugin;
        this.data = data;
    }

    /**
     * Wire in the SkillTreeManager after construction (avoids a circular
     * dependency at plugin startup time).
     */
    public void setSkillTreeManager(SkillTreeManager skillTreeManager) {
        this.skillTreeManager = skillTreeManager;
    }

    /**
     * Wire in the RatingManager after construction, same reasoning as
     * {@link #setSkillTreeManager}. Used to show the owner star badge
     * after a player's name in the tab list.
     */
    public void setRatingManager(RatingManager ratingManager) {
        this.ratingManager = ratingManager;
    }

    public Rank getRank(UUID uuid) {
        return Rank.fromName(data.getRank(uuid));
    }

    public Rank getRankOrDefault(UUID uuid) {
        Rank rank = getRank(uuid);
        return rank == null ? Rank.MEMBER : rank;
    }

    public void ensureRank(Player player) {
        if (getRank(player.getUniqueId()) == null) {
            setRank(player, Rank.MEMBER, false);
        } else {
            apply(player);
            updateTabName(player);
            setPrefix(player, getRankOrDefault(player.getUniqueId()));
        }
    }

    public void setRank(Player target, Rank rank, boolean save) {
        data.setRank(
                target.getUniqueId(),
                rank.name().toLowerCase(),
                save
        );

        apply(target);
        updateTabName(target);
        setPrefix(target, rank);
    }

    public void setRank(UUID uuid, Rank rank) {
        data.setRank(
                uuid,
                rank.name().toLowerCase(),
                true
        );

        Player player = plugin.getServer().getPlayer(uuid);

        if (player != null) {
            apply(player);
            updateTabName(player);
            setPrefix(player, rank);
        }
    }

    /**
     * Applies the rank prefix to the scoreboard currently being used
     * by the player.
     *
     * This is deliberately NOT the main scoreboard. IslandCore's sidebar
     * uses an individual scoreboard for every player, so the rank team must
     * exist on that same scoreboard.
     */
    public void setPrefix(Player player, Rank rank) {
        Scoreboard scoreboard = player.getScoreboard();

        if (scoreboard == null) {
            return;
        }

        /*
         * Remove the player from every existing IslandCore rank team.
         * This prevents a player changing rank from remaining in their
         * previous team.
         */
        for (Rank existingRank : Rank.values()) {
            Team existingTeam = scoreboard.getTeam(existingRank.name());

            if (existingTeam != null) {
                existingTeam.removeEntry(player.getName());
            }
        }

        Team team = scoreboard.getTeam(rank.name());

        if (team == null) {
            team = scoreboard.registerNewTeam(rank.name());
        }

        // Nametag above the player's head shows: [Rank] [Symbol] Name
        String symbol = skillTreeManager != null
                ? skillTreeManager.getActiveSymbol(player.getUniqueId())
                : "";

        team.setPrefix(Msg.color(rank.getPrefix() + " &f" + symbol + " "));

        /*
         * Add the player to the correct rank team.
         */
        team.addEntry(player.getName());
    }

    public void apply(Player player) {
        PermissionAttachment old =
                attachments.remove(player.getUniqueId());

        if (old != null) {
            player.removeAttachment(old);
        }

        Rank rank = getRankOrDefault(player.getUniqueId());

        PermissionAttachment attachment =
                player.addAttachment(plugin);

        /*
         * Start from a clean state for every IslandCore permission so a
         * player changing rank cannot retain permissions from their
         * previous rank.
         */
        for (String permission : Rank.ALL_PERMISSIONS) {
            attachment.setPermission(permission, false);
        }

        for (String permission : rank.getPermissions()) {
            attachment.setPermission(permission, true);
        }

        /*
         * Operators are server administrators. Give them every IslandCore
         * permission regardless of their stored rank.
         */
        if (player.isOp()) {
            for (String permission : Rank.ALL_PERMISSIONS) {
                attachment.setPermission(permission, true);
            }
        }

        attachments.put(player.getUniqueId(), attachment);

        /*
         * The client builds its command tree from the permissions it has
         * when the tree is sent. Refresh on the next tick.
         */
        plugin.getServer().getScheduler().runTask(
                plugin,
                player::updateCommands
        );
    }

    public void updateTabName(Player player) {
        Rank rank = getRankOrDefault(player.getUniqueId());
        String symbol = skillTreeManager != null
                ? skillTreeManager.getActiveSymbol(player.getUniqueId()) + " "
                : "";
        // Owner star badge goes after the name in the tab list only — chat
        // format is built separately in RankListener and never touches this.
        String ownerBadge = ratingManager != null
                ? ratingManager.ownerStarBadge(player.getUniqueId())
                : "";

        player.setPlayerListName(
                Msg.color(
                        rank.getPrefix()
                                + " " + symbol
                                + "&f" + player.getName()
                                + ownerBadge
                )
        );
    }

    public String getChatPrefix(UUID uuid) {
        return getRankOrDefault(uuid).getPrefix();
    }

    /**
     * Returns the full chat prefix including the player's active skill
     * symbol, e.g.: {@code [Member] [⛏]}
     */
    public String getFullChatPrefix(UUID uuid) {
        String rankPrefix = getRankOrDefault(uuid).getPrefix();
        if (skillTreeManager == null) {
            return rankPrefix;
        }
        return rankPrefix + " " + skillTreeManager.getActiveSymbol(uuid);
    }

    public void remove(Player player) {
        PermissionAttachment attachment =
                attachments.remove(player.getUniqueId());

        if (attachment != null) {
            player.removeAttachment(attachment);
        }
    }

    public void removeAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            remove(player);
        }
    }
}
