package com.islandcore.anticheat.check;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import com.islandcore.anticheat.data.PlayerData;
import com.islandcore.anticheat.util.MathUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import java.util.Deque;

class ReachCheck extends Check {

    ReachCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.REACH); }

    void handle(EntityDamageByEntityEvent event) {
        if (!isEnabled()) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (shouldSkip(player)) return;

        Entity victim = event.getEntity();
        double maxReach = plugin.getConfig().getDouble("checks.reach.max-distance", 3.3);
        double distance = MathUtil.distanceToBox(player.getEyeLocation(), victim.getBoundingBox());

        if (distance > maxReach) {
            flag(player, String.format("hit entity from %.2f blocks (max %.2f)", distance, maxReach));
        }
    }
}

class KillAuraCheck extends Check {

    KillAuraCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.KILLAURA); }

    void handle(EntityDamageByEntityEvent event) {
        if (!isEnabled()) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (shouldSkip(player)) return;

        Entity victim = event.getEntity();
        PlayerData data = plugin.getPlayerDataManager().get(player);
        long now = System.currentTimeMillis();

        Vector toEntity = victim.getLocation().toVector().subtract(player.getEyeLocation().toVector()).normalize();
        Vector look = player.getEyeLocation().getDirection().normalize();
        double dot = MathUtil.clamp(look.dot(toEntity), -1.0, 1.0);
        double angle = Math.toDegrees(Math.acos(dot));
        double maxAngle = plugin.getConfig().getDouble("checks.killaura.max-angle", 60.0);

        if (angle > maxAngle) {
            flag(player, String.format("hit target %.1f deg off crosshair (max %.1f)", angle, maxAngle));
        }

        long window = plugin.getConfig().getLong("checks.killaura.multi-target-window-ms", 100);
        int maxTargets = plugin.getConfig().getInt("checks.killaura.multi-target-count", 2);

        if (now - data.getLastAttackTime() < window
                && data.getLastAttackedEntity() != null
                && !data.getLastAttackedEntity().equals(victim.getUniqueId())) {
            data.incrementMultiAuraCount();
            if (data.getMultiAuraCount() >= maxTargets) {
                flag(player, "attacked " + (data.getMultiAuraCount() + 1) + " different entities within " + window + "ms");
                data.resetMultiAuraCount();
            }
        } else {
            data.resetMultiAuraCount();
        }

        data.setLastAttackTime(now);
        data.setLastAttackedEntity(victim.getUniqueId());
    }
}

class AutoClickerCheck extends Check {

    AutoClickerCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.AUTOCLICKER); }

    void handle(EntityDamageByEntityEvent event) {
        if (!isEnabled()) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (shouldSkip(player)) return;

        PlayerData data = plugin.getPlayerDataManager().get(player);
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = data.getAttackTimestamps();
        timestamps.addLast(now);
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > 1000) {
            timestamps.pollFirst();
        }

        int cps = timestamps.size();
        int maxCps = plugin.getConfig().getInt("checks.autoclicker.max-cps", 20);
        if (cps > maxCps) {
            flag(player, "clicked " + cps + " times/sec (max " + maxCps + ")");
        }
    }
}
