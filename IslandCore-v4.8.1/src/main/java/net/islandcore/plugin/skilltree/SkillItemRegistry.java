package net.islandcore.plugin.skilltree;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Single source of truth for the lootable Minecraft materials used by the
 * IslandCore skill tree.
 *
 * Every real Bukkit item is allowed unless it appears in
 * loot-prohibited-items in config.yml. Groups are deliberately independent:
 * a material may belong to several groups (for example lapis is useful for
 * both Mining T2 and Enchanting T1, and obsidian is useful for Mining T3,
 * Nether T1 and End T1).
 */
public final class SkillItemRegistry {

    public enum Group {
        START,
        MINING_T1, MINING_T2, MINING_T3,
        FARMING_T1, FARMING_T2, FARMING_T3,
        FISHING,
        NETHER_T1, NETHER_T2,
        END_T1, END_T2,
        BIOME,
        ENCHANTING_T1, ENCHANTING_T2, ENCHANTING_T3,
        MAGIC_T1, MAGIC_T2, MAGIC_T3, ALCHEMY, BREWING,
        COLOUR,
        FLOWERS_T1, FLOWERS_T2
    }

    private final EnumSet<Material> prohibited = EnumSet.noneOf(Material.class);
    private final EnumSet<Material> allowed = EnumSet.noneOf(Material.class);
    private final EnumMap<Group, LinkedHashSet<Material>> groups = new EnumMap<>(Group.class);

    public SkillItemRegistry(JavaPlugin plugin) {
        for (Group group : Group.values()) groups.put(group, new LinkedHashSet<>());

        FileConfiguration config = plugin.getConfig();
        for (String raw : config.getStringList("loot-prohibited-items")) {
            try {
                prohibited.add(Material.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Unknown loot-prohibited-items material: " + raw);
            }
        }

        for (Material material : Material.values()) {
            // LootTask gives ItemStacks, so only actual inventory items belong
            // in this registry. This also naturally excludes AIR and fluids.
            if (!material.isItem()) continue;
            if (prohibited.contains(material)) continue;
            allowed.add(material);
        }

        buildGroups();
        validate(plugin);
    }

    public Set<Material> getAllowedItems() {
        return Collections.unmodifiableSet(allowed);
    }

    public Set<Material> get(Group group) {
        return Collections.unmodifiableSet(groups.get(group));
    }

    public List<Material> getList(Group group) {
        return List.copyOf(groups.get(group));
    }

    public boolean isAllowed(Material material) {
        return allowed.contains(material);
    }

    /** Returns the groups to which a material belongs. */
    public Set<Group> groupsFor(Material material) {
        EnumSet<Group> result = EnumSet.noneOf(Group.class);
        for (Group group : Group.values()) {
            if (groups.get(group).contains(material)) result.add(group);
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Returns every item in a Minecraft wood family. This intentionally uses
     * the material names rather than a hand-written list, so new 1.21.x wood
     * items such as hanging signs, boats and chest boats are included.
     */
    public List<Material> woodFamily(String wood) {
        String upper = wood.toUpperCase(Locale.ROOT);
        List<Material> result = new ArrayList<>();

        for (Material material : allowed) {
            String name = material.name();
            boolean family = name.startsWith(upper + "_")
                    || name.startsWith("STRIPPED_" + upper + "_");
            if (upper.equals("BAMBOO")) {
                family |= name.startsWith("BAMBOO_");
            }
            if (family) result.add(material);
        }

        result.sort(Comparator.comparing(Enum::name));
        return List.copyOf(result);
    }

    private void add(Group group, Material... materials) {
        for (Material material : materials) if (allowed.contains(material)) groups.get(group).add(material);
    }

    private void addByName(Group group, String... names) {
        for (String name : names) {
            try {
                Material material = Material.valueOf(name);
                if (allowed.contains(material)) groups.get(group).add(material);
            } catch (IllegalArgumentException ignored) {
                // Version-safe: an item can be absent from a future/older API.
            }
        }
    }

    private void addPrefix(Group group, String... prefixes) {
        for (Material material : allowed) {
            String name = material.name();
            for (String prefix : prefixes) {
                if (name.startsWith(prefix)) {
                    groups.get(group).add(material);
                    break;
                }
            }
        }
    }

    /**
     * Same as {@link #addPrefix}, but matches on the end of the material
     * name instead of the start. Useful for the dyed-block families (for
     * example {@code LIGHT_BLUE_CONCRETE}, {@code RED_WOOL}), where the
     * colour name is the prefix and the item type is the shared suffix.
     */
    private void addSuffix(Group group, String... suffixes) {
        for (Material material : allowed) {
            String name = material.name();
            for (String suffix : suffixes) {
                if (name.endsWith(suffix)) {
                    groups.get(group).add(material);
                    break;
                }
            }
        }
    }

    private void buildGroups() {
        /* ---------------------------------------------------------------
         * START — the actual early-game pool. Other groups may overlap it.
         * --------------------------------------------------------------- */
        addByName(Group.START,
                "OAK_LOG", "OAK_PLANKS", "STICK", "COBBLESTONE", "STONE", "DIRT", "SAND",
                "GRAVEL", "OAK_SAPLING", "WHEAT_SEEDS", "APPLE", "FLINT", "CHARCOAL",
                "CRAFTING_TABLE", "FURNACE", "CHEST", "TORCH", "GLASS", "LADDER",
                "OAK_DOOR", "OAK_FENCE", "OAK_FENCE_GATE", "BOWL", "STRING", "BONE",
                "ROTTEN_FLESH", "GUNPOWDER", "SPIDER_EYE", "ARROW", "WOODEN_SWORD",
                "WOODEN_AXE", "WOODEN_PICKAXE", "WOODEN_SHOVEL", "WOODEN_HOE",
                "STONE_SWORD", "STONE_AXE", "STONE_SHOVEL", "STONE_HOE",
                "LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS");

        /* ---------------------------------------------------------------
         * MINING
         * --------------------------------------------------------------- */
        addPrefix(Group.MINING_T1,
                "COBBLESTONE", "STONE", "ANDESITE", "DIORITE", "GRANITE", "COAL", "COPPER",
                "TUFF", "CALCITE", "DRIPSTONE", "FLINT", "GRAVEL", "SAND", "SANDSTONE");
        addByName(Group.MINING_T1,
                "STONE_PICKAXE", "STONE_SWORD", "STONE_AXE", "STONE_SHOVEL", "STONE_HOE",
                "COAL", "RAW_COPPER", "COPPER_INGOT", "FLINT", "TORCH", "AMETHYST_SHARD", "CLAY_BALL");

        addPrefix(Group.MINING_T2,
                "IRON_", "GOLD", "REDSTONE", "LAPIS", "DEEPSLATE", "BLAST_FURNACE");
        addByName(Group.MINING_T2,
                "RAW_IRON", "IRON_INGOT", "IRON_ORE", "RAW_GOLD", "GOLD_INGOT", "GOLD_ORE",
                "REDSTONE", "REDSTONE_BLOCK", "LAPIS_LAZULI", "LAPIS_BLOCK", "BLAST_FURNACE",
                "DEEPSLATE", "COBBLED_DEEPSLATE", "CHISELED_DEEPSLATE", "IRON_PICKAXE",
                "IRON_SWORD", "IRON_AXE", "IRON_SHOVEL", "IRON_HOE", "IRON_HELMET",
                "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS");

        addPrefix(Group.MINING_T3,
                "DIAMOND", "DEEPSLATE", "AMETHYST", "LAVA", "OBSIDIAN");
        addByName(Group.MINING_T3,
                "DIAMOND", "DIAMOND_ORE", "DEEPSLATE_DIAMOND_ORE", "DEEPSLATE_LAVA_BUCKET",
                "LAVA_BUCKET", "AMETHYST_SHARD", "AMETHYST_BLOCK", "OBSIDIAN",
                "DIAMOND_PICKAXE", "DIAMOND_SWORD", "DIAMOND_AXE", "DIAMOND_SHOVEL",
                "DIAMOND_HOE", "DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS",
                "DIAMOND_BOOTS");

        /* ---------------------------------------------------------------
         * FARMING — crops, food, animals, natural drops and husbandry.
         * --------------------------------------------------------------- */
        addPrefix(Group.FARMING_T1,
                "WHEAT", "CARROT", "POTATO", "BEETROOT", "SEED", "BONE_MEAL",
                "DIRT", "FARMLAND", "COMPOSTER", "HAY_BLOCK", "GRASS", "FERN",
                "DANDELION", "POPPY", "AZURE_BLUET", "OXEYE_DAISY", "CORNFLOWER",
                "LILY_OF_THE_VALLEY", "SUNFLOWER", "PUMPKIN", "MELON", "SUGAR_CANE",
                "CACTUS", "KELP", "SEAGRASS", "BAMBOO", "VINE", "MOSS", "SPORE_BLOSSOM",
                "DRIPLEAF", "TORCHFLOWER", "PITCHER");
        addByName(Group.FARMING_T1,
                "WHEAT", "WHEAT_SEEDS", "CARROT", "POTATO", "BEETROOT", "BEETROOT_SEEDS",
                "BREAD", "APPLE", "SWEET_BERRIES", "GLOW_BERRIES", "COCOA_BEANS", "BONE_MEAL",
                "SHEARS", "BUCKET", "WATER_BUCKET", "MILK_BUCKET", "LEAD", "NAME_TAG", "COMPASS");

        addPrefix(Group.FARMING_T2,
                "PUMPKIN", "MELON", "COCOA", "SUGAR_CANE", "CACTUS", "BAMBOO", "MOSS",
                "FLOWER", "TULIP", "ORCHID", "LILY", "AZURE", "ALLIUM", "PEONY", "ROSE",
                "BERR", "HONEY", "BEE", "HAY", "COMPOSTER");
        addByName(Group.FARMING_T2,
                "GOLDEN_APPLE", "GOLDEN_CARROT", "GLISTERING_MELON_SLICE", "PUMPKIN_PIE",
                "CAKE", "COOKIE", "SUSPICIOUS_STEW", "MUSHROOM_STEW", "RABBIT_STEW",
                "HONEY_BOTTLE", "HONEYCOMB", "BEEHIVE", "BEE_NEST", "SHEARS");

        addPrefix(Group.FARMING_T3,
                "COOKED_", "RAW_", "MEAT", "BEEF", "PORK", "CHICKEN", "MUTTON", "RABBIT",
                "LEATHER", "FEATHER", "EGG", "WOOL", "MUTTON", "INK_SAC");
        addByName(Group.FARMING_T3,
                "BEEF", "COOKED_BEEF", "PORKCHOP", "COOKED_PORKCHOP", "CHICKEN", "COOKED_CHICKEN",
                "MUTTON", "COOKED_MUTTON", "RABBIT", "COOKED_RABBIT", "COD", "COOKED_COD",
                "SALMON", "COOKED_SALMON", "TROPICAL_FISH", "PUFFERFISH", "LEATHER", "FEATHER",
                "RABBIT_HIDE", "RABBIT_FOOT", "EGG", "MILK_BUCKET", "ENCHANTED_GOLDEN_APPLE", "GLOW_BERRIES");
        addPrefix(Group.FARMING_T3, "SPAWN_EGG", "DYE");

        /* Fishing gets everything that is sensible to fish, plus rod/bobber
         * related loot. Some of these also intentionally live elsewhere. */
        addByName(Group.FISHING,
                "FISHING_ROD", "COD", "SALMON", "TROPICAL_FISH", "PUFFERFISH", "BOW",
                "ENCHANTED_BOOK", "NAME_TAG", "NAUTILUS_SHELL", "HEART_OF_THE_SEA",
                "LILY_PAD", "BOWL", "LEATHER", "LEATHER_BOOTS", "ROTTEN_FLESH", "BONE",
                "STRING", "INK_SAC", "TRIPWIRE_HOOK", "SADDLE");

        /* ---------------------------------------------------------------
         * NETHER / END
         * --------------------------------------------------------------- */
        addPrefix(Group.NETHER_T1,
                "NETHERRACK", "NETHER_QUARTZ", "NETHER_GOLD", "SOUL_", "BASALT",
                "BLACKSTONE", "GLOWSTONE", "NETHER_BRICK", "CRIMSON", "WARPED",
                "SHROOMLIGHT", "MAGMA", "NETHER_WART", "GRAVEL");
        addByName(Group.NETHER_T1,
                "NETHER_QUARTZ_ORE", "NETHER_GOLD_ORE", "NETHERRACK", "SOUL_SAND", "SOUL_SOIL",
                "BASALT", "BLACKSTONE", "GLOWSTONE", "MAGMA_BLOCK", "NETHER_WART",
                "NETHER_BRICKS", "NETHER_BRICK", "NETHER_BRICK_SLAB", "NETHER_BRICK_STAIRS",
                "NETHER_BRICK_FENCE", "NETHER_BRICK_WALL", "CRIMSON_STEM", "WARPED_STEM",
                "CRIMSON_PLANKS", "WARPED_PLANKS", "CRIMSON_FUNGUS", "WARPED_FUNGUS", "LAVA_BUCKET");

        addPrefix(Group.NETHER_T2,
                "BLAZE", "GHAST", "WITHER", "PIGLIN", "HOGLIN", "ZOMBIFIED_PIGLIN",
                "NETHERITE", "ANCIENT_DEBRIS", "RESPAWN_ANCHOR", "LODESTONE", "GILDED_BLACKSTONE",
                "NETHER_STAR", "CRYING_OBSIDIAN", "NETHER_BRICK", "QUARTZ");
        addByName(Group.NETHER_T2,
                "BLAZE_ROD", "BLAZE_POWDER", "GHAST_TEAR", "WITHER_SKELETON_SKULL", "NETHER_STAR",
                "PIGLIN_BANNER_PATTERN", "SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE", "NETHERITE_SCRAP",
                "NETHERITE_INGOT", "NETHERITE_PICKAXE", "NETHERITE_AXE", "NETHERITE_SHOVEL",
                "NETHERITE_HOE", "NETHERITE_SWORD", "NETHERITE_HELMET", "NETHERITE_CHESTPLATE",
                "NETHERITE_LEGGINGS", "NETHERITE_BOOTS", "ANCIENT_DEBRIS", "RESPAWN_ANCHOR",
                "LODESTONE", "CRYING_OBSIDIAN", "OBSIDIAN", "MAGMA_CREAM", "LAVA_BUCKET");

        addByName(Group.END_T1,
                "END_STONE", "END_STONE_BRICKS", "OBSIDIAN", "CRYING_OBSIDIAN", "CHORUS_FRUIT",
                "CHORUS_FLOWER", "CHORUS_PLANT", "END_ROD", "ENDER_PEARL", "ENDER_EYE",
                "PURPUR_BLOCK", "PURPUR_PILLAR", "PURPUR_SLAB", "PURPUR_STAIRS", "END_CRYSTAL");
        addByName(Group.END_T2,
                "ELYTRA", "SHULKER_SHELL", "SHULKER_BOX", "DRAGON_BREATH", "DRAGON_EGG",
                "END_CITY", "DIAMOND", "EMERALD", "ENDER_CHEST", "END_ROD", "PURPUR_BLOCK",
                "PURPUR_PILLAR", "PURPUR_SLAB", "PURPUR_STAIRS", "CHORUS_FRUIT", "END_CRYSTAL");

        /* ---------------------------------------------------------------
         * ENCHANTING / BREWING
         * --------------------------------------------------------------- */
        addByName(Group.ENCHANTING_T1,
                "ENCHANTING_TABLE", "BOOK", "BOOKSHELF", "WRITABLE_BOOK", "WRITTEN_BOOK", "PAPER",
                "LAPIS_LAZULI", "EXPERIENCE_BOTTLE", "GRINDSTONE", "ANVIL", "FEATHER", "INK_SAC",
                "LECTERN", "QUILL", "ENCHANTED_BOOK");
        addByName(Group.ENCHANTING_T2, "ENCHANTED_BOOK", "EXPERIENCE_BOTTLE", "LAPIS_LAZULI", "ANVIL", "GRINDSTONE");
        addByName(Group.ENCHANTING_T3,
                "ENCHANTED_BOOK", "EXPERIENCE_BOTTLE", "LAPIS_LAZULI", "LECTERN", "ANVIL",
                "ENCHANTING_TABLE", "MENDING", "NETHERITE_UPGRADE_SMITHING_TEMPLATE");

        addByName(Group.BREWING,
                "BREWING_STAND", "GLASS_BOTTLE", "CAULDRON", "BLAZE_POWDER", "BLAZE_ROD",
                "NETHER_WART", "FERMENTED_SPIDER_EYE", "GLISTERING_MELON_SLICE", "MAGMA_CREAM",
                "GHAST_TEAR", "GOLDEN_CARROT", "RABBIT_FOOT", "PUFFERFISH", "SUGAR",
                "REDSTONE", "GLOWSTONE_DUST", "GUNPOWDER", "DRAGON_BREATH", "SPIDER_EYE",
                "PHANTOM_MEMBRANE", "TURTLE_HELMET", "TURTLE_SCUTE", "LEATHER", "BONE",
                "ROTTEN_FLESH", "STRING", "SLIME_BALL", "ENDER_PEARL", "WITHER_SKELETON_SKULL");

        /* ---------------------------------------------------------------
         * COLOUR — dyed/decorative building blocks: concrete, concrete
         * powder, terracotta, glazed terracotta, wool and other colourful
         * decoration blocks. Suffix-matched so every dye colour variant is
         * picked up automatically.
         * --------------------------------------------------------------- */
        addSuffix(Group.COLOUR,
                "_CONCRETE", "_CONCRETE_POWDER", "_TERRACOTTA", "_GLAZED_TERRACOTTA",
                "_WOOL", "_STAINED_GLASS", "_STAINED_GLASS_PANE", "_CARPET",
                "_BANNER", "_BED", "_CANDLE", "_SHULKER_BOX");
        addByName(Group.COLOUR, "TERRACOTTA", "CANDLE", "SHULKER_BOX", "CLAY_BALL");

        /* ---------------------------------------------------------------
         * Cross-category item families. These are intentionally allowed to
         * overlap the source-material groups above so tools, armour, weapons,
         * templates, maps, discs, etc. are never lost just because their name
         * does not look like a block/material.
         * --------------------------------------------------------------- */
        addPrefix(Group.ENCHANTING_T2,
                "WOODEN_", "STONE_", "IRON_", "GOLDEN_", "DIAMOND_", "NETHERITE_",
                "LEATHER_", "CHAINMAIL_", "TURTLE_", "BOW", "CROSSBOW", "TRIDENT", "SHIELD");
        addByName(Group.ENCHANTING_T2,
                "BOW", "CROSSBOW", "TRIDENT", "SHIELD", "ELYTRA",
                "MUSIC_DISC_13", "MUSIC_DISC_CAT", "MUSIC_DISC_BLOCKS", "MUSIC_DISC_CHIRP",
                "MUSIC_DISC_FAR", "MUSIC_DISC_MALL", "MUSIC_DISC_MELLOHI", "MUSIC_DISC_STAL",
                "MUSIC_DISC_STRAD", "MUSIC_DISC_WARD", "MUSIC_DISC_11", "MUSIC_DISC_WAIT",
                "MUSIC_DISC_OTHERSIDE", "MUSIC_DISC_PIGSTEP", "MUSIC_DISC_5", "MUSIC_DISC_RELIC",
                "ARMOR_STAND", "NAME_TAG", "SADDLE", "LEAD", "COMPASS", "CLOCK", "RECOVERY_COMPASS",
                "SPYGLASS", "GOAT_HORN", "TOTEM_OF_UNDYING", "FIREWORK_ROCKET", "FIREWORK_STAR");
        addPrefix(Group.ENCHANTING_T3, "SMITHING_TEMPLATE", "ARMOR_TRIM", "MUSIC_DISC_");

        addPrefix(Group.BREWING, "POTION", "SPLASH_POTION", "LINGERING_POTION");
        /*
         * BIOME is intentionally empty here.
         * Its loot is resolved dynamically by SkillTreeManager from the
         * player's selected biome. This prevents one biome from leaking
         * another wood family into the pool.
         */

        /* ---------------------------------------------------------------
         * FLOWERS — common flowers (T1) and rarer flowers / flower-adjacent
         * decorations (T2, "Bouquet").
         * --------------------------------------------------------------- */
        addByName(Group.FLOWERS_T1,
                "DANDELION", "POPPY", "ALLIUM", "AZURE_BLUET", "RED_TULIP", "ORANGE_TULIP",
                "WHITE_TULIP", "PINK_TULIP", "OXEYE_DAISY", "CORNFLOWER", "SUNFLOWER",
                "LILY_OF_THE_VALLEY", "BLUE_ORCHID");

        addByName(Group.FLOWERS_T2,
                "CHERRY_LEAVES", "FLOWERING_AZALEA_LEAVES", "MANGROVE_PROPAGULE",
                "FLOWERING_AZALEA", "TORCHFLOWER", "WITHER_ROSE", "PINK_PETALS",
                "SPORE_BLOSSOM", "LILAC", "ROSE_BUSH", "PEONY", "PITCHER_PLANT",
                "GLOW_BERRIES");
    }

    private void validate(JavaPlugin plugin) {
        EnumSet<Material> grouped = EnumSet.noneOf(Material.class);
        for (Set<Material> set : groups.values()) grouped.addAll(set);

        EnumSet<Material> ungrouped = EnumSet.copyOf(allowed);
        ungrouped.removeAll(grouped);

        plugin.getLogger().info("Skill item registry: " + allowed.size() + " allowed item materials, "
                + prohibited.size() + " prohibited, " + ungrouped.size() + " currently ungrouped.");

        if (!ungrouped.isEmpty()) {
            plugin.getLogger().warning("Ungrouped allowed materials: " + ungrouped);
            // Do not add these to START. START is deliberately restricted to
            // early-game materials; later/uncategorised items can be added to
            // the appropriate skill group explicitly.
        }
    }
}