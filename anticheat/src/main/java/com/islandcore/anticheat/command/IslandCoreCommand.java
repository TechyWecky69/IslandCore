package com.islandcore.anticheat.command;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import com.islandcore.anticheat.check.CheckType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class IslandCoreCommand implements CommandExecutor {

    private final IslandCoreAntiCheat plugin;

    public IslandCoreCommand(IslandCoreAntiCheat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("islandcore.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "[IslandCore] Configuration reloaded.");
            }
            case "alerts" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "Only players can toggle alerts.");
                    return true;
                }
                boolean nowDisabled = plugin.toggleAlerts(player);
                sender.sendMessage(ChatColor.YELLOW + "[IslandCore] Alerts " + (nowDisabled ? "disabled." : "enabled."));
            }
            case "vl" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /islandcore vl <player>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found or offline.");
                    return true;
                }
                sender.sendMessage(ChatColor.AQUA + "Violation levels for " + target.getName() + ":");
                Map<CheckType, Double> all = plugin.getViolationManager().getAllViolations(target);
                if (all.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "  No violations recorded.");
                } else {
                    for (Map.Entry<CheckType, Double> entry : all.entrySet()) {
                        sender.sendMessage(ChatColor.GRAY + "  " + entry.getKey().getDisplayName() + ": " + String.format("%.1f", entry.getValue()));
                    }
                }
            }
            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /islandcore reset <player>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found or offline.");
                    return true;
                }
                plugin.getViolationManager().resetViolations(target);
                sender.sendMessage(ChatColor.GREEN + "[IslandCore] Reset violations for " + target.getName() + ".");
            }
            case "check" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /islandcore check <name> <on|off>");
                    return true;
                }
                CheckType type = CheckType.fromString(args[1]);
                if (type == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown check: " + args[1]);
                    return true;
                }
                boolean value = args[2].equalsIgnoreCase("on");
                plugin.getCheckManager().setEnabled(type, value);
                sender.sendMessage(ChatColor.GREEN + "[IslandCore] " + type.getDisplayName() + " check " + (value ? "enabled." : "disabled."));
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + "--- IslandCore AntiCheat ---");
        sender.sendMessage(ChatColor.GRAY + "/islandcore reload " + ChatColor.DARK_GRAY + "- reload config.yml");
        sender.sendMessage(ChatColor.GRAY + "/islandcore alerts " + ChatColor.DARK_GRAY + "- toggle violation alerts");
        sender.sendMessage(ChatColor.GRAY + "/islandcore vl <player> " + ChatColor.DARK_GRAY + "- view violation levels");
        sender.sendMessage(ChatColor.GRAY + "/islandcore reset <player> " + ChatColor.DARK_GRAY + "- clear violations");
        sender.sendMessage(ChatColor.GRAY + "/islandcore check <name> <on|off> " + ChatColor.DARK_GRAY + "- toggle a check");
    }
}
