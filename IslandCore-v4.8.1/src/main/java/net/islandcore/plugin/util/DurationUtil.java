package net.islandcore.plugin.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DurationUtil {

    private DurationUtil() {}

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    // Preserves the exact tokens and display labels from the original /ban command.
    private static final Map<String, long[]> DURATIONS = new LinkedHashMap<>();
    // long[0] = millis, unused second slot kept for clarity/extension
    static {
        DURATIONS.put("1day", new long[]{DAY});
        DURATIONS.put("1week", new long[]{7 * DAY});
        DURATIONS.put("2weeks", new long[]{14 * DAY});
        DURATIONS.put("1month", new long[]{30 * DAY});
        DURATIONS.put("3months", new long[]{90 * DAY});
        DURATIONS.put("6months", new long[]{180 * DAY});
        DURATIONS.put("9months", new long[]{270 * DAY});
        DURATIONS.put("1year", new long[]{365 * DAY});
    }

    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put("1day", "1 day");
        LABELS.put("1week", "1 week");
        LABELS.put("2weeks", "2 weeks");
        LABELS.put("1month", "1 month");
        LABELS.put("3months", "3 months");
        LABELS.put("6months", "6 months");
        LABELS.put("9months", "9 months");
        LABELS.put("1year", "1 year");
    }

    public static boolean isValidToken(String token) {
        return DURATIONS.containsKey(token);
    }

    public static long millis(String token) {
        long[] v = DURATIONS.get(token);
        return v == null ? -1 : v[0];
    }

    public static String label(String token) {
        return LABELS.getOrDefault(token, token);
    }
}
