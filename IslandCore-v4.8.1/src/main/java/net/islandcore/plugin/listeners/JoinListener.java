package net.islandcore.plugin.listeners;

import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.managers.IslandManager;
import net.islandcore.plugin.skilltree.SkillTreeManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class JoinListener implements Listener {

    private final JavaPlugin plugin;
    private final DataStore data;
    private final List<String> worldCreateCommands;
    private final IslandManager islandManager;
    private final SkillTreeManager skillTreeManager;

    public JoinListener(JavaPlugin plugin, DataStore data, List<String> worldCreateCommands,
                        IslandManager islandManager, SkillTreeManager skillTreeManager) {
        this.plugin = plugin;
        this.data = data;
        this.worldCreateCommands = worldCreateCommands;
        this.islandManager = islandManager;
        this.skillTreeManager = skillTreeManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String worldName = WorldUtil.islandWorldName(player.getUniqueId());
        event.setJoinMessage(null);

        // Initialise skill tree state for this player (no-op if already exists)
        skillTreeManager.initPlayer(player.getUniqueId());

        Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                "mvtp " + player.getName() + " world"
        );

        if (WorldUtil.islandExists(player.getUniqueId())) {
            islandManager.load(player.getUniqueId());

            new BukkitRunnable() {
                @Override
                public void run() {
                    World existing = WorldUtil.getIslandWorld(player.getUniqueId());
                    if (existing != null) {
                        WorldUtil.configureIslandWorld(existing);
                        if (player.isOnline()) {
                            player.teleport(existing.getSpawnLocation());
                        }
                    }
                }
            }.runTaskLater(plugin, 10L);
            return;
        }

        Msg.sendTitle(player, "&4&lCreating world...");

        for (String template : worldCreateCommands) {
            String cmd = template.replace("{world}", worldName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        data.setWorldName(player.getUniqueId(), worldName + "'s world");

        new BukkitRunnable() {
            @Override
            public void run() {
                World created = WorldUtil.getIslandWorld(player.getUniqueId());
                if (created != null) {
                    WorldUtil.configureIslandWorld(created);
                    // Disable autoload so the world only loads on demand
                    Bukkit.dispatchCommand(
                            Bukkit.getConsoleSender(),
                            "mv modify set autoload false " + created.getName()
                    );
                    if (player.isOnline()) {
                        player.teleport(created.getSpawnLocation());
                    }
                }

                Msg.send(player, "&e--------------------------------------");
                Msg.send(player, "&bWelcome to your world, &a&l" + player.getName() + "&b!");
                Msg.send(player, "&bHere are a few commands:%nl%  /toggle - toggle item looting%nl%  /skilltree - open your skill tree%nl%  /myisland - view your island stats%nl%  /toggleislandvisits - open or close your island to visitors%nl%  /home - Go to your island (or /h)");
                Msg.send(player, "&e--------------------------------------");
            }
        }.runTaskLater(plugin, 60L);
    }
}
