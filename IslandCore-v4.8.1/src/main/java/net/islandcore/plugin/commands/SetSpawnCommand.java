package net.islandcore.plugin.commands;

import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.setspawn")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        World world = WorldUtil.getIslandWorld(player.getUniqueId());
        if (world == null || !player.getWorld().equals(world)) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You can only set your spawn on your own island.");
            return true;
        }

        Location current = player.getLocation();
        Location spawn = new Location(world,
                current.getBlockX() + 0.5,
                current.getBlockY(),
                current.getBlockZ() + 0.5,
                current.getYaw(),
                current.getPitch());
        world.setSpawnLocation(spawn);
        WorldUtil.configureIslandWorld(world);

        Msg.send(player, "&a" + Symbols.CHECK + " &7Island spawn set to &e" + current.getBlockX() + ", "
                + current.getBlockY() + ", " + current.getBlockZ() + "&7.");
        Msg.send(player, "&7Blocks at &eY+1 &7and &eY+2 &7above the spawn are now protected from placement.");
        return true;
    }
}
