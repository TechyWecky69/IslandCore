package net.islandcore.plugin.commands;

import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.skilltree.SkillTreeManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /resetislandscore <player>
 *
 * Staff command that fully resets a player's Island Score back to 0.
 * Clears all three components that feed the score:
 *   - Visit/day data (the auto-score portion in RatingManager)
 *   - Community star ratings (1-5 star votes from visitors)
 *   - Skill tree progress (worth 40% of the total auto-score)
 */
public class ResetIslandScoreCommand implements CommandExecutor {

    private final RatingManager ratings;
    private final SkillTreeManager skillTreeManager;

    public ResetIslandScoreCommand(RatingManager ratings, SkillTreeManager skillTreeManager) {
        this.ratings = ratings;
        this.skillTreeManager = skillTreeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.resetislandscore")) return true;

        if (args.length != 1) {
            Msg.send(sender, "&cUsage: /resetislandscore <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getName() == null) {
            Msg.send(sender, "&c" + Symbols.WARNING + " &7That player has never joined before.");
            return true;
        }

        // Reset all three components of the Island Score
        ratings.resetAutoScoreData(target.getUniqueId()); // visit count + island day
        ratings.clearRatings(target.getUniqueId());       // community star votes
        skillTreeManager.resetPlayer(target.getUniqueId()); // skill tree progress (40% of score)

        Msg.send(sender, "&a" + Symbols.CHECK + " &7Fully reset &b"
                + target.getName() + "&7's Island Score to &b0&7.");
        return true;
    }
}
