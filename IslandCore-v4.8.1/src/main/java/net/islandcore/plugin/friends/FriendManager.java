package net.islandcore.plugin.friends;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Persistent friends lists + pending friend requests.
 *
 * Friendships are mutual: linking two players updates both of their lists.
 * Requests are stored keyed by the *target* (the player who needs to accept
 * or deny) and are persisted, so a request sent to someone who is offline is
 * still waiting for them next time they log in.
 */
public class FriendManager {

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    /** playerUuid -> that player's friends. */
    private final Map<UUID, Set<UUID>> friends = new HashMap<>();

    /** targetUuid -> UUIDs of players waiting on that target's decision. */
    private final Map<UUID, Set<UUID>> pendingRequests = new HashMap<>();

    public FriendManager(JavaPlugin plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        this.file = new File(dataDir, "friends.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data/friends.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        readInto(config.getConfigurationSection("friends"), friends);
        readInto(config.getConfigurationSection("pending"), pendingRequests);
    }

    private void readInto(ConfigurationSection section, Map<UUID, Set<UUID>> target) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            UUID owner = parse(key);
            if (owner == null) continue;

            Set<UUID> values = new HashSet<>();
            for (String raw : section.getStringList(key)) {
                UUID uuid = parse(raw);
                if (uuid != null) values.add(uuid);
            }
            target.put(owner, values);
        }
    }

    public synchronized void save() {
        config.set("friends", null);
        config.set("pending", null);
        writeFrom(friends, "friends");
        writeFrom(pendingRequests, "pending");

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data/friends.yml", e);
        }
    }

    private void writeFrom(Map<UUID, Set<UUID>> source, String path) {
        for (Map.Entry<UUID, Set<UUID>> entry : source.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            List<String> values = new ArrayList<>();
            for (UUID uuid : entry.getValue()) values.add(uuid.toString());
            config.set(path + "." + entry.getKey(), values);
        }
    }

    private UUID parse(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ----- Friendships -----

    public boolean areFriends(UUID a, UUID b) {
        return friends.getOrDefault(a, Set.of()).contains(b);
    }

    public Set<UUID> getFriends(UUID player) {
        return Collections.unmodifiableSet(friends.getOrDefault(player, Set.of()));
    }

    public void removeFriend(UUID a, UUID b) {
        friends.computeIfAbsent(a, k -> new HashSet<>()).remove(b);
        friends.computeIfAbsent(b, k -> new HashSet<>()).remove(a);
        save();
    }

    private void link(UUID a, UUID b) {
        friends.computeIfAbsent(a, k -> new HashSet<>()).add(b);
        friends.computeIfAbsent(b, k -> new HashSet<>()).add(a);
    }

    // ----- Requests -----

    public boolean hasPendingRequest(UUID target, UUID requester) {
        return pendingRequests.getOrDefault(target, Set.of()).contains(requester);
    }

    public Set<UUID> getPendingRequests(UUID target) {
        return Collections.unmodifiableSet(pendingRequests.getOrDefault(target, Set.of()));
    }

    public void addRequest(UUID target, UUID requester) {
        pendingRequests.computeIfAbsent(target, k -> new HashSet<>()).add(requester);
        save();
    }

    /** Accepts requester's pending request to target: links them as friends and clears the request. */
    public void acceptRequest(UUID target, UUID requester) {
        Set<UUID> set = pendingRequests.get(target);
        if (set != null) set.remove(requester);
        link(target, requester);
        save();
    }

    public void denyRequest(UUID target, UUID requester) {
        Set<UUID> set = pendingRequests.get(target);
        if (set != null) set.remove(requester);
        save();
    }
}
