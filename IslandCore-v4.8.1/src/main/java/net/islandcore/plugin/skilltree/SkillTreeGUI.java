package net.islandcore.plugin.skilltree;

import net.islandcore.plugin.ranks.Rank;
import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.util.Msg;
import net.islandcore.plugin.util.Symbols;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SkillTreeGUI {

    public static final String TREE_TITLE = Msg.color("&7[&a" + Symbols.SKILL_TREE + "&7] &fSkill Tree");
    public static final String BIOME_TITLE = Msg.color("&7[&e" + Symbols.BIOME + "&7] &fBiomes");
    public static final String ENCHANT_TITLE = Msg.color("&7[&5" + Symbols.ENCHANTING + "&7] &fEnchanting");


    private static boolean isEnchantNode(String id) {
        return id.equals("brewing") || id.startsWith("enchanting");
    }

    private final SkillTree tree;
    private final SkillTreeManager manager;
    private final RankManager rankManager;

    public SkillTreeGUI(SkillTree tree, SkillTreeManager manager, RankManager rankManager) {
        this.tree = tree;
        this.manager = manager;
        this.rankManager = rankManager;
    }

    /** Returns true if the player holds Admin or Owner rank. */
    private boolean isAdminOrOwner(Player player) {
        Rank rank = rankManager.getRankOrDefault(player.getUniqueId());
        return rank == Rank.ADMIN || rank == Rank.OWNER;
    }

    // ─────────────────────────────────────────────────────
    //  Page: MAIN TREE
    // ─────────────────────────────────────────────────────

    public void openTreeTab(Player player) {

        Inventory inv = Bukkit.createInventory(
                new SkillTreeHolder(SkillTreeHolder.Page.MAIN),
                SkillTreeSlots.MAIN_SIZE,
                TREE_TITLE
        );

        UUID uuid = player.getUniqueId();

        inv.setItem(
                SkillTreeSlots.MAIN_BACK,
                button(
                        SkillTreeSlots.MAIN_BACK_ICON,
                        "§fBack",
                        "§7Close the skill tree"
                )
        );

        inv.setItem(
                SkillTreeSlots.MAIN_TOKENS,
                buildTokenItem(uuid)
        );

        // Biome button — not currently active tab, no glow
        inv.setItem(
                SkillTreeSlots.MAIN_BIOME_BUTTON,
                button(
                        SkillTreeSlots.MAIN_BIOME_BUTTON_ICON,
                        "§aBiome",
                        "§7Open the biome tab"
                )
        );

        // Enchanting button — not currently active tab, no glow
        inv.setItem(
                SkillTreeSlots.MAIN_ENCHANTING_BUTTON,
                button(
                        SkillTreeSlots.MAIN_ENCHANTING_ICON,
                        "§dEnchanting",
                        "§7Open enchanting and brewing"
                )
        );

        // Admin/Owner unlock-all button — only shown to qualifying ranks and
        // only when the slot has been set to a valid position (>= 0).
        if (isAdminOrOwner(player) && SkillTreeSlots.MAIN_ADMIN_UNLOCK >= 0) {
            inv.setItem(
                    SkillTreeSlots.MAIN_ADMIN_UNLOCK,
                    buildAdminUnlockButton()
            );
        }

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int slot = 0; slot < inv.getSize(); slot++) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, filler);
            }
        }

        for (SkillNode node : tree.getNodes().values()) {

            if (isEnchantNode(node.getId())) {
                continue;
            }

            if (node.getGuiSlot() < 0 || node.getGuiSlot() >= inv.getSize()) {
                continue;
            }

            inv.setItem(
                    node.getGuiSlot(),
                    buildNodeItem(node, uuid)
            );
        }

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────
    //  Page: ENCHANTING
    // ─────────────────────────────────────────────────────

    public void openEnchantTab(Player player) {

        Inventory inv = Bukkit.createInventory(
                new SkillTreeHolder(SkillTreeHolder.Page.ENCHANTING),
                SkillTreeSlots.ENCHANTING_SIZE,
                ENCHANT_TITLE
        );

        UUID uuid = player.getUniqueId();

        inv.setItem(
                SkillTreeSlots.ENCHANT_TOKENS,
                buildTokenItem(uuid)
        );

        // Back button — returns to MAIN, no glow needed
        inv.setItem(
                SkillTreeSlots.ENCHANT_BACK,
                button(
                        Material.CHEST,
                        "§fBack",
                        "§7Return to the Skill Tree"
                )
        );

        // Current-tab indicator with enchant glow so the player knows where they are
        inv.setItem(
                SkillTreeSlots.ENCHANT_TAB_INDICATOR,
                tabIndicator(
                        SkillTreeSlots.MAIN_ENCHANTING_ICON,
                        "§d§lEnchanting",
                        "§7Currently viewing enchanting & brewing"
                )
        );

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int slot = 0; slot < inv.getSize(); slot++) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, filler);
            }
        }

        for (SkillNode node : tree.getNodes().values()) {

            if (!isEnchantNode(node.getId())) {
                continue;
            }

            inv.setItem(
                    node.getGuiSlot(),
                    buildNodeItem(node, uuid)
            );
        }

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────
    //  Page: BIOME
    // ─────────────────────────────────────────────────────

    public void openBiomeTab(Player player) {

        Inventory inv = Bukkit.createInventory(
                new SkillTreeHolder(SkillTreeHolder.Page.BIOME),
                SkillTreeSlots.BIOME_SIZE,
                BIOME_TITLE
        );

        UUID uuid = player.getUniqueId();

        inv.setItem(
                SkillTreeSlots.BIOME_BACK,
                button(
                        Material.CHEST,
                        "§fBack",
                        "§7Return to the Skill Tree"
                )
        );

        inv.setItem(
                SkillTreeSlots.BIOME_TOKENS,
                buildTokenItem(uuid)
        );

        // Current-tab indicator with enchant glow so the player knows where they are
        inv.setItem(
                SkillTreeSlots.BIOME_CURRENT,
                tabIndicator(
                        Material.OAK_SAPLING,
                        "§a§lBiome",
                        "§7Currently viewing biomes"
                )
        );

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int slot = 0; slot < inv.getSize(); slot++) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, filler);
            }
        }

        for (BiomeNode node : tree.getBiomeNodes().values()) {

            inv.setItem(
                    node.getGuiSlot(),
                    buildBiomeItem(node, uuid)
            );
        }

        player.openInventory(inv);
    }

    // ─────────────────────────────────────────────────────
    //  Item builders
    // ─────────────────────────────────────────────────────

    private ItemStack buildNodeItem(SkillNode node, UUID uuid) {

        boolean unlocked = manager.isUnlocked(uuid, node.getId());
        boolean active = node.getId().equals(manager.getActiveNode(uuid));

        // Check prerequisite satisfaction for lore display
        List<String> missingPrereqs = manager.getMissingPrerequisites(uuid, node.getId());

        ItemStack item = new ItemStack(
                unlocked ? node.getIcon() : Material.BEDROCK
        );

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (active) {
            meta.setDisplayName(
                    "§a§l" + stripColor(node.getDisplayName())
            );
        } else if (unlocked) {
            meta.setDisplayName(node.getDisplayName());
        } else {
            meta.setDisplayName(
                    "§7§l" + stripColor(node.getDisplayName())
            );
        }

        List<String> lore = new ArrayList<>();

        for (String line : node.getDescription()) {
            lore.add(line);
        }

        lore.add(" ");

        if (active) {

            lore.add("§a§lACTIVE");

        } else if (unlocked) {

            lore.add("§eClick to select");

        } else {

            // Show prerequisite requirements before cost info
            if (!missingPrereqs.isEmpty()) {
                lore.add("§c§lREQUIRES:");
                for (String prereq : missingPrereqs) {
                    lore.add("§7• §c" + prereq);
                }
                lore.add(" ");
            }

            lore.add("§c§lLOCKED");

            if (node.getTokenCost() > 0) {
                lore.add(
                        "§7Tokens: §6" + node.getTokenCost()
                );
            }

            for (SkillNode.ItemCost cost : node.getItemCosts()) {

                lore.add(
                        "§7• §f"
                                + cost.getAmount()
                                + "x "
                                + prettyName(cost.getMaterial())
                );
            }

            if (missingPrereqs.isEmpty()) {
                lore.add("§eClick to unlock");
            } else {
                lore.add("§7Unlock prerequisites first");
            }
        }

        meta.setLore(lore);

        meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES
        );

        // Active category icon gets the enchanted glow
        if (active) {
            applyGlow(meta);
        }

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack buildBiomeItem(
            BiomeNode node,
            UUID uuid
    ) {

        boolean unlocked =
                manager.isBiomeUnlocked(
                        uuid,
                        node.getId()
                );

        boolean active =
                node.getId().equals(
                        manager.getActiveBiome(uuid)
                );

        List<String> missingBiomePrereqs =
                unlocked ? List.of() : manager.getMissingBiomePrerequisites(uuid, node.getId());

        ItemStack item = new ItemStack(
                unlocked ? node.getIcon() : Material.BEDROCK
        );

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        if (active) {

            meta.setDisplayName(
                    "§a§l" + stripColor(node.getDisplayName())
            );

        } else if (unlocked) {

            meta.setDisplayName(node.getDisplayName());

        } else {

            meta.setDisplayName(
                    "§7§l" + stripColor(node.getDisplayName())
            );
        }

        List<String> lore = new ArrayList<>();

        lore.add(
                "§7" +
                        (node.getBiomeType() ==
                                BiomeNode.BiomeType.FOREST
                                ? "Forest"
                                : "Nether")
                        + " biome"
        );

        lore.add(
                "§7Wood/building family changes with selection."
        );

        lore.add(" ");

        if (active) {

            lore.add("§a§lSELECTED");

        } else if (unlocked) {

            lore.add("§eClick to select");

        } else {

            // Show biome prerequisite requirements before cost info
            if (!missingBiomePrereqs.isEmpty()) {
                lore.add("§c§lREQUIRES:");
                for (String prereq : missingBiomePrereqs) {
                    lore.add("§7• §c" + prereq);
                }
                lore.add(" ");
            }

            lore.add("§c§lLOCKED");

            if (node.getTokenCost() > 0) {
                lore.add(
                        "§7Tokens: §6"
                                + node.getTokenCost()
                );
            }

            for (SkillNode.ItemCost cost :
                    node.getItemCosts()) {

                lore.add(
                        "§7• §f"
                                + cost.getAmount()
                                + "x "
                                + prettyName(
                                cost.getMaterial()
                        )
                );
            }

            if (missingBiomePrereqs.isEmpty()) {
                lore.add("§eClick to unlock");
            } else {
                lore.add("§7Unlock prerequisites first");
            }
        }

        meta.setLore(lore);

        meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES
        );

        // Active/selected biome icon gets the enchanted glow
        if (active) {
            applyGlow(meta);
        }

        item.setItemMeta(meta);

        return item;
    }

    private ItemStack buildTokenItem(UUID uuid) {

        ItemStack item =
                new ItemStack(SkillTreeSlots.MAIN_TOKENS_ICON);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                "§6§lTree Tokens"
        );

        meta.setLore(
                List.of(
                        "§7Balance: §6"
                                + manager.getTokens(uuid),
                        "§7Used to unlock skill tree nodes."
                )
        );

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS
        );

        item.setItemMeta(meta);

        return item;
    }

    // ─────────────────────────────────────────────────────
    //  Helper: admin unlock-all button
    // ─────────────────────────────────────────────────────

    private static ItemStack buildAdminUnlockButton() {
        ItemStack item = new ItemStack(SkillTreeSlots.MAIN_ADMIN_UNLOCK_ICON);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.setDisplayName("§6§lUnlock All");
        meta.setLore(List.of(
                "§7Instantly unlocks every skill node",
                "§7and biome for this player.",
                " ",
                "§c§lAdmin / Owner only"
        ));

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        applyGlow(meta);

        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────────────
    //  Helper: tab indicator (current-tab nav icon with glow)
    // ─────────────────────────────────────────────────────

    /**
     * Builds a nav icon that represents the <em>currently open</em> tab.
     * It gets the enchanted glow so players can instantly see which section
     * they're browsing without reading the inventory title.
     */
    private static ItemStack tabIndicator(
            Material material,
            String name,
            String lore
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);
        meta.setLore(List.of(lore));

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS
        );

        applyGlow(meta);

        item.setItemMeta(meta);
        return item;
    }

    // ─────────────────────────────────────────────────────
    //  Shared utilities
    // ─────────────────────────────────────────────────────

    /**
     * Applies the enchantment-glint visual effect to an ItemMeta.
     * Uses the Luck enchantment (harmless, never shown thanks to HIDE_ENCHANTS).
     */
    private static void applyGlow(ItemMeta meta) {
        Enchantment luck =
                Enchantment.getByKey(
                        NamespacedKey.minecraft("luck")
                );

        if (luck != null) {
            meta.addEnchant(luck, 1, true);
        }
    }

    private static ItemStack button(
            Material material,
            String name,
            String lore
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name);
        meta.setLore(List.of(lore));

        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS
        );

        item.setItemMeta(meta);

        return item;
    }

    private static String prettyName(Material material) {

        String raw =
                material.name()
                        .replace('_', ' ');

        StringBuilder result =
                new StringBuilder();

        for (String word :
                raw.split(" ")) {

            if (!word.isEmpty()) {

                result.append(
                                Character.toUpperCase(
                                        word.charAt(0)
                                )
                        )
                        .append(
                                word.substring(1)
                                        .toLowerCase()
                        )
                        .append(' ');
            }
        }

        return result.toString().trim();
    }

    private static String stripColor(String value) {

        return value.replaceAll(
                "§[0-9a-fk-or]",
                ""
        );
    }
}
