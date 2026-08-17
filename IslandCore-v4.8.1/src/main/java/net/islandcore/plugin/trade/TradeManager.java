package net.islandcore.plugin.trade;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks pending /trade requests and active trade sessions, and writes completed trades to disk. */
public class TradeManager {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final JavaPlugin plugin;
    private final File logFolder;

    /** target uuid -> requester uuid, mirrors FriendManager's pending-request shape. */
    private final Map<UUID, UUID> pendingRequests = new ConcurrentHashMap<>();

    /** player uuid -> the session they are currently in (both players in a session map to the same instance). */
    private final Map<UUID, TradeSession> activeSessions = new ConcurrentHashMap<>();

    public TradeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logFolder = new File(plugin.getDataFolder(), "tradelogs");
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }
    }

    // ----- pending chat requests -----

    public boolean hasPendingRequest(UUID target, UUID requester) {
        return requester.equals(pendingRequests.get(target));
    }

    public boolean hasAnyPendingRequest(UUID target) {
        return pendingRequests.containsKey(target);
    }

    public void addRequest(UUID target, UUID requester) {
        pendingRequests.put(target, requester);
    }

    public void clearRequest(UUID target) {
        pendingRequests.remove(target);
    }

    // ----- active sessions -----

    public boolean isTrading(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public TradeSession getSession(UUID uuid) {
        return activeSessions.get(uuid);
    }

    public TradeSession startSession(UUID a, UUID b) {
        TradeSession session = new TradeSession(a, b);
        activeSessions.put(a, session);
        activeSessions.put(b, session);
        return session;
    }

    public void endSession(TradeSession session) {
        session.setClosed(true);
        activeSessions.remove(session.getPlayerA());
        activeSessions.remove(session.getPlayerB());
    }

    /** Every currently active session, de-duplicated (both players map to the same instance). */
    public java.util.Set<TradeSession> allSessions() {
        return new java.util.LinkedHashSet<>(activeSessions.values());
    }

    // ----- trade log -----

    /**
     * Writes the completed trade to disk as JSON so the dashboard can read it later.
     * File name pattern: player1-player2-yyyy-MM-dd_HH-mm-ss.log
     */
    public void logTrade(Player playerA, Player playerB, List<ItemStack> givenByA, List<ItemStack> givenByB) {
        String stamp = LocalDateTime.now().format(FILE_STAMP);
        String fileName = sanitize(playerA.getName()) + "-" + sanitize(playerB.getName()) + "-" + stamp + ".log";
        File file = new File(logFolder, fileName);

        String json = "{"
                + "\"timestamp\": \"" + escape(LocalDateTime.now().toString()) + "\","
                + "\"playerA\": " + playerJson(playerA, givenByA) + ","
                + "\"playerB\": " + playerJson(playerB, givenByB)
                + "}";

        try {
            Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("[IslandCore] Failed to write trade log " + fileName + ": " + e.getMessage());
        }
    }

    private String playerJson(OfflinePlayer player, List<ItemStack> given) {
        StringBuilder items = new StringBuilder("[");
        for (int i = 0; i < given.size(); i++) {
            if (i > 0) items.append(",");
            items.append(itemJson(given.get(i)));
        }
        items.append("]");

        return "{"
                + "\"uuid\": \"" + player.getUniqueId() + "\","
                + "\"name\": \"" + escape(player.getName()) + "\","
                + "\"itemsGiven\": " + items
                + "}";
    }

    private String itemJson(ItemStack stack) {
        String displayName = null;
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            displayName = stack.getItemMeta().getDisplayName();
        }

        return "{"
                + "\"material\": \"" + stack.getType().name() + "\","
                + "\"amount\": " + stack.getAmount()
                + (displayName != null ? ",\"displayName\": \"" + escape(displayName) + "\"" : "")
                + "}";
    }

    private static String sanitize(String name) {
        return name == null ? "unknown" : name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static String escape(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
