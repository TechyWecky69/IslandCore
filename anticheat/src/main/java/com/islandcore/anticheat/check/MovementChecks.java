package com.islandcore.anticheat.check;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import com.islandcore.anticheat.data.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Movement checks are bundled in one file since they're all small, share the
 * same event (PlayerMoveEvent) and largely the same exemption rules. None of
 * these classes are public - they're only ever driven through CheckManager,
 * which lives in this same package.
 */
final class MovementExemptions {
    private MovementExemptions() {}

    static boolean isExempt(Player player) {
        return player.isInsideVehicle()
                || player.isGliding()
                || player.isRiptiding()
                || player.isFlying()
                || player.getAllowFlight()
                || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR;
    }
}

class SpeedCheck extends Check {

    SpeedCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.SPEED); }

    void handle(PlayerMoveEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        PlayerData data = plugin.getPlayerDataManager().get(player);

        if (data.isExempt() || player.isSwimming() || MovementExemptions.isExempt(player)) {
            data.setLastValidLocation(to);
            data.resetSpeedBuffer();
            return;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        double base = player.isSprinting() ? 0.2861 : 0.221;
        PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
        if (speed != null) {
            base *= 1 + 0.2 * (speed.getAmplifier() + 1);
        }

        double buffer = plugin.getConfig().getDouble("checks.speed.buffer", 1.4);
        double maxAllowed = base * buffer;

        if (horizontalDistance > maxAllowed) {
            data.incrementSpeedBuffer();
            int sampleSize = plugin.getConfig().getInt("checks.speed.sample-size", 3);
            if (data.getSpeedBufferCount() > sampleSize) {
                flag(player, String.format("moved %.3f blocks/tick (max ~%.3f)", horizontalDistance, maxAllowed));
                if (plugin.getConfig().getBoolean("checks.speed.setback", true) && data.getLastValidLocation() != null) {
                    player.teleport(data.getLastValidLocation());
                }
                data.resetSpeedBuffer();
            }
        } else {
            data.resetSpeedBuffer();
            data.setLastValidLocation(from);
        }
    }
}

class FlyCheck extends Check {

    FlyCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.FLY); }

    void handle(PlayerMoveEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        PlayerData data = plugin.getPlayerDataManager().get(player);
        Location to = event.getTo();
        if (to == null) return;

        if (data.isExempt() || MovementExemptions.isExempt(player)
                || player.isSwimming() || player.getPotionEffect(PotionEffectType.LEVITATION) != null
                || isNearClimbable(player) || isInLiquid(to)) {
            data.setAirTicks(0);
            data.setLastValidLocation(to);
            return;
        }

        if (player.isOnGround()) {
            data.setAirTicks(0);
            data.setLastValidLocation(to);
            return;
        }

        data.setAirTicks(data.getAirTicks() + 1);
        double deltaY = to.getY() - event.getFrom().getY();
        int maxAirTicks = plugin.getConfig().getInt("checks.fly.max-air-ticks", 10);

        if (data.getAirTicks() > maxAirTicks && deltaY >= -0.02) {
            flag(player, String.format("airborne %d ticks without falling (dy=%.3f)", data.getAirTicks(), deltaY));
            if (plugin.getConfig().getBoolean("checks.fly.setback", true) && data.getLastValidLocation() != null) {
                player.teleport(data.getLastValidLocation());
            }
            data.setAirTicks(0);
        }
    }

    private boolean isNearClimbable(Player player) {
        Material type = player.getLocation().getBlock().getType();
        return type == Material.LADDER || type == Material.VINE || type == Material.SCAFFOLDING;
    }

    private boolean isInLiquid(Location loc) {
        Material type = loc.getBlock().getType();
        return type == Material.WATER || type == Material.LAVA;
    }
}

class NoFallCheck extends Check {

    NoFallCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.NOFALL); }

    void handle(PlayerMoveEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        PlayerData data = plugin.getPlayerDataManager().get(player);
        if (data.isExempt() || MovementExemptions.isExempt(player)) return;

        if (!player.isOnGround()) return;
        if (player.getFallDistance() < 3.0f) return;

        // Player claims to be on solid ground with a large pending fall
        // distance - verify a block actually exists close enough below
        // them. If not, this looks like a spoofed "on ground" packet, a
        // common way NoFall clients cancel fall damage server-side.
        Location loc = player.getLocation();
        Block below = loc.clone().subtract(0, 0.3, 0).getBlock();
        if (below.getType().isAir()) {
            flag(player, String.format("landed with no supporting block beneath (fall distance %.2f)", player.getFallDistance()));
        }
    }
}

class JesusCheck extends Check {

    JesusCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.JESUS); }

    void handle(PlayerMoveEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        PlayerData data = plugin.getPlayerDataManager().get(player);
        Location to = event.getTo();
        if (to == null) return;

        if (data.isExempt() || MovementExemptions.isExempt(player)
                || player.isSwimming() || player.isInsideVehicle()) {
            data.setJesusTicks(0);
            return;
        }

        Block feet = to.getBlock();
        Block below = feet.getRelative(0, -1, 0);

        double dx = to.getX() - event.getFrom().getX();
        double dz = to.getZ() - event.getFrom().getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        boolean onWaterSurface = below.getType() == Material.WATER
                && feet.getType() != Material.WATER
                && player.getFallDistance() == 0f
                && horizontal > 0.02;

        if (onWaterSurface) {
            data.setJesusTicks(data.getJesusTicks() + 1);
            int maxTicks = plugin.getConfig().getInt("checks.jesus.max-ticks", 5);
            if (data.getJesusTicks() > maxTicks) {
                flag(player, "walking on water surface for " + data.getJesusTicks() + " ticks");
                data.setJesusTicks(0);
            }
        } else {
            data.setJesusTicks(0);
        }
    }
}

class RotationCheck extends Check {

    RotationCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.ROTATION); }

    void handle(PlayerMoveEvent event) {
        if (!isEnabled()) return;
        Player player = event.getPlayer();
        if (shouldSkip(player)) return;

        Location to = event.getTo();
        if (to == null) return;

        float pitch = to.getPitch();
        if (pitch > 90.5f || pitch < -90.5f) {
            flag(player, "invalid pitch " + pitch);
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().get(player);
        Float lastYaw = data.getLastYaw();
        float yaw = to.getYaw();
        if (lastYaw != null) {
            double delta = Math.abs(normalizeAngle(yaw - lastYaw));
            var deque = data.getRecentYawDeltas();
            deque.addLast(delta);
            if (deque.size() > 20) deque.pollFirst();

            if (deque.size() >= 12 && delta > 0.5) {
                long matches = deque.stream().filter(d -> Math.abs(d - delta) < 0.01).count();
                if (matches >= 10) {
                    flag(player, "repeating identical yaw increments (" + String.format("%.2f", delta) + "deg x" + matches + ") - possible aim assist");
                    deque.clear();
                }
            }
        }
        data.setLastYaw(yaw);
    }

    private double normalizeAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }
}
