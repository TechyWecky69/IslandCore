package net.islandcore.plugin.skilltree;

import net.islandcore.plugin.util.Colors;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

import static net.islandcore.plugin.skilltree.BiomeNode.BiomeType.FOREST;
import static net.islandcore.plugin.skilltree.BiomeNode.BiomeType.NETHER;
import static net.islandcore.plugin.skilltree.SkillItemRegistry.Group.*;
import static net.islandcore.plugin.skilltree.SkillNode.Rarity.*;

/**
 * The visual skill-tree definition. The GUI slot numbers deliberately mirror
 * the supplied redesign exactly; loot itself is resolved through
 * SkillItemRegistry so a material can safely belong to multiple branches.
 */
public class SkillTree {

    private final SkillItemRegistry items;
    private final FileConfiguration prices;
    private final Map<String, SkillNode> nodes = new LinkedHashMap<>();
    private final Map<String, BiomeNode> biomeNodes = new LinkedHashMap<>();

    public SkillTree(JavaPlugin plugin, FileConfiguration prices) {
        this.items = new SkillItemRegistry(plugin);
        this.prices = prices;
        buildTree();
        buildBiomes();
    }

    public SkillItemRegistry getItemRegistry() { return items; }

    private static List<SkillNode.ItemCost> costs(Object... pairs) {
        List<SkillNode.ItemCost> result = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.add(new SkillNode.ItemCost((Material) pairs[i], (int) pairs[i + 1]));
        }
        return result;
    }

    private void add(SkillNode node) { nodes.put(node.getId(), node); }

    private SkillNode node(String id, String name, Material icon, int slot, int tokenCost,
                           String[] description, List<SkillNode.ItemCost> costs,
                           SkillItemRegistry.Group... groups) {
        EnumSet<SkillItemRegistry.Group> groupSet = EnumSet.noneOf(SkillItemRegistry.Group.class);
        groupSet.addAll(Arrays.asList(groups));
        return new SkillNode(id, name, icon, description, groupSet, configuredTokens(id, tokenCost), configuredItems(id, costs), slot);
    }

    private void buildTree() {

        add(node(
                "start",
                "§a§lStart",
                SkillTreeSlots.MAIN_START_ICON,
                SkillTreeSlots.MAIN_START,
                0,
                new String[]{
                        "§7Your starting collection.",
                        "§7Basic early-game materials."
                },
                costs(),
                START
        ));

    /* =========================================================
       MINING
       ========================================================= */

        add(node(
                "mining1",
                Msg.color("&7[" + Colors.BRONZE + Symbols.MINING + "&7] §6§lCave opening"),
                SkillTreeSlots.MAIN_MINING_T1_ICON,
                SkillTreeSlots.MAIN_MINING_T1,
                25,
                new String[]{
                        "§7Cobblestone, andesite, coal, copper",
                        "§7and other early mining materials."
                },
                costs(
                        Material.OAK_PLANKS, 4,
                        Material.DIRT, 2
                ),
                MINING_T1
        ));

        add(node(
                "mining2",
                Msg.color("&7[" + Colors.SILVER + Symbols.MINING + "&7] §6§lDeep Caverns"),
                SkillTreeSlots.MAIN_MINING_T2_ICON,
                SkillTreeSlots.MAIN_MINING_T2,
                50,
                new String[]{
                        "§7Iron, gold, redstone, lapis,",
                        "§7blast furnace and advanced mining."
                },
                costs(
                        Material.COBBLESTONE, 8,
                        Material.COAL, 4
                ),
                MINING_T2
        ));

        add(node(
                "mining3",
                Msg.color("&7[" + Colors.GOLD + Symbols.MINING + "&7] §6§lMineshaft"),
                SkillTreeSlots.MAIN_MINING_T3_ICON,
                SkillTreeSlots.MAIN_MINING_T3,
                100,
                new String[]{
                        "§7Diamonds, deepslate, lava,",
                        "§7amethyst and the deepest materials."
                },
                costs(
                        Material.IRON_INGOT, 16,
                        Material.REDSTONE, 16
                ),
                MINING_T3
        ));


    /* =========================================================
       END
       ========================================================= */

        add(node(
                "end1",
                Msg.color("&7[" + Colors.SILVER + Symbols.END + "&7] §5§lEnd Island"),
                SkillTreeSlots.MAIN_END_T1_ICON,
                SkillTreeSlots.MAIN_END_T1,
                100,
                new String[]{
                        "§7End stone, obsidian, chorus",
                        "§7and basic End materials."
                },
                costs(
                        Material.OBSIDIAN, 4,
                        Material.ENDER_PEARL, 2
                ),
                END_T1
        ));

        add(node(
                "end2",
                Msg.color("&7[" + Colors.GOLD + Symbols.END + "&7] §5§lEnd City"),
                SkillTreeSlots.MAIN_END_T2_ICON,
                SkillTreeSlots.MAIN_END_T2,
                200,
                new String[]{
                        "§7End City blocks, Elytra, diamonds,",
                        "§7gold, Ender items and End City loot."
                },
                costs(
                        Material.END_STONE, 32,
                        Material.DIAMOND, 8
                ),
                END_T2
        ));


    /* =========================================================
       NETHER
       ========================================================= */

        add(node(
                "nether1",
                Msg.color("&7[" + Colors.SILVER + Symbols.NETHER + "&7] §c§lWastelands"),
                SkillTreeSlots.MAIN_NETHER_T1_ICON,
                SkillTreeSlots.MAIN_NETHER_T1,
                100,
                new String[]{
                        "§7Netherrack, quartz, gold, nuggets,",
                        "§7gravel, soul sand, basalt and common Nether items."
                },
                costs(
                        Material.OBSIDIAN, 12,
                        Material.FLINT_AND_STEEL, 1
                ),
                NETHER_T1
        ));

        add(node(
                "nether2",
                Msg.color("&7[" + Colors.GOLD + Symbols.NETHER + "&7] §c§lNether Civilisation"),
                SkillTreeSlots.MAIN_NETHER_T2_ICON,
                SkillTreeSlots.MAIN_NETHER_T2,
                200,
                new String[]{
                        "§7Nether fortress loot, Nether bricks,",
                        "§7wither skulls, bastion loot and ancient debris."
                },
                costs(
                        Material.NETHERRACK, 32,
                        Material.QUARTZ, 8
                ),
                NETHER_T2
        ));


    /* =========================================================
       FARMING
       ========================================================= */

        add(node(
                "farming1",
                Msg.color("&7[" + Colors.BRONZE + Symbols.FARM + "&7] §2§lIsland Farmer"),
                SkillTreeSlots.MAIN_FARMING_T1_ICON,
                SkillTreeSlots.MAIN_FARMING_T1,
                25,
                new String[]{
                        "§7Wheat, seeds, carrots, hay bales",
                        "§7and other early farming materials."
                },
                costs(
                        Material.DIRT, 4,
                        Material.WHEAT_SEEDS, 4
                ),
                FARMING_T1
        ));

        add(node(
                "farming2",
                Msg.color("&7[" + Colors.SILVER + Symbols.FARM + "&7] §2§lProfessional"),
                SkillTreeSlots.MAIN_FARMING_T2_ICON,
                SkillTreeSlots.MAIN_FARMING_T2,
                50,
                new String[]{
                        "§7Pumpkin, melon, golden apples,",
                        "§7golden carrots, composters and hay bales."
                },
                costs(
                        Material.WHEAT, 32,
                        Material.WHEAT_SEEDS, 32
                ),
                FARMING_T2
        ));

        add(node(
                "farming3",
                Msg.color("&7[" + Colors.GOLD + Symbols.FARM +  "&7] §2§lSlaughterhouse"),
                SkillTreeSlots.MAIN_FARMING_T3_ICON,
                SkillTreeSlots.MAIN_FARMING_T3,
                100,
                new String[]{
                        "§7Animal meat and cooked variants,",
                        "§7leather, feathers, rabbit hide and more."
                },
                costs(
                        Material.MELON_SLICE, 64,
                        Material.PUMPKIN, 32
                ),
                FARMING_T3
        ));


    /* =========================================================
       FISHING
       ========================================================= */

        add(node(
                "fishing",
                Msg.color("&7[" + Colors.GOLD + Symbols.FISHING + "&7] §3§lFishing Dock"),
                SkillTreeSlots.MAIN_FISHING_ICON,
                SkillTreeSlots.MAIN_FISHING,
                200,
                new String[]{
                        "§7Everything obtainable from fishing:",
                        "§7fish, rods and fishing treasure."
                },
                costs(
                        Material.STRING, 4,
                        Material.LEATHER, 16
                ),
                FISHING
        ));


    /* =========================================================
       FLOWERS
       ========================================================= */

        add(node(
                "flowers1",
                Msg.color("&7[" + Colors.BRONZE + Symbols.FLOWER + "&7] §d§lFlowers"),
                SkillTreeSlots.MAIN_FLOWERS_T1_ICON,
                SkillTreeSlots.MAIN_FLOWERS_T1,
                25,
                new String[]{
                        "§7Flowers found growing",
                        "§7across the island."
                },
                costs(),
                FLOWERS_T1
        ));

        add(node(
                "flowers2",
                Msg.color("&7[" + Colors.SILVER + Symbols.FLOWER + "&7] §d§lBouquet"),
                SkillTreeSlots.MAIN_FLOWERS_T2_ICON,
                SkillTreeSlots.MAIN_FLOWERS_T2,
                50,
                new String[]{
                        "§7Rarer flowers and flower-based",
                        "§7materials."
                },
                costs(),
                FLOWERS_T2
        ));


    /* =========================================================
       BIOME
       ========================================================= */

        add(node(
                "biome",
                Msg.color("&7[" + Colors.GOLD + Symbols.BIOME + "&7] §e§lBiome"),
                SkillTreeSlots.MAIN_BIOME_ICON,
                SkillTreeSlots.MAIN_BIOME,
                20,
                new String[]{
                        "§7All building blocks based on the",
                        "§7currently selected biome."
                },
                costs(
                        Material.CRAFTING_TABLE, 1,
                        Material.OAK_PLANKS, 8,
                        Material.COBBLESTONE, 8,
                        Material.COAL, 4
                ),
                BIOME
        ));


    /* =========================================================
       COLOUR
       ========================================================= */

        add(node(
                "colour",
                Msg.color("&7[" + Colors.GOLD + Symbols.COLOUR + "&7] §e§lColour"),
                SkillTreeSlots.MAIN_COLOR_ICON,
                SkillTreeSlots.MAIN_COLOUR,
                30,
                new String[]{
                        "§7Concrete, concrete powder, terracotta,",
                        "§7glazed terracotta, wool and other",
                        "§7colourful decoration blocks."
                },
                costs(
                        Material.CLAY, 32,
                        Material.COBBLESTONE, 16
                ),
                COLOUR
        ));


    /* =========================================================
       ENCHANTING
       ========================================================= */

        add(node(
                "enchanting1",
                Msg.color("&7[" + Colors.BRONZE + Symbols.ENCHANTING + "&7] §d§lAmateur Magic"),
                SkillTreeSlots.ENCH_T1_ICON,
                SkillTreeSlots.ENCHANTING_T1,
                40,
                new String[]{
                        "§7Enchanting tables, lapis, basic books,",
                        "§7bookshelves, book & quill and basics."
                },
                costs(
                        Material.DIAMOND, 8,
                        Material.LAPIS_LAZULI, 16
                ),
                ENCHANTING_T1
        ));

        add(node(
                "enchanting2",
                Msg.color("&7[" + Colors.SILVER + Symbols.ENCHANTING + "&7] §d§lWizard"),
                SkillTreeSlots.ENCH_T2_ICON,
                SkillTreeSlots.ENCHANTING_T2,
                55,
                new String[]{
                        "§7Enchanted books with a maximum level",
                        "§7of 1, except enchantments without level 1."
                },
                costs(
                        Material.LAPIS_LAZULI, 64,
                        Material.REDSTONE_BLOCK, 2
                ),
                ENCHANTING_T2
        ));

        add(node(
                "enchanting3",
                Msg.color("&7[" + Colors.GOLD + Symbols.ENCHANTING + "&7] §d§lSorcerer"),
                SkillTreeSlots.ENCH_T3_ICON,
                SkillTreeSlots.ENCHANTING_T3,
                75,
                new String[]{
                        "§7Maximum-level enchanted books,",
                        "§7XP bottles, lapis and the lectern."
                },
                costs(
                        Material.ENCHANTED_BOOK, 32,
                        Material.LAPIS_BLOCK, 8
                ),
                ENCHANTING_T3
        ));

        add(node(
                "brewing",
                Msg.color("&7[" + Colors.GOLD + Symbols.ALCHEMY + "&7] §d§lAlchemist"),
                SkillTreeSlots.ENCH_BREWING_ICON,
                SkillTreeSlots.BREWING,
                15,
                new String[]{
                        "§7Everything usable in a brewing stand,",
                        "§7including otherwise unobtainable mob drops."
                },
                costs(
                        Material.BLAZE_ROD, 8,
                        Material.GLASS_BOTTLE, 8
                ),
                BREWING
        ));
    }

    private void buildBiomes() {

        addBiome(
                "oak",
                "§a§lOak",
                SkillTreeSlots.BIOME_OAK_ICON,
                FOREST,
                "oak",
                0,
                SkillTreeSlots.BIOME_OAK
        );

        addBiome(
                "birch",
                "§f§lBirch",
                SkillTreeSlots.BIOME_BIRCH_ICON,
                FOREST,
                "birch",
                10,
                SkillTreeSlots.BIOME_BIRCH
        );

        addBiome(
                "dark_oak",
                "§8§lDark Oak",
                SkillTreeSlots.BIOME_DARK_OAK_ICON,
                FOREST,
                "dark_oak",
                25,
                SkillTreeSlots.BIOME_DARK_OAK
        );

        addBiome(
                "spruce",
                "§7§lSpruce",
                SkillTreeSlots.BIOME_SPRUCE_ICON,
                FOREST,
                "spruce",
                50,
                SkillTreeSlots.BIOME_SPRUCE
        );

        addBiome(
                "jungle",
                "§2§lJungle",
                SkillTreeSlots.BIOME_JUNGLE_ICON,
                FOREST,
                "jungle",
                75,
                SkillTreeSlots.BIOME_JUNGLE
        );

        addBiome(
                "acacia",
                "§6§lAcacia",
                SkillTreeSlots.BIOME_ACACIA_ICON,
                FOREST,
                "acacia",
                100,
                SkillTreeSlots.BIOME_ACACIA
        );

        addBiome(
                "mangrove",
                "§2§lMangrove",
                SkillTreeSlots.BIOME_MANGROVE_ICON,
                FOREST,
                "mangrove",
                150,
                SkillTreeSlots.BIOME_MANGROVE
        );

        addBiome(
                "cherry",
                "§d§lCherry",
                SkillTreeSlots.BIOME_CHERRY_ICON,
                FOREST,
                "cherry",
                200,
                SkillTreeSlots.BIOME_CHERRY
        );

        addBiome(
                "crimson",
                "§c§lCrimson",
                SkillTreeSlots.BIOME_CRIMSON_ICON,
                NETHER,
                "crimson",
                300,
                SkillTreeSlots.BIOME_CRIMSON
        );

        addBiome(
                "warped",
                "§b§lWarped",
                SkillTreeSlots.BIOME_WARPED_ICON,
                NETHER,
                "warped",
                300,
                SkillTreeSlots.BIOME_WARPED
        );

        addBiome(
                "basalt",
                "§8§lBasalt",
                SkillTreeSlots.BIOME_BASALT_ICON,
                NETHER,
                "basalt",
                1000,
                SkillTreeSlots.BIOME_BASALT
        );
    }

    private void addBiome(String id, String name, Material icon, BiomeNode.BiomeType type,
                          String family, int tokenCost, int slot) {
        List<Material> drops = new ArrayList<>(items.woodFamily(family));

        // Forest and Nether wood selections contain only their complete wood
        // family. Basalt has no wood family, so give that special selection its
        // matching basalt/blackstone building family instead.
        if (family.equals("basalt")) {
            addIfAllowed(drops, Material.BASALT, Material.POLISHED_BASALT, Material.BLACKSTONE,
                    Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS,
                    Material.GILDED_BLACKSTONE, Material.MAGMA_BLOCK);
        }

        addBiome(new BiomeNode(id, name, icon, type, drops,
                configuredTokens("biomes." + id, tokenCost),
                configuredItems("biomes." + id, biomeCosts(id)), slot));
    }

    /**
     * Reads an upgrade price from skilltree-prices.yml. The values passed by
     * the tree are used as safe defaults if the entry is missing or invalid.
     */
    private int configuredTokens(String path, int fallback) {
        int value = prices.getInt("nodes." + path + ".tokens", fallback);
        return Math.max(0, value);
    }

    private List<SkillNode.ItemCost> configuredItems(String path, List<SkillNode.ItemCost> fallback) {
        ConfigurationSection section = prices.getConfigurationSection("nodes." + path + ".items");
        if (section == null) return fallback;

        List<SkillNode.ItemCost> result = new ArrayList<>();
        for (String rawMaterial : section.getKeys(false)) {
            try {
                Material material = Material.valueOf(rawMaterial.toUpperCase(Locale.ROOT));
                int amount = section.getInt(rawMaterial, 0);
                if (amount > 0) result.add(new SkillNode.ItemCost(material, amount));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid materials so one typo cannot stop the plugin loading.
            }
        }
        return result;
    }

    private List<SkillNode.ItemCost> biomeCosts(String id) {
        return switch (id) {
            case "oak" -> List.of();
            case "birch" -> costs(Material.OAK_PLANKS, 8, Material.CRAFTING_TABLE, 1);
            case "dark_oak" -> costs(Material.BIRCH_PLANKS, 8, Material.CRAFTING_TABLE, 1);
            case "spruce" -> costs(Material.DARK_OAK_PLANKS, 8, Material.CRAFTING_TABLE, 1);
            case "jungle" -> costs(Material.SPRUCE_PLANKS, 8, Material.CRAFTING_TABLE, 1);
            case "acacia" -> costs(Material.JUNGLE_PLANKS, 8, Material.CRAFTING_TABLE, 1);
            case "mangrove" -> costs(Material.ACACIA_PLANKS, 8, Material.MUD, 1, Material.CRAFTING_TABLE, 1);
            case "cherry" -> costs(Material.MANGROVE_PLANKS, 8, Material.CRAFTING_TABLE, 1, Material.EMERALD, 1);
            case "crimson" -> costs(Material.CHERRY_PLANKS, 8, Material.CRAFTING_TABLE, 1, Material.NETHER_BRICKS, 4);
            case "warped" -> costs(Material.CRIMSON_PLANKS, 8, Material.CRAFTING_TABLE, 1, Material.CRIMSON_STEM, 2);
            case "basalt" -> costs(Material.BLACKSTONE, 32);
            default -> List.of();
        };
    }

    private void addIfAllowed(List<Material> list, Material... materials) {
        for (Material material : materials) if (items.isAllowed(material) && !list.contains(material)) list.add(material);
    }

    private void addBiome(BiomeNode node) { biomeNodes.put(node.getId(), node); }

    /** Builds a weighted pool from the node's groups, de-duplicating overlap. */
    public List<SkillNode.LootEntry> getLootPool(String nodeId) {
        SkillNode node = nodes.get(nodeId);
        if (node == null) return List.of();

        LinkedHashSet<Material> materials = new LinkedHashSet<>();
        for (SkillItemRegistry.Group group : node.getGroups()) materials.addAll(items.get(group));

        List<SkillNode.LootEntry> result = new ArrayList<>();
        for (Material material : materials) {
            result.add(new SkillNode.LootEntry(material, rarityFor(material, node)));
        }
        return result;
    }

    private SkillNode.Rarity rarityFor(Material material, SkillNode node) {
        String name = material.name();
        if (name.contains("DIAMOND") || name.contains("NETHERITE") || name.contains("ELYTRA")
                || name.contains("SHULKER") || name.contains("ANCIENT_DEBRIS") || name.contains("WITHER_SKELETON")
                || name.contains("DRAGON") || name.contains("TOTEM") || name.contains("HEART_OF_THE_SEA")
                || name.contains("NETHER_STAR") || name.contains("BEACON") || name.contains("CONDUIT")) return RARE;
        if (node.getId().endsWith("2") || node.getId().endsWith("3")) return UNCOMMON;
        return COMMON;
    }

    public Map<String, SkillNode> getNodes() { return Collections.unmodifiableMap(nodes); }
    public Map<String, BiomeNode> getBiomeNodes() { return Collections.unmodifiableMap(biomeNodes); }
    public SkillNode getNode(String id) { return nodes.get(id); }
    public BiomeNode getBiomeNode(String id) { return biomeNodes.get(id); }
}
