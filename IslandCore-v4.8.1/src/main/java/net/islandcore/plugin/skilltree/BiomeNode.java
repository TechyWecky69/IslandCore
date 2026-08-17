package net.islandcore.plugin.skilltree;

import org.bukkit.Material;
import java.util.List;

/**
 * A biome unlock on the Biome tab.
 * Unlocking a biome adds its wood/biome-specific items into the
 * relevant skill node's effective loot pool when that biome is selected.
 */
public class BiomeNode {

    public enum BiomeType { FOREST, NETHER }

    private final String id;
    private final String displayName;
    private final Material icon;
    private final BiomeType biomeType;
    private final List<Material> drops;
    private final int tokenCost;
    private final List<SkillNode.ItemCost> itemCosts;
    private final int guiSlot;

    public BiomeNode(String id, String displayName, Material icon, BiomeType biomeType,
                     List<Material> drops, int tokenCost, List<SkillNode.ItemCost> itemCosts, int guiSlot) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.biomeType = biomeType;
        this.drops = drops;
        this.tokenCost = tokenCost;
        this.itemCosts = itemCosts;
        this.guiSlot = guiSlot;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }
    public BiomeType getBiomeType() { return biomeType; }
    public List<Material> getDrops() { return drops; }
    public int getTokenCost() { return tokenCost; }
    public List<SkillNode.ItemCost> getItemCosts() { return itemCosts; }
    public int getGuiSlot() { return guiSlot; }
}
