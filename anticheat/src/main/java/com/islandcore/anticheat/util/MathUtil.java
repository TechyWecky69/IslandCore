package com.islandcore.anticheat.util;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public final class MathUtil {

    private MathUtil() {}

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Shortest distance from a point to an axis-aligned bounding box.
     * Used for the reach check instead of centre-to-centre distance,
     * since entity hitboxes are not points.
     */
    public static double distanceToBox(Location point, BoundingBox box) {
        double x = clamp(point.getX(), box.getMinX(), box.getMaxX());
        double y = clamp(point.getY(), box.getMinY(), box.getMaxY());
        double z = clamp(point.getZ(), box.getMinZ(), box.getMaxZ());
        double dx = point.getX() - x;
        double dy = point.getY() - y;
        double dz = point.getZ() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
