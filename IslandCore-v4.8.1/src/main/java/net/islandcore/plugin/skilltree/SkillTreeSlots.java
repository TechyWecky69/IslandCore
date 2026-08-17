package net.islandcore.plugin.skilltree;

import org.bukkit.Material;

/**
 * ALL skill-tree GUI slot positions live here.
 *
 * Slot numbers are standard Bukkit inventory slots:
 *
 * 0   1   2   3   4   5   6   7   8
 * 9   10  11  12  13  14  15  16  17
 * 18  19  20  21  22  23  24  25  26
 * 27  28  29  30  31  32  33  34  35
 * 36  37  38  39  40  41  42  43  44
 * 45  46  47  48  49  50  51  52  53
 *
 * Change a number here and both the GUI and click handling will follow it.
 */
public final class SkillTreeSlots {

    private SkillTreeSlots() {}

    /* =========================================================
       INVENTORY SIZES
       ========================================================= */

    public static final int MAIN_SIZE = 54;
    public static final int BIOME_SIZE = 54;
    public static final int ENCHANTING_SIZE = 27;


    /* =========================================================
       MAIN TREE
       ========================================================= */

    public static int MAIN_BACK = 0;
    public static int MAIN_TOKENS = 4;
    public static int MAIN_BIOME_BUTTON = 8;

    // Enchanting
    public static int MAIN_ENCHANTING_BUTTON = 50;

    // Mining
    public static int MAIN_MINING_T1 = 38;
    public static int MAIN_MINING_T2 = 28;
    public static int MAIN_MINING_T3 = 19;

    // End
    public static int MAIN_END_T1 = 21;
    public static int MAIN_END_T2 = 11;

    // Nether
    public static int MAIN_NETHER_T1 = 23;
    public static int MAIN_NETHER_T2 = 15;

    // Farming
    public static int MAIN_FARMING_T1 = 42;
    public static int MAIN_FARMING_T2 = 34;
    public static int MAIN_FARMING_T3 = 25;

    // Fishing
    public static int MAIN_FISHING = 48;

    // Flowers
    public static int MAIN_FLOWERS_T1 = 22; //
    public static int MAIN_FLOWERS_T2 = 13; //

    // Item used to represent each category's node icon in the GUI.
    // Change these to swap the icon without touching SkillTree.java.

    // Biome
    public static int MAIN_BIOME = 31;

    public static int MAIN_COLOUR = 40;

    // Start
    public static int MAIN_START = 49;

    // Admin/Owner only — unlocks the entire skill tree instantly.
    // Set to -1 to disable (button will not appear). Change to any valid
    // inventory slot (0-53) to move it.
    public static int MAIN_ADMIN_UNLOCK = 45;


    /* =========================================================
       ENCHANTING / BREWING
       ========================================================= */

    public static int ENCHANT_TOKENS = 4;
    public static int ENCHANT_BACK = 9;
    // Glowing indicator in the enchanting page so the player can see which tab is open
    public static int ENCHANT_TAB_INDICATOR = 0;

    public static int ENCHANTING_T1 = 11;
    public static int ENCHANTING_T2 = 12;
    public static int ENCHANTING_T3 = 13;
    public static int BREWING = 22;


    /* =========================================================
       BIOME PAGE
       ========================================================= */

    public static int BIOME_BACK = 0;
    public static int BIOME_TOKENS = 4;
    public static int BIOME_CURRENT = 8;

    public static int BIOME_OAK = 11;
    public static int BIOME_BIRCH = 20;
    public static int BIOME_DARK_OAK = 29;
    public static int BIOME_SPRUCE = 38;
    public static int BIOME_JUNGLE = 39;
    public static int BIOME_ACACIA = 40;
    public static int BIOME_MANGROVE = 31;
    public static int BIOME_CHERRY = 22;
    public static int BIOME_CRIMSON = 13;
    public static int BIOME_WARPED = 14;
    public static int BIOME_BASALT = 15;

    /* =========================================================
    SLOT ICONS
    ========================================================= */

    public static Material MAIN_BACK_ICON = Material.CRAFTING_TABLE;
    public static Material MAIN_TOKENS_ICON = Material.SUNFLOWER;
    public static Material MAIN_BIOME_BUTTON_ICON = Material.OAK_SAPLING;

    public static Material MAIN_ADMIN_UNLOCK_ICON = Material.NETHER_STAR;

    public static Material MAIN_START_ICON = Material.GRASS_BLOCK;
    public static Material MAIN_ENCHANTING_ICON = Material.ENCHANTING_TABLE;
    public static Material MAIN_FISHING_ICON = Material.FISHING_ROD;

    public static Material MAIN_COLOR_ICON = Material.ORANGE_CONCRETE;
    public static Material MAIN_BIOME_ICON = Material.OAK_WOOD;

    public static Material MAIN_END_T1_ICON = Material.END_STONE;
    public static Material MAIN_END_T2_ICON = Material.DRAGON_HEAD;

    public static Material MAIN_NETHER_T1_ICON = Material.NETHERRACK;
    public static Material MAIN_NETHER_T2_ICON = Material.ANCIENT_DEBRIS;

    public static Material MAIN_FLOWERS_T1_ICON = Material.POPPY;
    public static Material MAIN_FLOWERS_T2_ICON = Material.PITCHER_PLANT;

    public static Material MAIN_MINING_T1_ICON = Material.WOODEN_PICKAXE;
    public static Material MAIN_MINING_T2_ICON = Material.IRON_PICKAXE;
    public static Material MAIN_MINING_T3_ICON = Material.NETHERITE_PICKAXE;

    public static Material MAIN_FARMING_T1_ICON = Material.WOODEN_HOE;
    public static Material MAIN_FARMING_T2_ICON = Material.IRON_HOE;
    public static Material MAIN_FARMING_T3_ICON = Material.NETHERITE_HOE;

    // Enchanting Page
    public static Material ENCH_T1_ICON = Material.ENCHANTING_TABLE;
    public static Material ENCH_T2_ICON = Material.BOOK;
    public static Material ENCH_T3_ICON = Material.ENCHANTED_BOOK;

    public static Material ENCH_BREWING_ICON = Material.BREWING_STAND;

    //Biome page
    public static Material BIOME_OAK_ICON = Material.OAK_WOOD;
    public static Material BIOME_BIRCH_ICON = Material.BIRCH_WOOD;
    public static Material BIOME_DARK_OAK_ICON = Material.DARK_OAK_WOOD;
    public static Material BIOME_SPRUCE_ICON = Material.SPRUCE_WOOD;
    public static Material BIOME_JUNGLE_ICON = Material.JUNGLE_WOOD;
    public static Material BIOME_ACACIA_ICON = Material.ACACIA_WOOD;
    public static Material BIOME_MANGROVE_ICON = Material.MANGROVE_WOOD;
    public static Material BIOME_CHERRY_ICON = Material.CHERRY_WOOD;
    public static Material BIOME_CRIMSON_ICON = Material.CRIMSON_HYPHAE;
    public static Material BIOME_WARPED_ICON = Material.WARPED_HYPHAE;
    public static Material BIOME_BASALT_ICON = Material.BASALT;

}