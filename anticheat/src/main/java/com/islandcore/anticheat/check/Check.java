package com.islandcore.anticheat.check;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import org.bukkit.entity.Player;

public abstract class Check {

    protected final IslandCoreAntiCheat plugin;
    protected final CheckType type;

    protected Check(IslandCoreAntiCheat plugin, CheckType type) {
        this.plugin = plugin;
        this.type = type;
    }

    public CheckType getType() { return type; }

    protected boolean isEnabled() {
        return plugin.getCheckManager().isEnabled(type)
                && plugin.getConfig().getBoolean(type.getConfigPath() + ".enabled", true);
    }

    protected boolean shouldSkip(Player player) {
        return player.hasPermission("islandcore.bypass");
    }

    protected void flag(Player player, String details) {
        if (shouldSkip(player)) return;
        plugin.getViolationManager().addViolation(player, type, details);
    }
}
