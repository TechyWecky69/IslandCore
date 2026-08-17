package net.islandcore.plugin.listeners;

import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kicks players after five minutes without changing their block position.
 * Looking around does not reset the timer; actual movement does.
 */
public class AfkListener implements Listener {

    private final long afkMillis;

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastMovement = new ConcurrentHashMap<>();
    private final BukkitTask checker;

    public AfkListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.afkMillis = Math.max(1L, plugin.getConfig().getLong("afk-kick-minutes", 5L))
                * 60_000L;
        this.checker = Bukkit.getScheduler().runTaskTimer(plugin, this::checkAfk, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        lastMovement.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        // Only X/Y/Z movement counts. Head rotation and camera movement do not.
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            lastMovement.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastMovement.remove(event.getPlayer().getUniqueId());
    }

    private void checkAfk() {
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            long last = lastMovement.getOrDefault(uuid, now);

            if (now - last >= afkMillis) {
                lastMovement.remove(uuid);
                player.kickPlayer("§cYou have been kicked for being AFK for 5 minutes.");
            }
        }
    }

    public void shutdown() {
        checker.cancel();
        lastMovement.clear();
    }
}
