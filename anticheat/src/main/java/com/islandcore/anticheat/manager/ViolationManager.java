package com.islandcore.anticheat.manager;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import com.islandcore.anticheat.check.CheckType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViolationManager {

    private final IslandCoreAntiCheat plugin;
    private final Map<UUID, Map<CheckType, Double>> violations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<CheckType, Long>> lastViolationTime = new ConcurrentHashMap<>();
    private final Map<UUID, Map<CheckType, Double>> lastPunishedThreshold = new ConcurrentHashMap<>();
    private BukkitTask decayTask;

    public ViolationManager(IslandCoreAntiCheat plugin) {
        this.plugin = plugin;
    }

    public void addViolation(Player player, CheckType type, String details) {
        UUID uuid = player.getUniqueId();
        Map<CheckType, Double> playerViolations = violations.computeIfAbsent(uuid, k -> new EnumMap<>(CheckType.class));
        double vl = playerViolations.merge(type, 1.0, Double::sum);

        lastViolationTime.computeIfAbsent(uuid, k -> new EnumMap<>(CheckType.class)).put(type, System.currentTimeMillis());

        alertStaff(player, type, vl, details);
        checkPunishments(player, type, vl);

        plugin.getLogger().info(String.format("%s failed %s (VL %.1f) - %s", player.getName(), type.getDisplayName(), vl, details));
    }

    private void alertStaff(Player player, CheckType type, double vl, String details) {
        String format = plugin.getConfig().getString("alerts.format",
                "&8[&bIslandCore&8] &e%player% &7failed &b%check% &7(VL &c%vl%&7) &8- &7%details%");
        String message = ChatColor.translateAlternateColorCodes('&', format
                .replace("%player%", player.getName())
                .replace("%check%", type.getDisplayName())
                .replace("%vl%", String.format("%.1f", vl))
                .replace("%details%", details));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("islandcore.alerts") && !plugin.isAlertsDisabled(staff)) {
                staff.sendMessage(message);
            }
        }
    }

    private void checkPunishments(Player player, CheckType type, double vl) {
        List<?> rawList = plugin.getConfig().getList("punishments");
        if (rawList == null) return;

        Map<CheckType, Double> thresholds = lastPunishedThreshold.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(CheckType.class));
        double alreadyPunished = thresholds.getOrDefault(type, 0.0);

        for (Object raw : rawList) {
            if (!(raw instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) raw;
            Object vlObj = entry.get("vl");
            Object cmdObj = entry.get("command");
            if (vlObj == null || cmdObj == null) continue;

            double threshold = ((Number) vlObj).doubleValue();
            if (vl >= threshold && threshold > alreadyPunished) {
                String command = cmdObj.toString();
                if (!command.equalsIgnoreCase("alert")) {
                    String finalCommand = ChatColor.translateAlternateColorCodes('&', command)
                            .replace("%player%", player.getName())
                            .replace("%check%", type.getDisplayName());
                    ConsoleCommandSender console = Bukkit.getConsoleSender();
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(console, ChatColor.stripColor(finalCommand)));
                }
                thresholds.put(type, threshold);
            }
        }
    }

    public double getViolationLevel(Player player, CheckType type) {
        Map<CheckType, Double> playerViolations = violations.get(player.getUniqueId());
        if (playerViolations == null) return 0.0;
        return playerViolations.getOrDefault(type, 0.0);
    }

    public Map<CheckType, Double> getAllViolations(Player player) {
        return violations.getOrDefault(player.getUniqueId(), new EnumMap<>(CheckType.class));
    }

    public void resetViolations(Player player) {
        violations.remove(player.getUniqueId());
        lastViolationTime.remove(player.getUniqueId());
        lastPunishedThreshold.remove(player.getUniqueId());
    }

    public void startDecayTask() {
        double decayAmount = plugin.getConfig().getDouble("violations.decay-amount", 1.0);
        long intervalSeconds = plugin.getConfig().getLong("violations.decay-interval-seconds", 3);
        long delaySeconds = plugin.getConfig().getLong("violations.decay-delay-seconds", 8);

        decayTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, Map<CheckType, Double>> playerEntry : violations.entrySet()) {
                    Map<CheckType, Long> times = lastViolationTime.get(playerEntry.getKey());
                    if (times == null) continue;
                    for (Map.Entry<CheckType, Double> checkEntry : playerEntry.getValue().entrySet()) {
                        Long last = times.get(checkEntry.getKey());
                        if (last == null) continue;
                        if (now - last > delaySeconds * 1000L) {
                            double newValue = Math.max(0.0, checkEntry.getValue() - decayAmount);
                            checkEntry.setValue(newValue);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    public void stopDecayTask() {
        if (decayTask != null) decayTask.cancel();
    }
}
