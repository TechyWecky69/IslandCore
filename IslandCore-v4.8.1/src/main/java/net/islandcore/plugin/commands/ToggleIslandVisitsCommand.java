package net.islandcore.plugin.commands;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.util.IslandVisitorUtil;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.PermissionUtil;
import net.islandcore.plugin.util.Symbols;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /toggleislandvisits
 *
 * Allows a player to toggle whether other players can visit their island.
 * When visits are disabled, any current visitors are immediately kicked back
 * to their own islands.
 */
public class ToggleIslandVisitsCommand implements CommandExecutor {

    private final DataStore data;
    private final RankManager ranks;

    public ToggleIslandVisitsCommand(DataStore data, RankManager ranks) {
        this.data = data;
        this.ranks = ranks;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (PermissionUtil.deny(sender, "islandcore.toggleislandvisits")) return true;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!WorldUtil.isInOwnWorld(player)) {
            Msg.send(player, "&c" + Symbols.WARNING + " &7You can only toggle island visits from your own island!");
            return true;
        }

        boolean nowVisitable = !data.isVisitable(player.getUniqueId());
        data.setVisitable(player.getUniqueId(), nowVisitable);

        if (nowVisitable) {
            Msg.send(player, "&a" + Symbols.CHECK + " &7Island visits &aenabled&7. Players can now visit your island.");
        } else {
            int kicked = IslandVisitorUtil.kickVisitors(player.getUniqueId(), ranks);
            if (kicked > 0) {
                Msg.send(player, "&c" + Symbols.WARNING + " &7Island visits &cdisabled&7. Kicked &b" + kicked + " &7visitor(s).");
            } else {
                Msg.send(player, "&c" + Symbols.WARNING + " &7Island visits &cdisabled&7. Players can no longer visit your island.");
            }
        }

        return true;
    }
}
