package net.islandcore.plugin.commands;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.managers.IslandManager;
import net.islandcore.plugin.gui.VisitConfirmGUI;
import net.islandcore.plugin.util.*;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VisitCommand implements CommandExecutor {

    private final DataStore data;
    private final VisitConfirmGUI confirmGUI;
    private final IslandManager islandManager;

    public VisitCommand(DataStore data, VisitConfirmGUI confirmGUI, IslandManager islandManager) {
        this.data = data;
        this.confirmGUI = confirmGUI;
        this.islandManager = islandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.visit")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 1) {
            Msg.send(player, "&cUsage: /visit <player>");
            return true;
        }

        long remaining = TeleportCooldownManager.remainingSeconds(player.getUniqueId());
        if (remaining > 0) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Please wait &e" + remaining + "s &7before teleporting again.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (target.getUniqueId().equals(player.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You can not visit yourself! Run /home to go home!");
            return true;
        }

        if (!WorldUtil.islandExists(target.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Oops! Looks like " + args[0] + " does not have an island!");
            return true;
        }

        if (!data.isVisitable(target.getUniqueId()) && !player.isOp()) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7Oops! Looks like " + args[0] + " has disabled island visits!");
            return true;
        }

        confirmGUI.open(player, target);
        return true;
    }
}
