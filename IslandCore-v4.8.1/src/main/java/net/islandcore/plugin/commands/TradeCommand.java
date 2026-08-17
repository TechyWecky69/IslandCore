package net.islandcore.plugin.commands;

import net.islandcore.plugin.trade.TradeGUI;
import net.islandcore.plugin.trade.TradeManager;
import net.islandcore.plugin.trade.TradeRequestPrompt;
import net.islandcore.plugin.trade.TradeSession;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TradeCommand implements CommandExecutor, TabCompleter {

    private final TradeManager tradeManager;
    private final TradeGUI tradeGUI;

    public TradeCommand(TradeManager tradeManager, TradeGUI tradeGUI) {
        this.tradeManager = tradeManager;
        this.tradeGUI = tradeGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Usage: /trade <player>");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept") && args.length >= 2) {
            accept(player, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("deny") && args.length >= 2) {
            deny(player, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            cancel(player);
            return true;
        }

        request(player, args[0]);
        return true;
    }

    private void request(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7" + targetName + " is offline!");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You can't trade with yourself!");
            return;
        }

        if (!validateSameIsland(player, target)) return;
        if (!validateNotTrading(player, target)) return;

        // If the target already sent us a request, treat this as accepting it
        // instead of creating a duplicate request going the other way.
        if (tradeManager.hasPendingRequest(player.getUniqueId(), target.getUniqueId())) {
            tradeManager.clearRequest(player.getUniqueId());
            startTrade(player, target);
            return;
        }

        if (tradeManager.hasPendingRequest(target.getUniqueId(), player.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You already sent &e" + target.getName() + " &7a trade request!");
            return;
        }

        tradeManager.addRequest(target.getUniqueId(), player.getUniqueId());
        Msg.send(player, "&bTrade request sent to &e" + target.getName() + "&b!");
        TradeRequestPrompt.send(target, player.getName());
    }

    private void accept(Player player, String requesterName) {
        Player requester = Bukkit.getPlayer(requesterName);
        if (requester == null) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7" + requesterName + " is offline!");
            return;
        }

        if (!tradeManager.hasPendingRequest(player.getUniqueId(), requester.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You don't have a pending trade request from &e" + requesterName + "&7!");
            return;
        }

        tradeManager.clearRequest(player.getUniqueId());

        if (!validateSameIsland(player, requester)) return;
        if (!validateNotTrading(player, requester)) return;

        startTrade(player, requester);
    }

    private void deny(Player player, String requesterName) {
        Player requester = Bukkit.getPlayer(requesterName);
        if (requester == null || !tradeManager.hasPendingRequest(player.getUniqueId(), requester.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You don't have a pending trade request from &e" + requesterName + "&7!");
            return;
        }

        tradeManager.clearRequest(player.getUniqueId());
        Msg.send(player, "&c" + Symbols.CHECK + " &7Denied &e" + requester.getName() + "&7's trade request.");
        Msg.send(requester, "&c" + Symbols.WARNING + " &7" + player.getName() + " &7denied your trade request.");
    }

    private void cancel(Player player) {
        TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You're not currently trading with anyone.");
            return;
        }
        player.closeInventory();
    }

    private void startTrade(Player a, Player b) {
        TradeSession session = tradeManager.startSession(a.getUniqueId(), b.getUniqueId());
        tradeGUI.open(session);
    }

    private boolean validateSameIsland(Player player, Player target) {
        if (!player.getWorld().equals(target.getWorld())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You need to be on the same island as &e"
                    + target.getName() + " &7to trade with them!");
            return false;
        }
        return true;
    }

    private boolean validateNotTrading(Player player, Player target) {
        if (tradeManager.isTrading(player.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You're already in a trade!");
            return false;
        }
        if (tradeManager.isTrading(target.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7" + target.getName() + " &7is already in a trade!");
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("accept", "deny", "cancel"));
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId())) options.add(online.getName());
            }
            return partial(args[0], options);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.getUniqueId().equals(player.getUniqueId())) names.add(online.getName());
            }
            return partial(args[1], names);
        }

        return List.of();
    }

    private List<String> partial(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream().filter(option -> option.toLowerCase().startsWith(lower)).toList();
    }
}
