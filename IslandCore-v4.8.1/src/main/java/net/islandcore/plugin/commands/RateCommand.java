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
import org.bukkit.entity.Player;

public class RateCommand implements CommandExecutor {

    private final RatingManager ratings;

    public RateCommand(RatingManager ratings) {
        this.ratings = ratings;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.rate")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length < 2) {
            Msg.send(player, "&cUsage: /rate <player> <1-5>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target.getUniqueId().equals(player.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You can't rate your own island!");
            return true;
        }

        int score;
        try {
            score = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Rating must be a number from 1 to 5.");
            return true;
        }

        RatingManager.RateResult result = ratings.rate(player.getUniqueId(), target.getUniqueId(), score);

        switch (result) {
            case SUCCESS -> {
                String stars = ratings.formatStars(score);
                Msg.send(player, "&a" + Symbols.CHECK + " &7You rated &b" + target.getName() + "&7's island &6" + stars + " &7(" + score + "/5).");
            }
            case SELF -> Msg.send(player, "&c" + Symbols.WARNING + " &7You can't rate your own island!");
            case TOO_SOON -> Msg.send(player, "&c" + Symbols.WARNING + " &7Visit and spend a bit of time on the island before rating it.");
            case ON_COOLDOWN -> Msg.send(player, "&c" + Symbols.WARNING + " &7You've already rated this island recently. Try again later.");
            case INVALID_SCORE -> Msg.send(player, "&c" + Symbols.WARNING + " &7Rating must be a number from 1 to 5.");
        }

        return true;
    }
}
