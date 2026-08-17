package net.islandcore.plugin.commands;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.managers.IslandManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffTPCommand implements CommandExecutor {

    private final DataStore data;
    private final IslandManager islandManager;

    public StaffTPCommand(DataStore data, IslandManager islandManager) {
        this.data = data;
        this.islandManager = islandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.stafftp")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (args.length < 1) {
            Msg.send(player, "&cUsage: /stafftp <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!WorldUtil.islandExists(target.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7That player does not have an island.");
            return true;
        }

        if (!islandManager.load(target.getUniqueId())) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7That island could not be loaded right now.");
            return true;
        }

        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("IslandCore"),
                () -> {
                    if (!player.isOnline()) return;
                    World world = WorldUtil.getIslandWorld(target.getUniqueId());
                    if (world == null) {
                        Msg.send(player, "&c" + Symbols.WARNING + " &7That island could not be loaded right now.");
                        return;
                    }
                    player.teleport(world.getSpawnLocation());
                    player.setAllowFlight(true);
                    Msg.send(player, "&aStaff teleporting to &b" + target.getName() + "&a's island. &7(Visits setting bypassed)");
                },
                10L
        );
        return true;
    }
}
