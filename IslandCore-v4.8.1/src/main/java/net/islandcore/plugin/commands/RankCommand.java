package net.islandcore.plugin.commands;

import net.islandcore.plugin.ranks.Rank;
import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RankCommand implements CommandExecutor, TabCompleter {
    private final RankManager ranks;

    public RankCommand(RankManager ranks) {
        this.ranks = ranks;
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.rank.manage")) return true;

        if (args.length == 0) {
            Msg.send(sender, "&c" + Symbols.WARNING + " &7Usage: /rank set <player> <rank> or /rank view <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                Msg.send(sender, "&c" + Symbols.WARNING + " &7Please specify a player!");
                return true;
            }
            if (args.length < 3) {
                Msg.send(sender, "&c" + Symbols.WARNING + " &7Please specify a rank!");
                return true;
            }

            Rank rank = Rank.fromName(args[2]);
            if (rank == null) {
                Msg.send(sender, "&c" + Symbols.WARNING + " &7Unknown rank! Use member, helper, admin, or owner.");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            ranks.setRank(target.getUniqueId(), rank);
            Msg.send(sender, "&bSet &a" + target.getName() + "&b's rank to " + Msg.color(rank.getPrefix()));
            return true;
        }

        if (args[0].equalsIgnoreCase("view")) {
            if (args.length < 2) {
                Msg.send(sender, "&c" + Symbols.WARNING + " &7Please specify a player!");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            Rank rank = ranks.getRank(target.getUniqueId());
            String displayed = rank == null ? "&7[&8Member&7]" : rank.getPrefix();
            Msg.send(sender, "&b" + target.getName() + "'s rank is: " + displayed);
            return true;
        }

        Msg.send(sender, "&c" + Symbols.WARNING + " &7Usage: /rank set <player> <rank> or /rank view <player>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!PermissionUtil.has(sender, "islandcore.rank.manage")) return List.of();
        if (args.length == 1) {
            return partial(args[0], List.of("set", "view"));
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("view"))) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
            return partial(args[1], names);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return partial(args[2], Arrays.stream(Rank.values()).map(r -> r.name().toLowerCase()).toList());
        }
        return List.of();
    }

    private List<String> partial(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream().filter(option -> option.toLowerCase().startsWith(lower)).toList();
    }
}
