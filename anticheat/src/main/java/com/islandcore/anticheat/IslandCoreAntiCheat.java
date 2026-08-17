package com.islandcore.anticheat;

import com.islandcore.anticheat.check.CheckManager;
import com.islandcore.anticheat.command.IslandCoreCommand;
import com.islandcore.anticheat.data.PlayerDataManager;
import com.islandcore.anticheat.listener.BlockListener;
import com.islandcore.anticheat.listener.CombatListener;
import com.islandcore.anticheat.listener.ConnectionListener;
import com.islandcore.anticheat.listener.InventoryListener;
import com.islandcore.anticheat.listener.MovementListener;
import com.islandcore.anticheat.listener.PlayerJoinQuitListener;
import com.islandcore.anticheat.manager.ViolationManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IslandCoreAntiCheat extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private CheckManager checkManager;
    private ViolationManager violationManager;
    private final Set<UUID> alertsDisabled = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.playerDataManager = new PlayerDataManager();
        this.checkManager = new CheckManager(this);
        this.violationManager = new ViolationManager(this);

        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);

        PluginCommand pluginCommand = getCommand("islandcore");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(new IslandCoreCommand(this));
        }

        violationManager.startDecayTask();

        getLogger().info("IslandCore AntiCheat enabled.");
    }

    @Override
    public void onDisable() {
        if (violationManager != null) {
            violationManager.stopDecayTask();
        }
        getLogger().info("IslandCore AntiCheat disabled.");
    }

    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public CheckManager getCheckManager() { return checkManager; }
    public ViolationManager getViolationManager() { return violationManager; }

    public boolean isAlertsDisabled(Player player) {
        return alertsDisabled.contains(player.getUniqueId());
    }

    /** Returns true if alerts are now disabled for this player. */
    public boolean toggleAlerts(Player player) {
        UUID uuid = player.getUniqueId();
        if (alertsDisabled.contains(uuid)) {
            alertsDisabled.remove(uuid);
            return false;
        } else {
            alertsDisabled.add(uuid);
            return true;
        }
    }
}
