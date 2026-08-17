package net.islandcore.webdashboard;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class DashboardListener implements Listener {
    private final JavaPlugin plugin;
    private final HistoryStore history;
    private final MetricsCollector metrics;

    public DashboardListener(JavaPlugin plugin, HistoryStore history, MetricsCollector metrics) {
        this.plugin = plugin;
        this.history = history;
        this.metrics = metrics;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        history.addChat(event.getPlayer().getName(), event.getMessage());
        metrics.markChat();
    }

    // Refresh the snapshot the instant a world loads/unloads, instead of leaving the
    // "Loaded worlds" box to show stale data until the next scheduled metrics tick
    // (up to metrics-interval-ticks later).
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        metrics.collectNow();
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        // WorldUnloadEvent fires just before Bukkit actually drops the world from
        // getWorlds(), so collect on the next tick once it's really gone rather than
        // recording it as still-loaded one last time.
        Bukkit.getScheduler().runTask(plugin, metrics::collectNow);
    }
}
