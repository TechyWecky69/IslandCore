package net.islandcore.plugin.tasks;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.util.CountdownManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Equivalent to:
 *   every 1 second:
 *       loop all players:
 *           if in own island world:
 *               if looting active: show countdown, decrement
 *               else: show "not active"
 */
public class ActionBarTask extends BukkitRunnable {

    private final DataStore data;

    public ActionBarTask(DataStore data) {
        this.data = data;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!WorldUtil.isInOwnWorld(player)) continue;

            if (data.isLootingActive(player.getUniqueId())) {
                Msg.actionBar(player, "&aNext item in: " + CountdownManager.get(player.getUniqueId()));
                CountdownManager.decrement(player.getUniqueId());
            } else {
                Msg.actionBar(player, "&4Looting is not active");
            }
        }
    }
}
