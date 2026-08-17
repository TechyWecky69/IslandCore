package com.islandcore.anticheat.data;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> data = new ConcurrentHashMap<>();

    public PlayerData get(Player player) {
        return data.computeIfAbsent(player.getUniqueId(), PlayerData::new);
    }

    public void remove(Player player) {
        data.remove(player.getUniqueId());
    }
}
