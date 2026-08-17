package net.islandcore.plugin.listeners;

import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.islandcore.plugin.ranks.Rank;

public class RankListener implements Listener {
    private final RankManager ranks;

    public RankListener(RankManager ranks) {
        this.ranks = ranks;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        ranks.ensureRank(event.getPlayer());
    }

    /**
     * Chat format: [Rank] [Symbol] PlayerName >> message
     * e.g.  [Member] [⛏] Steve >> Hi
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Rank rank = ranks.getRankOrDefault(player.getUniqueId());
        // getFullChatPrefix returns e.g. "[Member] [⛏]"
        event.setFormat(Msg.color(ranks.getFullChatPrefix(player.getUniqueId())
                + " " + rank.getNameColor() + "%1$s" + " &f>> %2$s"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ranks.remove(event.getPlayer());
    }
}
