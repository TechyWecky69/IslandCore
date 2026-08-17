package com.islandcore.anticheat.check;

import com.islandcore.anticheat.IslandCoreAntiCheat;
import com.islandcore.anticheat.data.PlayerData;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * Inventory-level dupe prevention. Bundled together for the same reason the
 * movement/combat checks are: both hang off InventoryClickEvent, and neither
 * is big enough to need its own file.
 */
class DupeClickCheck extends Check {

    DupeClickCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.DUPECLICK); }

    void handle(InventoryClickEvent event) {
        if (!isEnabled()) return;
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player player)) return;
        if (shouldSkip(player)) return;

        PlayerData data = plugin.getPlayerDataManager().get(player);
        long now = System.currentTimeMillis();
        int rawSlot = event.getRawSlot();
        ItemStack current = event.getCurrentItem();
        int itemHash = (current == null || current.getType().isAir())
                ? 0
                : current.getType().hashCode() * 31 + current.getAmount();

        long minInterval = plugin.getConfig().getLong("checks.dupeclick.min-interval-ms", 30);
        long elapsed = now - data.getLastClickTime();

        // A legitimate client cannot land two clicks on the exact same slot,
        // against the exact same item stack, faster than human input allows.
        // Sub-threshold repeats here are the signature of packet-replayed or
        // macro'd click-dupe attempts (e.g. spamming the same shift-click to
        // race the server into moving one item twice).
        if (rawSlot >= 0 && rawSlot == data.getLastClickRawSlot()
                && itemHash != 0 && itemHash == data.getLastClickItemHash()
                && elapsed < minInterval) {
            event.setCancelled(true);
            flag(player, "duplicate click on slot " + rawSlot + " " + elapsed + "ms after the last one");
        }

        data.setLastClickRawSlot(rawSlot);
        data.setLastClickTime(now);
        data.setLastClickItemHash(itemHash);
    }
}

/**
 * Blocks the classic "shulker box (or bundle) inside a shulker box" dupe
 * vector: placing a filled shulker box into another open shulker box's
 * inventory. Vanilla has patched specific variants of this over the years,
 * but new ones keep surfacing, so it's cheap insurance to just disallow the
 * pattern outright rather than trust the current server version's patches.
 */
class NestedContainerCheck extends Check {

    NestedContainerCheck(IslandCoreAntiCheat plugin) { super(plugin, CheckType.NESTEDCONTAINER); }

    void handle(InventoryClickEvent event) {
        if (!isEnabled()) return;
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player player)) return;
        if (shouldSkip(player)) return;

        Inventory topInventory = event.getView().getTopInventory();
        if (topInventory.getType() != InventoryType.SHULKER_BOX) return;

        int topSize = topInventory.getSize();
        ItemStack candidate = null;

        if (event.getRawSlot() >= 0 && event.getRawSlot() < topSize) {
            // Placing whatever is on the cursor directly into a shulker box slot.
            candidate = event.getCursor();
        } else if (event.isShiftClick()) {
            // Shift-clicking an item from the player's own inventory into the
            // open shulker box.
            candidate = event.getCurrentItem();
        }

        if (candidate != null && isFilledShulkerBox(candidate)) {
            event.setCancelled(true);
            flag(player, "tried to place a filled shulker box inside another open shulker box");
        }
    }

    private boolean isFilledShulkerBox(ItemStack item) {
        if (item.getType().isAir() || !item.getType().name().endsWith("SHULKER_BOX")) return false;
        if (!(item.getItemMeta() instanceof BlockStateMeta blockStateMeta)) return false;
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) return false;

        for (ItemStack contained : shulkerBox.getInventory().getContents()) {
            if (contained != null && !contained.getType().isAir()) {
                return true;
            }
        }
        return false;
    }
}
