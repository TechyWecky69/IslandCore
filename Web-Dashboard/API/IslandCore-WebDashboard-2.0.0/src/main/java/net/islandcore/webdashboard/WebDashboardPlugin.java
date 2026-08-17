package net.islandcore.webdashboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Level;

public final class WebDashboardPlugin extends JavaPlugin {
    private DashboardServer dashboardServer;
    private MetricsCollector metrics;
    private HistoryStore history;
    private TradeLogStore tradeLogs;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        history = new HistoryStore(getConfig().getInt("chat-history-lines", 500));

        metrics = new MetricsCollector(this);
        metrics.setExcludedWorlds(new java.util.HashSet<>(getConfig().getStringList("hidden-worlds")));
        metrics.start(getConfig().getLong("metrics-interval-ticks", 20L));

        getServer().getPluginManager().registerEvents(
                new DashboardListener(this, history, metrics),
                this
        );

        tradeLogs = new TradeLogStore(
                this,
                getConfig().getString("trade-logs-dir", "plugins/IslandCore/tradelogs"),
                getConfig().getInt("trade-logs-max-entries", 2000)
        );

        dashboardServer = new DashboardServer(this, metrics, history, tradeLogs);
        try {
            dashboardServer.start();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Could not start web dashboard", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Web dashboard API started at " +
                dashboardServer.getDisplayAddress() + " (change the password in config.yml)");
    }

    @Override
    public void onDisable() {
        if (dashboardServer != null) dashboardServer.stop();
        if (metrics != null) metrics.stop();
    }

    public HistoryStore getHistory() {
        return history;
    }

    public MetricsCollector getMetrics() {
        return metrics;
    }

    public TradeLogStore getTradeLogs() {
        return tradeLogs;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("webdashboard")) return false;
        sender.sendMessage(ChatColor.AQUA + "IslandCore Web Dashboard: " +
                ChatColor.WHITE + (dashboardServer == null ? "offline" : dashboardServer.getDisplayAddress()));
        return true;
    }
}
