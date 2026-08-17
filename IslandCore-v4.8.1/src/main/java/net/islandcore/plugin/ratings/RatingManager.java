package net.islandcore.plugin.ratings;

import net.islandcore.plugin.skilltree.SkillTree;
import net.islandcore.plugin.skilltree.SkillTreeManager;
import net.islandcore.plugin.util.Colors;
import net.islandcore.plugin.util.Symbols;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Two independent island ratings:
 *
 * <p><b>Community rating</b> - a 1-5 star average built from one stored vote
 * per visitor per island. Re-rating overwrites a visitor's previous vote
 * rather than stacking, so the average always reflects each rater's current
 * opinion. Voting requires having actually spent time on the island during
 * the current visit, which stops drive-by griefing/boosting.
 *
 * <p><b>Island Score</b> - an automatic 0-100 number nobody can vote-brigade,
 * built from skill tree progress, island age and unique visitor count. New
 * islands with zero community votes still show a fair number.
 */
public class RatingManager {

    public enum RateResult {
        SUCCESS,
        SELF,
        TOO_SOON,
        ON_COOLDOWN,
        INVALID_SCORE
    }

    private static final int MIN_SCORE = 1;
    private static final int MAX_SCORE = 5;

    private final JavaPlugin plugin;
    private final SkillTree skillTree;
    private final SkillTreeManager skillTreeManager;

    private final File file;
    private FileConfiguration config;

    private final int minVisitSeconds;
    private final int revoteCooldownHours;
    private final int autoScoreMaxDay;
    private final int autoScoreMaxUniqueVisitors;

    /** visitor UUID -> the island owner they are currently on, with the time they arrived. Not persisted. */
    private final Map<UUID, VisitSession> activeVisits = new ConcurrentHashMap<>();

    private record VisitSession(UUID owner, long startMillis) {}

    public RatingManager(JavaPlugin plugin, SkillTree skillTree, SkillTreeManager skillTreeManager) {
        this.plugin = plugin;
        this.skillTree = skillTree;
        this.skillTreeManager = skillTreeManager;

        FileConfiguration pluginConfig = plugin.getConfig();
        this.minVisitSeconds = Math.max(0, pluginConfig.getInt("ratings.min-visit-seconds", 30));
        this.revoteCooldownHours = Math.max(0, pluginConfig.getInt("ratings.revote-cooldown-hours", 24));
        this.autoScoreMaxDay = Math.max(1, pluginConfig.getInt("ratings.auto-score.max-day", 60));
        this.autoScoreMaxUniqueVisitors = Math.max(1, pluginConfig.getInt("ratings.auto-score.max-unique-visitors", 20));

        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        this.file = new File(dataDir, "ratings.yml");
        load();
    }

    // ─────────────────────────────────────────────────────
    //  Persistence
    // ─────────────────────────────────────────────────────

    private void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data/ratings.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data/ratings.yml", e);
        }
    }

    // ─────────────────────────────────────────────────────
    //  Visit session tracking (drives both the min-visit-time
    //  requirement and the unique-visitor popularity signal)
    // ─────────────────────────────────────────────────────

    /** Call the moment a visitor actually lands on someone else's island. */
    public void recordVisitStart(UUID visitor, UUID owner) {
        if (visitor.equals(owner)) return;
        activeVisits.put(visitor, new VisitSession(owner, System.currentTimeMillis()));

        String base = "players." + owner + ".visits.";
        int total = config.getInt(base + "total", 0);
        config.set(base + "total", total + 1);

        List<String> uniques = config.getStringList(base + "uniques");
        if (!uniques.contains(visitor.toString())) {
            uniques.add(visitor.toString());
            config.set(base + "uniques", uniques);
        }
        save();
    }

    /** Call when a visitor leaves an island (world change, disconnect, teleport home). */
    public void endVisitSession(UUID visitor) {
        activeVisits.remove(visitor);
    }

    /** The island owner the visitor is currently on a tracked visit to, or null. */
    public UUID getActiveVisitOwner(UUID visitor) {
        VisitSession session = activeVisits.get(visitor);
        return session == null ? null : session.owner();
    }

    public long dwellSeconds(UUID visitor) {
        VisitSession session = activeVisits.get(visitor);
        if (session == null) return 0;
        return (System.currentTimeMillis() - session.startMillis()) / 1000L;
    }

    // ─────────────────────────────────────────────────────
    //  Community star rating
    // ─────────────────────────────────────────────────────

    public RateResult canRate(UUID rater, UUID owner) {
        if (rater.equals(owner)) return RateResult.SELF;

        VisitSession session = activeVisits.get(rater);
        boolean visitedLongEnough = session != null
                && session.owner().equals(owner)
                && dwellSeconds(rater) >= minVisitSeconds;

        // Also allow rating from a past visit already on record (e.g. via /rate
        // typed later), as long as they've visited at all and are not on cooldown.
        boolean hasVisitedBefore = getVoteTime(rater, owner) > 0
                || config.getStringList("players." + owner + ".visits.uniques").contains(rater.toString());

        if (!visitedLongEnough && !hasVisitedBefore) return RateResult.TOO_SOON;

        long lastVoteMillis = getVoteTime(rater, owner);
        if (lastVoteMillis > 0 && revoteCooldownHours > 0) {
            long cooldownMillis = revoteCooldownHours * 3_600_000L;
            if (System.currentTimeMillis() - lastVoteMillis < cooldownMillis) {
                return RateResult.ON_COOLDOWN;
            }
        }

        return RateResult.SUCCESS;
    }

    public RateResult rate(UUID rater, UUID owner, int score) {
        if (score < MIN_SCORE || score > MAX_SCORE) return RateResult.INVALID_SCORE;

        RateResult eligibility = canRate(rater, owner);
        if (eligibility != RateResult.SUCCESS) return eligibility;

        String base = "players." + owner + ".votes." + rater + ".";
        config.set(base + "score", score);
        config.set(base + "time", System.currentTimeMillis());
        save();

        return RateResult.SUCCESS;
    }

    private long getVoteTime(UUID rater, UUID owner) {
        return config.getLong("players." + owner + ".votes." + rater + ".time", 0);
    }

    public double getAverage(UUID owner) {
        var section = config.getConfigurationSection("players." + owner + ".votes");
        if (section == null) return 0.0;

        int sum = 0;
        int count = 0;
        for (String raterKey : section.getKeys(false)) {
            sum += config.getInt("players." + owner + ".votes." + raterKey + ".score", 0);
            count++;
        }
        return count == 0 ? 0.0 : (double) sum / count;
    }

    public int getVoteCount(UUID owner) {
        var section = config.getConfigurationSection("players." + owner + ".votes");
        return section == null ? 0 : section.getKeys(false).size();
    }

    /** e.g. "★★★★☆" — average rounded to the nearest whole star. */
    public String formatStars(double average) {
        int filled = (int) Math.round(average);
        filled = Math.max(0, Math.min(MAX_SCORE, filled));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filled; i++) sb.append(Symbols.STAR_FULL);
        for (int i = filled; i < MAX_SCORE; i++) sb.append(Symbols.STAR_EMPTY);
        return sb.toString();
    }

    /**
     * Wipes every community vote an island has received, resetting its
     * average back to 0 and its vote count back to 0. Does not touch visit
     * history or the automatic Island Score, since those aren't "ratings"
     * that can be brigaded — only the 1-5 star votes are cleared.
     *
     * @return how many votes were removed
     */
    public int clearRatings(UUID owner) {
        var section = config.getConfigurationSection("players." + owner + ".votes");
        int removed = section == null ? 0 : section.getKeys(false).size();
        config.set("players." + owner + ".votes", null);
        save();
        return removed;
    }

    /**
     * Wipes the visit counts and cached island day that feed the automatic
     * Island Score, putting that portion back to 0. Skill tree progress is
     * tracked separately by SkillTreeManager and isn't touched here — call
     * skillTreeManager.resetPlayer(uuid) too if you want the score fully at
     * 0 rather than just its visit/day components (skill tree progress is
     * worth 40% of the total).
     */
    public void resetAutoScoreData(UUID owner) {
        config.set("players." + owner + ".visits", null);
        config.set("players." + owner + ".last-known-day", null);
        save();
    }

    // ─────────────────────────────────────────────────────
    //  Owner star — a manually-awarded badge shown next to
    //  the community stars, granted only via /ownerrate.
    // ─────────────────────────────────────────────────────

    public boolean hasOwnerStar(UUID owner) {
        return config.getBoolean("players." + owner + ".owner-star", false);
    }

    public void setOwnerStar(UUID owner, boolean awarded) {
        config.set("players." + owner + ".owner-star", awarded ? true : null);
        save();
    }

    /** A light-blue star to append after {@link #formatStars}, or "" if none was awarded. */
    public String ownerStarBadge(UUID owner) {
        return hasOwnerStar(owner) ? " " + Colors.LIGHT_BLUE + Symbols.STAR_FULL : "";
    }

    // ─────────────────────────────────────────────────────
    //  Automatic Island Score (0-100, cannot be voted on)
    // ─────────────────────────────────────────────────────

    public int getAutoScore(UUID owner) {
        double progressFraction = skillTreeManager.getProgressFraction(owner);

        long day = getIslandDay(owner);
        double dayFraction = Math.min(day, autoScoreMaxDay) / (double) autoScoreMaxDay;

        int uniqueVisitors = config.getStringList("players." + owner + ".visits.uniques").size();
        double popularityFraction = Math.min(uniqueVisitors, autoScoreMaxUniqueVisitors) / (double) autoScoreMaxUniqueVisitors;

        double score = (progressFraction * 40.0) + (dayFraction * 30.0) + (popularityFraction * 30.0);
        return (int) Math.round(score);
    }

    /**
     * Island day, cached so offline/unloaded islands still report a sensible
     * value instead of dropping to 0 whenever nobody is on them.
     */
    private long getIslandDay(UUID owner) {
        World world = WorldUtil.getIslandWorld(owner);
        if (world != null) {
            long day = (world.getFullTime() / 24000L) + 1L;
            config.set("players." + owner + ".last-known-day", day);
            return day;
        }
        return config.getLong("players." + owner + ".last-known-day", 0L);
    }

    public int getUniqueVisitorCount(UUID owner) {
        return config.getStringList("players." + owner + ".visits.uniques").size();
    }

    public int getTotalVisitCount(UUID owner) {
        return config.getInt("players." + owner + ".visits.total", 0);
    }

    /** Every island owner with any rating data at all, for the leaderboard. */
    public Set<UUID> topIslandsByAutoScore(int limit) {
        Set<UUID> candidates = new LinkedHashSet<>(skillTreeManager.getKnownPlayers());

        List<UUID> sorted = new ArrayList<>(candidates);
        sorted.sort((a, b) -> Integer.compare(getAutoScore(b), getAutoScore(a)));

        if (sorted.size() > limit) {
            return new LinkedHashSet<>(sorted.subList(0, limit));
        }
        return new LinkedHashSet<>(sorted);
    }
}
