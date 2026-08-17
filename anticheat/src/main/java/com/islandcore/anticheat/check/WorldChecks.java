package com.islandcore.anticheat.check;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import com.islandcore.anticheat.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

class FastBreakCheck extends Check {

    FastBreakCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.FASTBREAK); }

    void handle(BlockBreakEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        PlayerData data = plugin.getPlayerDataManager().get(player);
        long now = System.currentTimeMillis();
        long minInterval = plugin.getConfig().getLong("checks.fastbreak.min-interval-ms", 90);

        long elapsed = now - data.getLastBreakTime();
        Location loc = event.getBlock().getLocation();

        if (data.getLastBreakTime() != 0 && elapsed < minInterval) {
            data.setFastBreakStreak(data.getFastBreakStreak() + 1);
            if (data.getFastBreakStreak() >= 3) {
                flag(player, "broke blocks " + elapsed + "ms apart (min " + minInterval + "ms)");
                data.setFastBreakStreak(0);
            }
        } else {
            data.setFastBreakStreak(0);
        }

        data.setLastBreakTime(now);
        data.setLastBreakLocation(loc);
    }
}
