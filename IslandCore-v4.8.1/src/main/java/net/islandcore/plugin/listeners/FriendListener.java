package net.islandcore.plugin.listeners;

import net.islandcore.plugin.friends.FriendManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Set;
import java.util.UUID;

/** Lets a player know about friend requests that arrived while they were offline. */
public class FriendListener implements Listener {

    private final FriendManager friends;

    public FriendListener(FriendManager friends) {
        this.friends = friends;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Set<UUID> pending = friends.getPendingRequests(player.getUniqueId());
        if (pending.isEmpty()) return;

        StringBuilder names = new StringBuilder();
        boolean first = true;
        for (UUID uuid : pending) {
            OfflinePlayer requester = Bukkit.getOfflinePlayer(uuid);
            String name = requester.getName() == null ? "someone" : requester.getName();
            if (!first) names.append("&7, &e");
            names.append(name);
            first = false;
        }

        Msg.send(player, "&b" + Symbols.CHECK + " &7You have &e" + pending.size()
                + " &7pending friend request(s) from: &e" + names);
        Msg.send(player, "&7Use &b/friend list &7to view and accept them.");
    }
}
