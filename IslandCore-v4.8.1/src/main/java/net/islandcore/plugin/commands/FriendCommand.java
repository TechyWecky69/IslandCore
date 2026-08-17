package net.islandcore.plugin.commands;

import net.islandcore.plugin.friends.FriendManager;
import net.islandcore.plugin.friends.FriendRequestPrompt;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final FriendManager friends;

    public FriendCommand(FriendManager friends) {
        this.friends = friends;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> add(player, args);
            case "remove" -> remove(player, args);
            case "accept" -> accept(player, args);
            case "deny" -> deny(player, args);
            case "list" -> list(player);
            default -> sendUsage(player);
        }
        return true;
    }

    private void sendUsage(Player player) {
        Msg.send(player, "&c" + Symbols.WARNING + " &7Usage: /friend <add|remove|accept|deny|list> [player]");
    }

    private void add(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Please specify a player!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7" + args[1] + " is offline!");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You can't friend yourself!");
            return;
        }

        if (friends.areFriends(player.getUniqueId(), target.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You're already friends with &e" + target.getName() + "&7!");
            return;
        }

        // If the target already sent us a request, accept it instead of
        // creating a duplicate one going the other way.
        if (friends.hasPendingRequest(player.getUniqueId(), target.getUniqueId())) {
            friends.acceptRequest(player.getUniqueId(), target.getUniqueId());
            Msg.send(player, "&a" + Symbols.CHECK + " &7You are now friends with &e" + target.getName() + "&7!");
            Msg.send(target, "&a" + Symbols.CHECK + " &7" + player.getName() + " &7accepted your friend request!");
            return;
        }

        if (friends.hasPendingRequest(target.getUniqueId(), player.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You already sent &e" + target.getName() + " &7a friend request!");
            return;
        }

        friends.addRequest(target.getUniqueId(), player.getUniqueId());
        Msg.send(player, "&bFriend request sent to &e" + target.getName() + "&b!");
        FriendRequestPrompt.send(target, player.getName());
    }

    private void remove(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Please specify a player!");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!friends.areFriends(player.getUniqueId(), target.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You're not friends with &e" + args[1] + "&7!");
            return;
        }

        friends.removeFriend(player.getUniqueId(), target.getUniqueId());
        Msg.send(player, "&c" + Symbols.CHECK + " &7Removed &e" + args[1] + " &7from your friends list.");

        Player targetPlayer = target.getPlayer();
        if (targetPlayer != null) {
            Msg.send(targetPlayer, "&c" + Symbols.CHECK + " &7" + player.getName() + " &7removed you from their friends list.");
        }
    }

    private void accept(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Please specify a player!");
            return;
        }

        OfflinePlayer requester = Bukkit.getOfflinePlayer(args[1]);
        if (!friends.hasPendingRequest(player.getUniqueId(), requester.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You don't have a pending request from &e" + args[1] + "&7!");
            return;
        }

        friends.acceptRequest(player.getUniqueId(), requester.getUniqueId());
        Msg.send(player, "&a" + Symbols.CHECK + " &7You are now friends with &e" + requester.getName() + "&7!");

        Player requesterPlayer = requester.getPlayer();
        if (requesterPlayer != null) {
            Msg.send(requesterPlayer, "&a" + Symbols.CHECK + " &7" + player.getName() + " &7accepted your friend request!");
        }
    }

    private void deny(Player player, String[] args) {
        if (args.length < 2) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Please specify a player!");
            return;
        }

        OfflinePlayer requester = Bukkit.getOfflinePlayer(args[1]);
        if (!friends.hasPendingRequest(player.getUniqueId(), requester.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You don't have a pending request from &e" + args[1] + "&7!");
            return;
        }

        friends.denyRequest(player.getUniqueId(), requester.getUniqueId());
        Msg.send(player, "&c" + Symbols.CHECK + " &7Denied &e" + requester.getName() + "&7's friend request.");
    }

    private void list(Player player) {
        Set<UUID> friendIds = friends.getFriends(player.getUniqueId());

        Msg.send(player, "&6&lFriends&e--------------------");

        if (friendIds.isEmpty()) {
            Msg.send(player, "&7You don't have any friends added yet.");
        } else {
            List<OfflinePlayer> sorted = new ArrayList<>();
            for (UUID uuid : friendIds) sorted.add(Bukkit.getOfflinePlayer(uuid));

            sorted.sort((a, b) -> {
                if (a.isOnline() != b.isOnline()) return a.isOnline() ? -1 : 1;
                String nameA = a.getName() == null ? "" : a.getName();
                String nameB = b.getName() == null ? "" : b.getName();
                return nameA.compareToIgnoreCase(nameB);
            });

            for (OfflinePlayer friend : sorted) {
                String name = friend.getName() == null ? "Unknown" : friend.getName();
                if (friend.isOnline()) {
                    Msg.send(player, "&a" + name + " &7- &aOnline");
                } else {
                    Msg.send(player, "&7" + name + " &7- &7Offline");
                }
            }
        }

        Set<UUID> pending = friends.getPendingRequests(player.getUniqueId());
        if (!pending.isEmpty()) {
            Msg.send(player, "&7Pending requests:");
            for (UUID uuid : pending) {
                OfflinePlayer requester = Bukkit.getOfflinePlayer(uuid);
                String name = requester.getName() == null ? "Unknown" : requester.getName();
                Msg.send(player, "&b" + name + " &7- &e/friend accept " + name);
            }
        }

        Msg.send(player, "&e-----------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        if (args.length == 1) {
            return partial(args[0], List.of("add", "remove", "accept", "deny", "list"));
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("add")) {
                List<String> names = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.getUniqueId().equals(player.getUniqueId())) names.add(online.getName());
                }
                return partial(args[1], names);
            }

            if (sub.equals("remove")) {
                List<String> names = new ArrayList<>();
                for (UUID uuid : friends.getFriends(player.getUniqueId())) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    if (op.getName() != null) names.add(op.getName());
                }
                return partial(args[1], names);
            }

            if (sub.equals("accept") || sub.equals("deny")) {
                List<String> names = new ArrayList<>();
                for (UUID uuid : friends.getPendingRequests(player.getUniqueId())) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    if (op.getName() != null) names.add(op.getName());
                }
                return partial(args[1], names);
            }
        }

        return List.of();
    }

    private List<String> partial(String input, List<String> options) {
        String lower = input.toLowerCase();
        return options.stream().filter(option -> option.toLowerCase().startsWith(lower)).toList();
    }
}
