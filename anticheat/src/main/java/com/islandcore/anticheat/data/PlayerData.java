package com.islandcore.anticheat.data;

import org.bukkit.Location;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;

    // Movement
    private Location lastValidLocation;
    private int airTicks = 0;
    private int jesusTicks = 0;
    private int speedBufferCount = 0;
    private long exemptUntil = 0L;

    // Combat
    private final Deque<Long> attackTimestamps = new ArrayDeque<>();
    private long lastAttackTime = 0L;
    private UUID lastAttackedEntity;
    private int multiAuraCount = 0;

    // Rotation
    private Float lastYaw;
    private final Deque<Double> recentYawDeltas = new ArrayDeque<>();

    // Blocks
    private long lastBreakTime = 0L;
    private Location lastBreakLocation;
    private int fastBreakStreak = 0;

    // Inventory / dupe
    private int lastClickRawSlot = -1;
    private long lastClickTime = 0L;
    private int lastClickItemHash = 0;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }

    public Location getLastValidLocation() { return lastValidLocation; }
    public void setLastValidLocation(Location loc) { this.lastValidLocation = loc; }

    public int getAirTicks() { return airTicks; }
    public void setAirTicks(int airTicks) { this.airTicks = airTicks; }

    public int getJesusTicks() { return jesusTicks; }
    public void setJesusTicks(int jesusTicks) { this.jesusTicks = jesusTicks; }

    public int getSpeedBufferCount() { return speedBufferCount; }
    public void incrementSpeedBuffer() { this.speedBufferCount++; }
    public void resetSpeedBuffer() { this.speedBufferCount = 0; }

    public long getExemptUntil() { return exemptUntil; }
    public void setExemptUntil(long exemptUntil) { this.exemptUntil = exemptUntil; }
    public boolean isExempt() { return System.currentTimeMillis() < exemptUntil; }

    public Deque<Long> getAttackTimestamps() { return attackTimestamps; }

    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long time) { this.lastAttackTime = time; }

    public UUID getLastAttackedEntity() { return lastAttackedEntity; }
    public void setLastAttackedEntity(UUID uuid) { this.lastAttackedEntity = uuid; }

    public int getMultiAuraCount() { return multiAuraCount; }
    public void incrementMultiAuraCount() { this.multiAuraCount++; }
    public void resetMultiAuraCount() { this.multiAuraCount = 0; }

    public Float getLastYaw() { return lastYaw; }
    public void setLastYaw(Float yaw) { this.lastYaw = yaw; }
    public Deque<Double> getRecentYawDeltas() { return recentYawDeltas; }

    public long getLastBreakTime() { return lastBreakTime; }
    public void setLastBreakTime(long time) { this.lastBreakTime = time; }
    public Location getLastBreakLocation() { return lastBreakLocation; }
    public void setLastBreakLocation(Location loc) { this.lastBreakLocation = loc; }
    public int getFastBreakStreak() { return fastBreakStreak; }
    public void setFastBreakStreak(int streak) { this.fastBreakStreak = streak; }

    public int getLastClickRawSlot() { return lastClickRawSlot; }
    public void setLastClickRawSlot(int slot) { this.lastClickRawSlot = slot; }
    public long getLastClickTime() { return lastClickTime; }
    public void setLastClickTime(long time) { this.lastClickTime = time; }
    public int getLastClickItemHash() { return lastClickItemHash; }
    public void setLastClickItemHash(int hash) { this.lastClickItemHash = hash; }
}
