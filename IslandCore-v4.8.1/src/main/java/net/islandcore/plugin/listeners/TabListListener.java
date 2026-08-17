package net.islandcore.plugin.listeners;

import net.islandcore.plugin.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TabListListener implements Listener {

    private final JavaPlugin plugin;

    public TabListListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        update(event.getPlayer());
    }

    public void update(Player player) {
        String title = plugin.getConfig().getString("tablist.title", "&b&lBLOCK BOUND");
        String footer = plugin.getConfig().getString("tablist.ip", "&7block-bound.org");
        player.setPlayerListHeaderFooter(Msg.color(title), Msg.color(footer));
    }
}
