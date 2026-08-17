package net.islandcore.webdashboard;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import com.sun.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class MetricsCollector {
    public record Snapshot(
            long timestamp,
            long usedMemory,
            long committedMemory,
            long maxMemory,
            long heapUsed,
            long heapMax,
            int onlinePlayers,
            int maxPlayers,
            List<String> players,
            List<WorldInfo> worlds,
            long uptimeMillis,
            double tps,
            double cpuUsage,
            long chunks,
            long entities,
            long networkInterfaces,
            long chatMessages,
            String serverVersion,
            String javaVersion,
            String osName,
            String osArch,
            int threads,
            long diskFree,
            long diskTotal
    ) {}

    public record WorldInfo(String name, String path, int players, int chunks, long entities) {}

    private final JavaPlugin plugin;
    private final AtomicReference<Snapshot> latest = new AtomicReference<>();
    private final AtomicLong chatMessages = new AtomicLong();
    private long startedAt = System.currentTimeMillis();
    private int taskId = -1;

    /** World names to leave out of the "Loaded worlds" table even while loaded
     *  (e.g. world_the_end/world_nether if you don't want them cluttering the box). */
    private Set<String> excludedWorlds = Collections.emptySet();

    public MetricsCollector(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setExcludedWorlds(Set<String> excludedWorlds) {
        this.excludedWorlds = excludedWorlds == null ? Collections.emptySet() : excludedWorlds;
    }

    public void start(long intervalTicks) {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::collect, 1L,
                Math.max(1L, intervalTicks)).getTaskId();
        collect();
    }

    public void stop() {
        if (taskId != -1) Bukkit.getScheduler().cancelTask(taskId);
    }

    public void markChat() {
        chatMessages.incrementAndGet();
    }

    public Snapshot get() {
        return latest.get();
    }

    /**
     * Recomputes the snapshot immediately instead of waiting for the next
     * scheduled tick. Called right after a world loads/unloads so the "Loaded
     * worlds" box reflects it the moment it happens rather than up to
     * metrics-interval-ticks later.
     */
    public void collectNow() {
        collect();
    }

    private void collect() {
        Runtime rt = Runtime.getRuntime();
        MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mx.getHeapMemoryUsage();

        long used = rt.totalMemory() - rt.freeMemory();
        long committed = rt.totalMemory();
        long max = rt.maxMemory();

        List<String> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) players.add(p.getName());

        List<WorldInfo> worlds = new ArrayList<>();
        long chunks = 0;
        long entities = 0;
        for (World w : Bukkit.getWorlds()) {
            int c = w.getLoadedChunks().length;
            long e = w.getEntities().size();
            // Totals stay server-wide truth even for worlds hidden from the table below.
            chunks += c;
            entities += e;
            if (excludedWorlds.contains(w.getName())) continue;
            worlds.add(new WorldInfo(
                    w.getName(),
                    w.getWorldFolder().getAbsolutePath(),
                    w.getPlayers().size(),
                    c,
                    e
            ));
        }

        latest.set(new Snapshot(
                System.currentTimeMillis(),
                used, committed, max,
                heap.getUsed(), heap.getMax(),
                Bukkit.getOnlinePlayers().size(),
                Bukkit.getMaxPlayers(),
                players, worlds,
                System.currentTimeMillis() - startedAt,
                getTps(),
                getCpuUsage(),
                chunks,
                entities,
                countNetworkInterfaces(),
                chatMessages.get(),
                Bukkit.getVersion(),
                System.getProperty("java.version", "unknown"),
                System.getProperty("os.name", "unknown"),
                System.getProperty("os.arch", "unknown"),
                Thread.getAllStackTraces().size(),
                getDiskFree(),
                getDiskTotal()
        ));
    }

    private long getDiskFree() { try { return plugin.getDataFolder().getAbsoluteFile().toPath().getRoot().toFile().getUsableSpace(); } catch (Throwable ignored) { return -1; } }
    private long getDiskTotal() { try { return plugin.getDataFolder().getAbsoluteFile().toPath().getRoot().toFile().getTotalSpace(); } catch (Throwable ignored) { return -1; } }

    private double getCpuUsage() {
        try {
            OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            if (os == null) return 0;
            double load = os.getCpuLoad();
            if (Double.isNaN(load) || load < 0) return 0;
            return Math.max(0, Math.min(100, load * 100.0));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private double getTps() {
        try {
            Object serverInstance = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            double[] tps = (double[]) serverInstance.getClass().getField("recentTps").get(serverInstance);

            return tps.length == 0 ? 0 : Math.max(0, Math.min(20, tps[0]));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private long countNetworkInterfaces() {
        try {
            long count = 0;
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en != null && en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                if (ni.isUp()) count++;
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }
}
