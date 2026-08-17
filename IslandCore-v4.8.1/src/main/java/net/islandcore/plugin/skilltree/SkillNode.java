package net.islandcore.plugin.skilltree;

import net.islandcore.plugin.util.Colors;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** One unlockable node in the visual IslandCore skill tree. */
public class SkillNode {

    public enum Rarity { COMMON, UNCOMMON, RARE }

    private final String id;
    private final String displayName;
    private final Material icon;
    private final String[] description;
    private final EnumSet<SkillItemRegistry.Group> groups;
    private final int tokenCost;
    private final List<ItemCost> itemCosts;
    private final int guiSlot;

    public SkillNode(String id, String displayName, Material icon, String[] description,
                     Set<SkillItemRegistry.Group> groups, int tokenCost,
                     List<ItemCost> itemCosts, int guiSlot) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.groups = groups.isEmpty()
                ? EnumSet.noneOf(SkillItemRegistry.Group.class)
                : EnumSet.copyOf(groups);
        this.tokenCost = tokenCost;
        this.itemCosts = itemCosts;
        this.guiSlot = guiSlot;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public String[] getDescription() { return description; }
    public Set<SkillItemRegistry.Group> getGroups() { return Set.copyOf(groups); }
    public int getTokenCost() { return tokenCost; }
    public List<ItemCost> getItemCosts() { return itemCosts; }
    public int getGuiSlot() { return guiSlot; }

    /**
     * Returns a colour-coded symbol string matching the tier colour used on
     * the skill tree GUI, e.g. {@code §6⛏} (gold mining).
     * Wrap the result in dark-grey brackets at the call site:
     * {@code "&7[" + node.getColoredSymbol() + "&7]"}
     */
    public String getColoredSymbol() {
        // Mining
        if (groups.contains(SkillItemRegistry.Group.MINING_T1))
            return Colors.BRONZE + Symbols.MINING;
        if (groups.contains(SkillItemRegistry.Group.MINING_T2))
            return Colors.SILVER + Symbols.MINING;
        if (groups.contains(SkillItemRegistry.Group.MINING_T3))
            return Colors.GOLD + Symbols.MINING;

        // Farming
        if (groups.contains(SkillItemRegistry.Group.FARMING_T1))
            return Colors.BRONZE + Symbols.FARM;
        if (groups.contains(SkillItemRegistry.Group.FARMING_T2))
            return Colors.SILVER + Symbols.FARM;
        if (groups.contains(SkillItemRegistry.Group.FARMING_T3))
            return Colors.GOLD + Symbols.FARM;

        // Nether
        if (groups.contains(SkillItemRegistry.Group.NETHER_T1))
            return Colors.SILVER + Symbols.NETHER;
        if (groups.contains(SkillItemRegistry.Group.NETHER_T2))
            return Colors.GOLD + Symbols.NETHER;

        // End
        if (groups.contains(SkillItemRegistry.Group.END_T1))
            return Colors.SILVER + Symbols.END;
        if (groups.contains(SkillItemRegistry.Group.END_T2))
            return Colors.GOLD + Symbols.END;

        // Enchanting
        if (groups.contains(SkillItemRegistry.Group.ENCHANTING_T1))
            return Colors.BRONZE + Symbols.ENCHANTING;
        if (groups.contains(SkillItemRegistry.Group.ENCHANTING_T2))
            return Colors.SILVER + Symbols.ENCHANTING;
        if (groups.contains(SkillItemRegistry.Group.ENCHANTING_T3))
            return Colors.GOLD + Symbols.ENCHANTING;

        // Brewing / Alchemy
        if (groups.contains(SkillItemRegistry.Group.BREWING))
            return Colors.GOLD + Symbols.ALCHEMY;

        // Fishing
        if (groups.contains(SkillItemRegistry.Group.FISHING))
            return Colors.GOLD + Symbols.FISHING;

        // Biome
        if (groups.contains(SkillItemRegistry.Group.BIOME))
            return Colors.GOLD + Symbols.BIOME;

        // Colour
        if (groups.contains(SkillItemRegistry.Group.COLOUR))
            return Colors.GOLD + Msg.color("&l") + Symbols.COLOUR;

        // Flowers
        if (groups.contains(SkillItemRegistry.Group.FLOWERS_T1))
            return Colors.SILVER + Msg.color("&l") + Symbols.FLOWER;
        if (groups.contains(SkillItemRegistry.Group.FLOWERS_T2))
            return Colors.GOLD + Msg.color("&l") + Symbols.FLOWER;

        // START / fallback
        return Symbols.SKILL_TREE;
    }

    public static class LootEntry {
        private final Material material;
        private final Rarity rarity;

        public LootEntry(Material material, Rarity rarity) {
            this.material = material;
            this.rarity = rarity;
        }

        public Material getMaterial() { return material; }
        public Rarity getRarity() { return rarity; }

        public int getWeight() {
            return switch (rarity) {
                case COMMON -> 60;
                case UNCOMMON -> 30;
                case RARE -> 10;
            };
        }
    }

    public static class ItemCost {
        private final Material material;
        private final int amount;

        public ItemCost(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
    }
}