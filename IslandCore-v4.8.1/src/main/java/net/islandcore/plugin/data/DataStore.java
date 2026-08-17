package net.islandcore.plugin.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

/** Persistent player/island data. Stored neatly under the plugin data directory. */
public class DataStore {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        this.file = new File(dataDir, "playerdata.yml");
        migrateLegacyFile();
        load();
    }

    private void migrateLegacyFile() {
        File legacy = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!file.exists() && legacy.exists()) {
            try {
                if (legacy.renameTo(file)) {
                    plugin.getLogger().info("Migrated playerdata.yml to data/playerdata.yml.");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Could not migrate legacy playerdata.yml", e);
            }
        }
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data/playerdata.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data/playerdata.yml", e);
        }
    }

    public String getRank(UUID uuid) { return config.getString("ranks." + uuid + ".rank"); }
    public void setRank(UUID uuid, String rank) { setRank(uuid, rank, true); }
    public void setRank(UUID uuid, String rank, boolean save) {
        config.set("ranks." + uuid + ".rank", rank);
        if (save) save();
    }

    public boolean isVisitable(UUID uuid) { return config.getBoolean("islands." + uuid + ".visitable", false); }
    public void setVisitable(UUID uuid, boolean visitable) {
        config.set("islands." + uuid + ".visitable", visitable);
        save();
    }

    public boolean isLootingActive(UUID uuid) { return config.getBoolean("game." + uuid + ".status", false); }
    public void setLootingActive(UUID uuid, boolean active) {
        config.set("game." + uuid + ".status", active);
        save();
    }

    /** Number of successful random-pull item rewards received by the player. */
    public int getLootPulls(UUID uuid) { return config.getInt("game." + uuid + ".loot-pulls", 0); }

    public void incrementLootPulls(UUID uuid) {
        config.set("game." + uuid + ".loot-pulls", getLootPulls(uuid) + 1);
    }

    public void setWorldName(UUID uuid, String worldName) {
        config.set("worldnames." + uuid, worldName);
        save();
    }

    public String getWorldName(UUID uuid) { return config.getString("worldnames." + uuid); }

    /**
     * Wipes everything tied to a player's island (visitable flag, looting
     * state, loot-pull count, stored world name) so the next join starts
     * completely fresh. Rank is left untouched - a reset is not a demotion.
     */
    public synchronized void resetPlayer(UUID uuid) {
        config.set("islands." + uuid, null);
        config.set("game." + uuid, null);
        config.set("worldnames." + uuid, null);
        save();
    }
}
