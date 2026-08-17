package net.islandcore.webdashboard;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Reads trade logs written by IslandCore to {@code trade-logs-dir} (one JSON
 * object per {@code .log} file, see the sample format in the plugin's
 * tradelogs folder). Parsed files are cached by name + last-modified time so
 * a directory of thousands of trades doesn't get re-parsed on every poll.
 */
public final class TradeLogStore {
    public record TradeItem(String material, int amount) {}

    public record PlayerTrade(String uuid, String name, List<TradeItem> itemsGiven) {}

    public record TradeLog(long time, String fileName, PlayerTrade playerA, PlayerTrade playerB) {}

    private record CacheEntry(long lastModified, TradeLog log) {}

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final JavaPlugin plugin;
    private final File dir;
    private final int maxEntries;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public TradeLogStore(JavaPlugin plugin, String dirPath, int maxEntries) {
        this.plugin = plugin;
        this.dir = new File(dirPath);
        this.maxEntries = Math.max(1, maxEntries);
    }

    /** Loads every trade log, newest first, capped at {@code maxEntries}. */
    public List<TradeLog> loadAll() {
        File[] files = dir.listFiles((f, name) -> name.endsWith(".log"));
        List<TradeLog> logs = new ArrayList<>();
        if (files == null) return logs;

        for (File file : files) {
            long lastModified = file.lastModified();
            CacheEntry cached = cache.get(file.getName());
            if (cached != null && cached.lastModified() == lastModified) {
                logs.add(cached.log());
                continue;
            }
            TradeLog parsed = parseFile(file);
            if (parsed != null) {
                cache.put(file.getName(), new CacheEntry(lastModified, parsed));
                logs.add(parsed);
            }
        }

        // Drop cache entries for files that no longer exist.
        cache.keySet().retainAll(java.util.Arrays.stream(files).map(File::getName).collect(java.util.stream.Collectors.toSet()));

        logs.sort(Comparator.comparingLong(TradeLog::time).reversed());
        if (logs.size() > maxEntries) {
            logs = new ArrayList<>(logs.subList(0, maxEntries));
        }
        return logs;
    }

    @SuppressWarnings("unchecked")
    private TradeLog parseFile(File file) {
        try {
            String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            Object root = JsonReader.parse(text);
            if (!(root instanceof Map<?, ?> map)) return null;

            long time = parseTime((String) map.get("timestamp"), file.lastModified());
            PlayerTrade playerA = parsePlayer((Map<String, Object>) map.get("playerA"));
            PlayerTrade playerB = parsePlayer((Map<String, Object>) map.get("playerB"));
            return new TradeLog(time, file.getName(), playerA, playerB);
        } catch (IOException | RuntimeException e) {
            plugin.getLogger().log(Level.WARNING, "Dashboard: skipping unreadable trade log " + file.getName(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private PlayerTrade parsePlayer(Map<String, Object> playerMap) {
        if (playerMap == null) return new PlayerTrade(null, null, List.of());
        String uuid = (String) playerMap.get("uuid");
        String name = (String) playerMap.get("name");
        List<TradeItem> items = new ArrayList<>();
        Object rawItems = playerMap.get("itemsGiven");
        if (rawItems instanceof List<?> list) {
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> itemMap)) continue;
                Object material = itemMap.get("material");
                Object amount = itemMap.get("amount");
                items.add(new TradeItem(
                        material == null ? "UNKNOWN" : material.toString(),
                        amount instanceof Number n ? n.intValue() : 0
                ));
            }
        }
        return new PlayerTrade(uuid, name, items);
    }

    /** Trade log timestamps are local, offset-less date-times, e.g. 2026-08-16T21:31:08.000778800. */
    private long parseTime(String raw, long fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return LocalDateTime.parse(raw, TIMESTAMP_FORMAT)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
