package net.islandcore.plugin;

import net.islandcore.plugin.commands.*;
import net.islandcore.plugin.data.DataStore;
import net.islandcore.plugin.friends.FriendManager;
import net.islandcore.plugin.gui.PlayerContextGUI;
import net.islandcore.plugin.gui.VisitConfirmGUI;
import net.islandcore.plugin.listeners.*;
import net.islandcore.plugin.managers.IslandManager;
import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.ratings.RatingManager;
import net.islandcore.plugin.skilltree.SkillTree;
import net.islandcore.plugin.skilltree.SkillTreeGUI;
import net.islandcore.plugin.skilltree.SkillTreeListener;
import net.islandcore.plugin.skilltree.SkillTreeManager;
import net.islandcore.plugin.tasks.*;
import net.islandcore.plugin.trade.TradeGUI;
import net.islandcore.plugin.trade.TradeListener;
import net.islandcore.plugin.trade.TradeManager;
import net.islandcore.plugin.util.BypassUtil;
import net.islandcore.plugin.util.WorldUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import net.islandcore.plugin.commands.TokenCommand;

import java.util.List;

public class IslandCorePlugin extends JavaPlugin {

    private DataStore dataStore;
    private RankManager rankManager;
    private AfkListener afkListener;
    private IslandManager islandManager;
    private SkillTreeManager skillTreeManager;
    private RatingManager ratingManager;
    private TokenCommand tokenCommand;
    private FriendManager friendManager;
    private TradeManager tradeManager;
    private TradeListener tradeListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        FileConfiguration config = getConfig();

        // Upgrade prices live in their own editable file so balancing does not
        // require changing/recompiling the plugin.
        saveResource("skilltree-prices.yml", false);
        File priceFile = new File(getDataFolder(), "skilltree-prices.yml");
        FileConfiguration prices = YamlConfiguration.loadConfiguration(priceFile);

        WorldUtil.setPrefix(config.getString("world-prefix", "worlds/"));
        WorldUtil.setBorderSize(config.getDouble("island-border-size", 200.0));
        BypassUtil.setLegacyName(config.getString("legacy-bypass-name", "ILiveOffCaffine"));

        dataStore = new DataStore(this);
        rankManager = new RankManager(this, dataStore);
        islandManager = new IslandManager(this);
        friendManager = new FriendManager(this);
        tradeManager = new TradeManager(this);

        // Skill tree
        SkillTree skillTree = new SkillTree(this, prices);
        skillTreeManager = new SkillTreeManager(this, skillTree, prices);
        SkillTreeGUI skillTreeGUI = new SkillTreeGUI(skillTree, skillTreeManager, rankManager);

        // Wire skill-tree into rank manager so chat/nametags can show the active symbol
        rankManager.setSkillTreeManager(skillTreeManager);

        // Island ratings: community stars + automatic score, both read skill tree progress
        ratingManager = new RatingManager(this, skillTree, skillTreeManager);

        // Wire ratings into rank manager so the tab list can show the owner star badge
        rankManager.setRatingManager(ratingManager);

        tokenCommand = new TokenCommand(
                this,
                skillTreeManager
        );

        registerCommands(skillTreeGUI);
        registerListeners(skillTree, skillTreeGUI);
        registerTasks(config);

        getLogger().info("IslandCore v4.8.0 enabled.");
    }

    @Override
    public void onDisable() {
        if (tradeListener != null)    tradeListener.cancelAllTrades("Server is restarting - your items have been returned.");
        if (rankManager != null)      rankManager.removeAll();
        if (afkListener != null)      afkListener.shutdown();
        if (islandManager != null)    islandManager.shutdown();
        if (skillTreeManager != null) skillTreeManager.save();
        if (ratingManager != null)    ratingManager.save();
        if (friendManager != null)    friendManager.save();
        if (dataStore != null)        dataStore.save();
    }

    public RankManager getRankManager() { return rankManager; }

    private void registerCommands(SkillTreeGUI skillTreeGUI) {
        VisitConfirmGUI visitConfirmGUI = new VisitConfirmGUI(dataStore, islandManager, ratingManager);
        getServer().getPluginManager().registerEvents(visitConfirmGUI, this);

        PlayerContextGUI playerContextGUI = new PlayerContextGUI(visitConfirmGUI, rankManager, ratingManager);
        getServer().getPluginManager().registerEvents(playerContextGUI, this);

        getCommand("visit").setExecutor(new VisitCommand(dataStore, visitConfirmGUI, islandManager));
        getCommand("home").setExecutor(new HomeCommand(islandManager));
        getCommand("setspawn").setExecutor(new SetSpawnCommand());
        getCommand("myisland").setExecutor(new MyIslandCommand(ratingManager, dataStore));
        getCommand("stafftp").setExecutor(new StaffTPCommand(dataStore, islandManager));
        getCommand("msg").setExecutor(new MsgCommand());
        getCommand("reply").setExecutor(new ReplyCommand());
        getCommand("invsee").setExecutor(new InvseeCommand());
        getCommand("enderchest").setExecutor(new EnderchestCommand());
        getCommand("kick").setExecutor(new KickCommand());
        getCommand("ban").setExecutor(new BanCommand());
        getCommand("toggle").setExecutor(new ToggleCommand(dataStore));
        getCommand("report").setExecutor(new ReportCommand());
        getCommand("reportisland").setExecutor(new ReportIslandCommand());
        getCommand("skilltree").setExecutor(new SkillTreeCommand(skillTreeGUI));
        getCommand("spawntoken").setExecutor(tokenCommand);
        getCommand("rate").setExecutor(new RateCommand(ratingManager));

        getCommand("topislands").setExecutor(new TopIslandsCommand(ratingManager));
        getCommand("resetratings").setExecutor(new ResetRatingsCommand(ratingManager));
        getCommand("ownerrate").setExecutor(new OwnerRateCommand(ratingManager, rankManager));
        getCommand("removeownerrate").setExecutor(new RemoveOwnerRateCommand(ratingManager, rankManager));
        getCommand("resetislandscore").setExecutor(new ResetIslandScoreCommand(ratingManager, skillTreeManager));


        RankCommand rankCommand = new RankCommand(rankManager);
        getCommand("rank").setExecutor(rankCommand);
        getCommand("rank").setTabCompleter(rankCommand);

        getCommand("toggleislandvisits").setExecutor(new ToggleIslandVisitsCommand(dataStore, rankManager));

        ResetPlayerCommand resetPlayerCommand =
                new ResetPlayerCommand(this, dataStore, islandManager, skillTreeManager);
        getCommand("resetplayer").setExecutor(resetPlayerCommand);
        getServer().getPluginManager().registerEvents(resetPlayerCommand, this);

        FriendCommand friendCommand = new FriendCommand(friendManager);
        getCommand("friend").setExecutor(friendCommand);
        getCommand("friend").setTabCompleter(friendCommand);

        TradeGUI tradeGUI = new TradeGUI();
        TradeCommand tradeCommand = new TradeCommand(tradeManager, tradeGUI);
        getCommand("trade").setExecutor(tradeCommand);
        getCommand("trade").setTabCompleter(tradeCommand);
        tradeListener = new TradeListener(tradeManager, tradeGUI);
        getServer().getPluginManager().registerEvents(tradeListener, this);
    }

    private void registerListeners(SkillTree skillTree, SkillTreeGUI skillTreeGUI) {
        List<String> worldCreateCommands = getConfig().getStringList("world-create-commands");

        getServer().getPluginManager().registerEvents(new ProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new EntityLimitListener(this), this);
        getServer().getPluginManager().registerEvents(new RedstoneLimitListener(this), this);

        getServer().getPluginManager().registerEvents(
                new JoinListener(this, dataStore, worldCreateCommands, islandManager, skillTreeManager), this);

        getServer().getPluginManager().registerEvents(
                new WorldChangeListener(this, dataStore, islandManager, ratingManager), this);

        getServer().getPluginManager().registerEvents(new RankListener(rankManager), this);
        getServer().getPluginManager().registerEvents(new DeathListener(), this);

        getServer().getPluginManager().registerEvents(
                new DisconnectListener(this, dataStore, islandManager, ratingManager), this);

        getServer().getPluginManager().registerEvents(new TabListListener(this), this);
        getServer().getPluginManager().registerEvents(new VoidDamageListener(), this);
        getServer().getPluginManager().registerEvents(new VisitorRespawnListener(this), this);

        // Skill tree GUI listener
        getServer().getPluginManager().registerEvents(
                new SkillTreeListener(skillTree, skillTreeManager, skillTreeGUI, rankManager), this);

        afkListener = new AfkListener(this);
        getServer().getPluginManager().registerEvents(afkListener, this);

        getServer().getPluginManager().registerEvents(tokenCommand, this);

        getServer().getPluginManager().registerEvents(new FriendListener(friendManager), this);
    }

    private void registerTasks(FileConfiguration config) {
        int intervalSeconds = Math.max(1, config.getInt("loot-interval-seconds", 30));

        // LootTask now uses SkillTreeManager instead of a flat material list
        new LootTask(dataStore, skillTreeManager, intervalSeconds)
                .runTaskTimer(this, intervalSeconds * 20L, intervalSeconds * 20L);

        new ActionBarTask(dataStore)
                .runTaskTimer(this, 20L, 20L);

        int cleanupSeconds = Math.max(10, config.getInt("item-cleanup.interval-seconds", 60));
        int itemAgeSeconds = Math.max(30, config.getInt("item-cleanup.max-age-seconds", 300));
        new ItemCleanupTask(itemAgeSeconds)
                .runTaskTimer(this, cleanupSeconds * 20L, cleanupSeconds * 20L);

        int maintenanceSeconds = Math.max(10, config.getInt("island-maintenance.interval-seconds", 30));
        new IslandMaintenanceTask(islandManager)
                .runTaskTimer(this, maintenanceSeconds * 20L, maintenanceSeconds * 20L);

        int scoreboardSeconds = Math.max(1, config.getInt("scoreboard.refresh-seconds", 1));
        new ScoreboardTask(dataStore, this, rankManager, ratingManager)
                .runTaskTimer(this, scoreboardSeconds * 20L, scoreboardSeconds * 20L);

        int dataSaveSeconds = Math.max(30, config.getInt("data-save-interval-seconds", 300));
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                dataStore.save();
                skillTreeManager.save();
                ratingManager.save();
                friendManager.save();
            }
        }.runTaskTimer(this, dataSaveSeconds * 20L, dataSaveSeconds * 20L);
    }
}
