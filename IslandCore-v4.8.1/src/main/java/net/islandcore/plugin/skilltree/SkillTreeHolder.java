package net.islandcore.plugin.skilltree;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Identifies a SkillTree GUI page without changing the vanilla chest title. */
public final class SkillTreeHolder implements InventoryHolder {
    public enum Page { MAIN, BIOME, ENCHANTING }

    private final Page page;

    public SkillTreeHolder(Page page) { this.page = page; }

    public Page getPage() { return page; }

    @Override
    public Inventory getInventory() { return null; }
}
