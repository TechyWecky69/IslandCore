package com.islandcore.anticheat.listener;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Lightweight, application-layer connection throttling. This runs inside the
 * Minecraft server process, so it only sees traffic that has already
 * completed a TCP handshake and reached the login stage - it stops simple
 * join-flood / fake-player bot tools, not a real volumetric or protocol-layer
 * DDoS attack (SYN floods, UDP amplification, etc. happen below this). See
 * the README for network-level protection recommendations.
 */
public class ConnectionListener implements Listener {

    private final IslandCoreAntiCheat plugin;
    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public ConnectionListener(IslandCoreAntiCheat plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!plugin.getConfig().getBoolean("connection-protection.enabled", true)) return;

        String ip = event.getAddress().getHostAddress();
        long now = System.currentTimeMillis();
        long window = plugin.getConfig().getLong("connection-protection.window-ms", 10000);
        int max = plugin.getConfig().getInt("connection-protection.max-attempts", 4);

        Deque<Long> timestamps = attempts.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > window) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= max) {
                String message = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("connection-protection.kick-message",
                                "&cToo many connection attempts. Please wait a moment and try again."));
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, message);
                return;
            }
            timestamps.addLast(now);
        }
    }

    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long window = plugin.getConfig().getLong("connection-protection.window-ms", 10000);
                attempts.entrySet().removeIf(entry -> {
                    Deque<Long> deque = entry.getValue();
                    synchronized (deque) {
                        deque.removeIf(t -> now - t > window);
                        return deque.isEmpty();
                    }
                });
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60, 20L * 60);
    }
}
