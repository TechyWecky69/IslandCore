package net.islandcore.plugin.skilltree;

import net.islandcore.plugin.util.Symbols;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Per-player skill tree state: which nodes are unlocked, which is active,
 * which biome is selected, and the player's token balance.
 * Persisted to data/skilltree.yml.
 */
public class SkillTreeManager {

    private final JavaPlugin plugin;
    private final SkillTree tree;
    private final File file;
    private FileConfiguration config;

    /**
     * Maps node ID → list of node IDs that must be unlocked before it.
     * Loaded from the prices config at startup under nodes.<id>.requires.
     */
    private final Map<String, List<String>> prerequisites = new HashMap<>();

    // In-memory cache so we don't hit YAML on every loot tick
    private final Map<UUID, Set<String>> unlockedNodes = new HashMap<>();
    private final Map<UUID, Set<String>> unlockedBiomes = new HashMap<>();
    private final Map<UUID, String> activeNode = new HashMap<>();
    private final Map<UUID, String> activeBiome = new HashMap<>();
    private final Map<UUID, Integer> tokens = new HashMap<>();

    public SkillTreeManager(JavaPlugin plugin, SkillTree tree, FileConfiguration prices) {
        this.plugin = plugin;
        this.tree = tree;
        File dataDir = new File(plugin.getDataFolder(), "data");
        dataDir.mkdirs();
        this.file = new File(dataDir, "skilltree.yml");
        loadPrerequisites(prices);
        load();
    }

    // ─────────────────────────────────────────────────────
    //  Prerequisites
    // ─────────────────────────────────────────────────────

    /**
     * Loads the 'requires' lists from skilltree-prices.yml.
     * Each node can declare: nodes.<id>.requires: [nodeA, nodeB, ...]
     * Biome nodes use: nodes.biomes.<id>.requires: [biomeA, biomeB, ...]
     */
    private void loadPrerequisites(FileConfiguration prices) {
        // Skill-tree nodes
        ConfigurationSection nodes = prices.getConfigurationSection("nodes");
        if (nodes != null) {
            for (String nodeId : nodes.getKeys(false)) {
                if (nodeId.equals("biomes")) continue; // handled below
                List<String> reqs = prices.getStringList("nodes." + nodeId + ".requires");
                if (!reqs.isEmpty()) {
                    prerequisites.put(nodeId, reqs);
                }
            }
        }

        // Biome nodes (stored under nodes.biomes.<id>)
        ConfigurationSection biomes = prices.getConfigurationSection("nodes.biomes");
        if (biomes != null) {
            for (String biomeId : biomes.getKeys(false)) {
                List<String> reqs = prices.getStringList("nodes.biomes." + biomeId + ".requires");
                if (!reqs.isEmpty()) {
                    // Use a namespaced key so biome prereqs don't collide with node prereqs
                    prerequisites.put("biome:" + biomeId, reqs);
                }
            }
        }
    }

    /**
     * Returns the display names of prerequisite nodes the player has NOT yet
     * unlocked for the given node ID. An empty list means all prerequisites
     * are satisfied (or there are none).
     */
    public List<String> getMissingPrerequisites(UUID uuid, String nodeId) {
        List<String> reqs = prerequisites.getOrDefault(nodeId, List.of());
        if (reqs.isEmpty()) return List.of();

        List<String> missing = new ArrayList<>();
        for (String reqId : reqs) {
            if (!isUnlocked(uuid, reqId)) {
                SkillNode reqNode = tree.getNode(reqId);
                String label = reqNode != null
                        ? stripColor(reqNode.getDisplayName())
                        : reqId;
                missing.add(label);
            }
        }
        return missing;
    }

    /** Returns true only when every prerequisite for the given node is unlocked. */
    public boolean prerequisitesMet(UUID uuid, String nodeId) {
        List<String> reqs = prerequisites.getOrDefault(nodeId, List.of());
        for (String reqId : reqs) {
            if (!isUnlocked(uuid, reqId)) return false;
        }
        return true;
    }

    /**
     * Same as getMissingPrerequisites but for biome nodes.
     * Prereqs listed are other biome IDs that must be unlocked first.
     */
    public List<String> getMissingBiomePrerequisites(UUID uuid, String biomeId) {
        List<String> reqs = prerequisites.getOrDefault("biome:" + biomeId, List.of());
        if (reqs.isEmpty()) return List.of();

        List<String> missing = new ArrayList<>();
        for (String reqId : reqs) {
            if (!isBiomeUnlocked(uuid, reqId)) {
                BiomeNode reqNode = tree.getBiomeNode(reqId);
                String label = reqNode != null
                        ? stripColor(reqNode.getDisplayName())
                        : reqId;
                missing.add(label);
            }
        }
        return missing;
    }

    /** Returns true only when every biome prerequisite for the given biome is unlocked. */
    public boolean biomePrerequisitesMet(UUID uuid, String biomeId) {
        List<String> reqs = prerequisites.getOrDefault("biome:" + biomeId, List.of());
        for (String reqId : reqs) {
            if (!isBiomeUnlocked(uuid, reqId)) return false;
        }
        return true;
    }

    private static String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }

    // ─────────────────────────────────────────────────────
    //  Persistence
    // ─────────────────────────────────────────────────────

    private void load() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create data/skilltree.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        // Populate in-memory cache from YAML
        if (config.isConfigurationSection("players")) {
            for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String base = "players." + uuidStr + ".";

                    List<String> unlocked = config.getStringList(base + "unlocked");
                    if (unlocked.contains("end")) {
                        unlocked = new ArrayList<>(unlocked);
                        unlocked.remove("end");
                        unlocked.add("end2");
                    }
                    unlockedNodes.put(uuid, new HashSet<>(unlocked));

                    List<String> biomes = config.getStringList(base + "biomes");
                    biomes.removeIf(id -> tree.getBiomeNode(id) == null);
                    if (biomes.isEmpty()) biomes.add("oak");
                    unlockedBiomes.put(uuid, new HashSet<>(biomes));

                    String active = config.getString(base + "active");
                    if (active != null) {
                        // v3.0.1 used a single "end" node; the redesigned tree
                        // splits that branch into End T1 and End T2.
                        if (active.equals("end")) active = "end2";
                        activeNode.put(uuid, active);
                    }

                    String biome = config.getString(base + "activeBiome");
                    if (biome != null && tree.getBiomeNode(biome) != null) {
                        activeBiome.put(uuid, biome);
                    } else {
                        activeBiome.put(uuid, "oak");
                    }

                    tokens.put(uuid, config.getInt(base + "tokens", 0));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public synchronized void save() {
        for (UUID uuid : unlockedNodes.keySet()) {
            String base = "players." + uuid + ".";
            config.set(base + "unlocked", new ArrayList<>(unlockedNodes.getOrDefault(uuid, Set.of())));
            config.set(base + "biomes", new ArrayList<>(unlockedBiomes.getOrDefault(uuid, Set.of())));
            config.set(base + "active", activeNode.get(uuid));
            config.set(base + "activeBiome", activeBiome.get(uuid));
            config.set(base + "tokens", tokens.getOrDefault(uuid, 0));
        }
        try { config.save(file); } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data/skilltree.yml", e);
        }
    }

    // ─────────────────────────────────────────────────────
    //  Player initialisation
    // ─────────────────────────────────────────────────────

    /** Call when a new player joins for the first time. Gives them the start node. */
    public void initPlayer(UUID uuid) {
        if (unlockedNodes.containsKey(uuid)) return;
        Set<String> starter = new HashSet<>();
        starter.add("start");
        unlockedNodes.put(uuid, starter);
        unlockedBiomes.put(uuid, new HashSet<>(Set.of("oak")));
        activeNode.put(uuid, "start");
        activeBiome.put(uuid, "oak");
        tokens.put(uuid, 0);
    }

    /** Wipes a player's skill tree progress entirely so initPlayer() starts them over. */
    public synchronized void resetPlayer(UUID uuid) {
        unlockedNodes.remove(uuid);
        unlockedBiomes.remove(uuid);
        activeNode.remove(uuid);
        activeBiome.remove(uuid);
        tokens.remove(uuid);
        config.set("players." + uuid, null);
        save();
    }

    // ─────────────────────────────────────────────────────
    //  Token management
    // ─────────────────────────────────────────────────────

    /** Every player UUID that has ever joined (and so has skill tree state). Used by the ratings leaderboard. */
    public Set<UUID> getKnownPlayers() { return Collections.unmodifiableSet(unlockedNodes.keySet()); }

    /** Fraction (0.0-1.0) of skill nodes + biomes this player has unlocked. Used for the automatic island score. */
    public double getProgressFraction(UUID uuid) {
        int totalNodes = tree.getNodes().size();
        int totalBiomes = tree.getBiomeNodes().size();
        if (totalNodes == 0 && totalBiomes == 0) return 0.0;

        double nodeFrac = totalNodes == 0 ? 0.0 : (double) getUnlockedNodes(uuid).size() / totalNodes;
        double biomeFrac = totalBiomes == 0 ? 0.0 : (double) getUnlockedBiomes(uuid).size() / totalBiomes;

        if (totalBiomes == 0) return nodeFrac;
        return (nodeFrac + biomeFrac) / 2.0;
    }

    public int getTokens(UUID uuid) { return tokens.getOrDefault(uuid, 0); }

    public void addTokens(UUID uuid, int amount) {
        tokens.merge(uuid, amount, Integer::sum);
    }

    public boolean spendTokens(UUID uuid, int amount) {
        int current = getTokens(uuid);
        if (current < amount) return false;
        tokens.put(uuid, current - amount);
        return true;
    }

    // ─────────────────────────────────────────────────────
    //  Node state
    // ─────────────────────────────────────────────────────

    public boolean isUnlocked(UUID uuid, String nodeId) {
        return unlockedNodes.getOrDefault(uuid, Set.of()).contains(nodeId);
    }

    public Set<String> getUnlockedNodes(UUID uuid) {
        return Collections.unmodifiableSet(unlockedNodes.getOrDefault(uuid, Set.of()));
    }

    public String getActiveNode(UUID uuid) { return activeNode.get(uuid); }

    public void setActiveNode(UUID uuid, String nodeId) { activeNode.put(uuid, nodeId); }

    /**
     * Attempts to unlock a node. Checks token balance and prerequisites —
     * item costs are checked and consumed by SkillTreeListener before calling
     * this. Returns true on success.
     */
    public boolean unlockNode(UUID uuid, String nodeId) {
        SkillNode node = tree.getNode(nodeId);
        if (node == null) return false;
        if (isUnlocked(uuid, nodeId)) return false;
        if (!prerequisitesMet(uuid, nodeId)) return false;
        if (!spendTokens(uuid, node.getTokenCost())) return false;
        unlockedNodes.computeIfAbsent(uuid, k -> new HashSet<>()).add(nodeId);
        return true;
    }

    // ─────────────────────────────────────────────────────
    //  Biome state
    // ─────────────────────────────────────────────────────

    public boolean isBiomeUnlocked(UUID uuid, String biomeId) {
        return unlockedBiomes.getOrDefault(uuid, Set.of()).contains(biomeId);
    }

    public Set<String> getUnlockedBiomes(UUID uuid) {
        return Collections.unmodifiableSet(unlockedBiomes.getOrDefault(uuid, Set.of()));
    }

    public String getActiveBiome(UUID uuid) { return activeBiome.getOrDefault(uuid, "oak"); }

    public void setActiveBiome(UUID uuid, String biomeId) { activeBiome.put(uuid, biomeId); }

    public boolean unlockBiome(UUID uuid, String biomeId) {
        BiomeNode node = tree.getBiomeNode(biomeId);
        if (node == null) return false;
        if (isBiomeUnlocked(uuid, biomeId)) return false;
        if (!biomePrerequisitesMet(uuid, biomeId)) return false;
        if (!spendTokens(uuid, node.getTokenCost())) return false;
        unlockedBiomes.computeIfAbsent(uuid, k -> new HashSet<>()).add(biomeId);
        return true;
    }

    // ─────────────────────────────────────────────────────
    //  Admin utilities
    // ─────────────────────────────────────────────────────

    /**
     * Unlocks every skill node and every biome for the given player instantly,
     * bypassing token costs, item costs, and prerequisites.
     * Intended for admin/owner use only — the caller is responsible for
     * verifying the player's rank before invoking this.
     */
    public void unlockAll(UUID uuid) {
        Set<String> nodes = unlockedNodes.computeIfAbsent(uuid, k -> new HashSet<>());
        for (String nodeId : tree.getNodes().keySet()) {
            nodes.add(nodeId);
        }

        Set<String> biomes = unlockedBiomes.computeIfAbsent(uuid, k -> new HashSet<>());
        for (String biomeId : tree.getBiomeNodes().keySet()) {
            biomes.add(biomeId);
        }
    }

    // ─────────────────────────────────────────────────────
    //  Loot resolution
    // ─────────────────────────────────────────────────────

    /**
     * Picks a random loot item from the player's active node's loot pool.
     * If the active node is a forest-related node, biome drops are mixed in.
     * Returns null if nothing is active.
     *
     * <p>Enchanted books are given a real random enchantment (rather than a
     * blank book) so they're actually usable at an anvil. The level is
     * tiered by the player's enchanting node: T2 caps at level 1 per its
     * description, T3 always rolls the enchantment's maximum level, and
     * everything else (including T1) rolls a random valid level.
     */
    public ItemStack pickLoot(UUID uuid, Random random) {
        String nodeId = activeNode.get(uuid);
        if (nodeId == null) return null;

        SkillNode node = tree.getNode(nodeId);
        if (node == null) return null;

        List<SkillNode.LootEntry> pool = new ArrayList<>(tree.getLootPool(nodeId));

        // The Biome node is intentionally dynamic: its loot is the complete
        // material family represented by the biome selected on the biome tab.
        String biomeId = activeBiome.get(uuid);
        BiomeNode biome = biomeId == null ? null : tree.getBiomeNode(biomeId);
        if (biome != null && node.getGroups().contains(SkillItemRegistry.Group.BIOME)) {
            for (Material material : biome.getDrops()) {
                if (tree.getItemRegistry().isAllowed(material)) {
                    pool.add(new SkillNode.LootEntry(material, SkillNode.Rarity.COMMON));
                }
            }
        }

        if (pool.isEmpty()) return null;

        int totalWeight = pool.stream().mapToInt(SkillNode.LootEntry::getWeight).sum();
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        Material chosen = null;
        for (SkillNode.LootEntry entry : pool) {
            cumulative += entry.getWeight();
            if (roll < cumulative) {
                chosen = entry.getMaterial();
                break;
            }
        }
        if (chosen == null) {
            chosen = pool.get(pool.size() - 1).getMaterial();
        }

        return buildLootItem(chosen, node, random);
    }

    private ItemStack buildLootItem(Material material, SkillNode node, Random random) {
        if (material == Material.ENCHANTED_BOOK) {
            return createEnchantedBook(node, random);
        }
        return new ItemStack(material, 1);
    }

    /** Builds an enchanted book carrying one real, randomly-picked enchantment. */
    private ItemStack createEnchantedBook(SkillNode node, Random random) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta rawMeta = book.getItemMeta();

        if (!(rawMeta instanceof EnchantmentStorageMeta meta)) {
            return book;
        }

        Enchantment[] all = Enchantment.values();
        Enchantment enchant = all[random.nextInt(all.length)];

        int level;
        if (node.getGroups().contains(SkillItemRegistry.Group.ENCHANTING_T3)) {
            // Sorcerer: always the enchantment's maximum level
            level = enchant.getMaxLevel();
        } else if (node.getGroups().contains(SkillItemRegistry.Group.ENCHANTING_T2)) {
            // Wizard: capped at level 1, except enchantments whose minimum is higher than 1
            level = Math.max(enchant.getStartLevel(), 1);
        } else {
            // Amateur Magic and everything else: a random valid level for that enchantment
            level = enchant.getStartLevel()
                    + random.nextInt((enchant.getMaxLevel() - enchant.getStartLevel()) + 1);
        }
        level = Math.max(enchant.getStartLevel(), Math.min(level, enchant.getMaxLevel()));

        meta.addStoredEnchant(enchant, level, true);
        book.setItemMeta(meta);
        return book;
    }

    // ─────────────────────────────────────────────────────
    //  Chat / nametag symbol
    // ─────────────────────────────────────────────────────

    /**
     * Returns the Unicode symbol for the player's currently-active skill node,
     * surrounded by brackets: e.g. {@code [⛏]}.
     * Falls back to {@code [🌲]} (the generic skill-tree icon) when the
     * player has no active node yet.
     */
    public String getActiveSymbol(UUID uuid) {
        String nodeId = activeNode.get(uuid);
        if (nodeId != null) {
            SkillNode node = tree.getNode(nodeId);
            if (node != null) {
                return "\u00a77[" + node.getColoredSymbol() + "\u00a77]";
            }
        }
        // Default before any node is selected — dark-grey brackets, plain tree icon
        return "\u00a77[" + Symbols.SKILL_TREE + "\u00a77]";
    }
}
