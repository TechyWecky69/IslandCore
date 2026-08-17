package net.islandcore.plugin.skilltree;

import net.islandcore.plugin.ranks.Rank;
import net.islandcore.plugin.ranks.RankManager;
import net.islandcore.plugin.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

/** Handles the three vanilla-looking chest pages without relying on titles. */
public class SkillTreeListener implements Listener {

    private final SkillTree tree;
    private final SkillTreeManager manager;
    private final SkillTreeGUI gui;
    private final RankManager rankManager;

    public SkillTreeListener(SkillTree tree, SkillTreeManager manager, SkillTreeGUI gui, RankManager rankManager) {
        this.tree = tree;
        this.manager = manager;
        this.gui = gui;
        this.rankManager = rankManager;
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof SkillTreeHolder holder)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;

        UUID uuid = player.getUniqueId();

        switch (holder.getPage()) {
            case MAIN -> handleMain(player, uuid, slot);
            case BIOME -> handleBiome(player, uuid, slot);
            case ENCHANTING -> handleEnchanting(player, uuid, slot);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SkillTreeHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMain(
            Player player,
            UUID uuid,
            int slot
    ) {

        if (slot == SkillTreeSlots.MAIN_BACK) {
            player.closeInventory();
            return;
        }

        if (slot == SkillTreeSlots.MAIN_BIOME_BUTTON) {
            gui.openBiomeTab(player);
            return;
        }

        if (slot == SkillTreeSlots.MAIN_ENCHANTING_BUTTON) {
            gui.openEnchantTab(player);
            return;
        }

        if (slot == SkillTreeSlots.MAIN_ADMIN_UNLOCK && SkillTreeSlots.MAIN_ADMIN_UNLOCK >= 0) {
            handleAdminUnlock(player, uuid);
            return;
        }

        SkillNode node = findNode(slot, false);

        if (node != null) {
            resolveNodeClick(
                    player,
                    uuid,
                    node,
                    () -> gui.openTreeTab(player)
            );
        }
    }

    private void handleEnchanting(
            Player player,
            UUID uuid,
            int slot
    ) {

        if (slot == SkillTreeSlots.ENCHANT_BACK) {
            gui.openTreeTab(player);
            return;
        }

        SkillNode node = findNode(slot, true);

        if (node != null) {
            resolveNodeClick(
                    player,
                    uuid,
                    node,
                    () -> gui.openEnchantTab(player)
            );
        }
    }

    private void handleBiome(
            Player player,
            UUID uuid,
            int slot
    ) {

        if (slot == SkillTreeSlots.BIOME_BACK) {
            gui.openTreeTab(player);
            return;
        }

        if (slot == SkillTreeSlots.BIOME_CURRENT) {
            return;
        }

        BiomeNode node = null;

        for (BiomeNode candidate :
                tree.getBiomeNodes().values()) {

            if (candidate.getGuiSlot() == slot) {
                node = candidate;
                break;
            }
        }

        if (node == null) {
            return;
        }

        // Keep the rest of your existing biome unlock/selection code here.
        handleBiomeNode(player, uuid, node);
    }

    private void handleBiomeNode(
            Player player,
            UUID uuid,
            BiomeNode node
    ) {

        if (manager.isBiomeUnlocked(
                uuid,
                node.getId()
        )) {

            manager.setActiveBiome(
                    uuid,
                    node.getId()
            );

            Msg.send(
                    player,
                    "&aSelected biome: &6"
                            + stripColor(
                            node.getDisplayName()
                    )
            );

            gui.openBiomeTab(player);
            return;
        }

        // Check biome prerequisites before item / token costs
        List<String> missingBiomePrereqs =
                manager.getMissingBiomePrerequisites(uuid, node.getId());

        if (!missingBiomePrereqs.isEmpty()) {
            Msg.send(player, "&cYou must unlock the following biomes first:");
            for (String prereq : missingBiomePrereqs) {
                Msg.send(player, "&6 • &f" + prereq);
            }
            return;
        }

        List<SkillNode.ItemCost> costs =
                node.getItemCosts();

        if (!hasItems(player, costs)) {

            Msg.send(
                    player,
                    "&cYou don't have the required items to unlock this biome!"
            );

            sendCostInfo(
                    player,
                    node.getTokenCost(),
                    costs
            );

            return;
        }

        if (manager.getTokens(uuid)
                < node.getTokenCost()) {

            Msg.send(
                    player,
                    "&cYou need &6"
                            + node.getTokenCost()
                            + " Tree Tokens &cto unlock this biome! You have &6"
                            + manager.getTokens(uuid)
                            + "&c."
            );

            return;
        }

        removeItems(player, costs);

        if (manager.unlockBiome(
                uuid,
                node.getId()
        )) {

            manager.setActiveBiome(
                    uuid,
                    node.getId()
            );

            Msg.send(
                    player,
                    "&aUnlocked biome: &6"
                            + stripColor(
                            node.getDisplayName()
                    )
            );

            gui.openBiomeTab(player);

        } else {

            for (SkillNode.ItemCost cost : costs) {

                player.getInventory().addItem(
                        new ItemStack(
                                cost.getMaterial(),
                                cost.getAmount()
                        )
                );
            }

            Msg.send(
                    player,
                    "&cSomething went wrong. Your items have been returned."
            );
        }
    }

    private void handleAdminUnlock(Player player, UUID uuid) {
        Rank rank = rankManager.getRankOrDefault(uuid);
        if (rank != Rank.ADMIN && rank != Rank.OWNER) {
            // Rank check failed — shouldn't normally reach here since the button
            // isn't rendered for non-admins, but guard anyway.
            Msg.send(player, "&cYou don't have permission to use this.");
            return;
        }

        manager.unlockAll(uuid);
        Msg.send(player, "&aAll skill tree nodes and biomes have been unlocked!");
        gui.openTreeTab(player);
    }

    private SkillNode findNode(int slot, boolean enchantingPage) {
        for (SkillNode node : tree.getNodes().values()) {
            boolean enchant = isEnchantNode(node.getId());
            if (enchant == enchantingPage && node.getGuiSlot() == slot) return node;
        }
        return null;
    }

    private void resolveNodeClick(Player player, UUID uuid, SkillNode node, Runnable reopen) {
        if (manager.isUnlocked(uuid, node.getId())) {
            manager.setActiveNode(uuid, node.getId());
            Msg.send(player, "&aActive category set to &6" + stripColor(node.getDisplayName()));
            // Refresh nametag so the new symbol shows above the player's head immediately
            rankManager.setPrefix(player, rankManager.getRankOrDefault(uuid));
            rankManager.updateTabName(player);
            reopen.run();
            return;
        }

        // Check cross-category prerequisites before anything else
        List<String> missingPrereqs = manager.getMissingPrerequisites(uuid, node.getId());
        if (!missingPrereqs.isEmpty()) {
            Msg.send(player, "&cYou must unlock the following first:");
            for (String prereq : missingPrereqs) {
                Msg.send(player, "&6 • &f" + prereq);
            }
            return;
        }

        List<SkillNode.ItemCost> costs = node.getItemCosts();
        if (!hasItems(player, costs)) {
            Msg.send(player, "&cYou don't have the required items to unlock this node!");
            sendCostInfo(player, node.getTokenCost(), costs);
            return;
        }
        if (manager.getTokens(uuid) < node.getTokenCost()) {
            Msg.send(player, "&cYou need &6" + node.getTokenCost() + " Tree Tokens &cto unlock this! You have &6"
                    + manager.getTokens(uuid) + "&c.");
            return;
        }

        removeItems(player, costs);
        if (manager.unlockNode(uuid, node.getId())) {
            manager.setActiveNode(uuid, node.getId());
            Msg.send(player, "&aUnlocked &6" + stripColor(node.getDisplayName()) + "&a! It is now active.");
            // Refresh nametag so the new symbol shows above the player's head immediately
            rankManager.setPrefix(player, rankManager.getRankOrDefault(uuid));
            rankManager.updateTabName(player);
            reopen.run();
        } else {
            for (SkillNode.ItemCost cost : costs) {
                player.getInventory().addItem(new ItemStack(cost.getMaterial(), cost.getAmount()));
            }
            Msg.send(player, "&cSomething went wrong. Your items have been returned.");
        }
    }

    private boolean hasItems(Player player, List<SkillNode.ItemCost> costs) {
        for (SkillNode.ItemCost cost : costs) {
            int count = 0;
            for (ItemStack stack : player.getInventory().getStorageContents()) {
                if (stack != null && stack.getType() == cost.getMaterial()) count += stack.getAmount();
            }
            if (count < cost.getAmount()) return false;
        }
        return true;
    }

    private void removeItems(Player player, List<SkillNode.ItemCost> costs) {
        for (SkillNode.ItemCost cost : costs) {
            int remaining = cost.getAmount();
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (int i = 0; i < contents.length && remaining > 0; i++) {
                ItemStack stack = contents[i];
                if (stack != null && stack.getType() == cost.getMaterial()) {
                    int take = Math.min(remaining, stack.getAmount());
                    stack.setAmount(stack.getAmount() - take);
                    remaining -= take;
                    if (stack.getAmount() <= 0) contents[i] = null;
                }
            }
            player.getInventory().setStorageContents(contents);
        }
    }

    private void sendCostInfo(Player player, int tokens, List<SkillNode.ItemCost> costs) {
        Msg.send(player, "&6Required: &f" + tokens + " tokens");
        for (SkillNode.ItemCost cost : costs) {
            Msg.send(player, "&6• &f" + cost.getAmount() + "x " + prettyName(cost.getMaterial()));
        }
    }

    private static boolean isEnchantNode(String id) {
        return id.equals("brewing") || id.startsWith("enchanting");
    }

    private static String stripColor(String s) { return s.replaceAll("§[0-9a-fk-or]", ""); }

    private static String prettyName(org.bukkit.Material material) {
        String raw = material.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : raw.split(" ")) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase()).append(' ');
        }
        return sb.toString().trim();
    }
}
