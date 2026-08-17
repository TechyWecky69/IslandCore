package net.islandcore.plugin.trade;

import net.islandcore.plugin.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

/** Opens and keeps in sync the two mirrored inventories that make up a trade. */
public class TradeGUI {

    private static final String TITLE = "§8Trade";

    /**
     * Opens the trade GUI for both players in the session. Each player gets
     * their own Inventory object so "my offer" always renders on the left
     * for whoever is looking at it.
     */
    public void open(TradeSession session) {
        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());
        if (a == null || b == null) return;

        TradeHolder holderA = new TradeHolder(session, a.getUniqueId());
        Inventory invA = Bukkit.createInventory(holderA, TradeSlots.SIZE, TITLE + " §7" + b.getName());
        holderA.setInventory(invA);

        TradeHolder holderB = new TradeHolder(session, b.getUniqueId());
        Inventory invB = Bukkit.createInventory(holderB, TradeSlots.SIZE, TITLE + " §7" + a.getName());
        holderB.setInventory(invB);

        render(invA, session, a.getUniqueId());
        render(invB, session, b.getUniqueId());

        a.openInventory(invA);
        b.openInventory(invB);
    }

    /** Rebuilds both players' inventories from the current session state. Call after any change. */
    public void refresh(TradeSession session) {
        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());

        if (a != null) {
            Inventory invA = findOpenInventory(a, session);
            if (invA != null) render(invA, session, a.getUniqueId());
        }
        if (b != null) {
            Inventory invB = findOpenInventory(b, session);
            if (invB != null) render(invB, session, b.getUniqueId());
        }
    }

    private Inventory findOpenInventory(Player player, TradeSession session) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof TradeHolder holder && holder.getSession() == session) {
            return top;
        }
        return null;
    }

    private void render(Inventory inv, TradeSession session, UUID viewer) {
        UUID other = session.other(viewer);

        // Own offer (editable, left)
        ItemStack[] ownOffer = session.getOffer(viewer);
        for (int i = 0; i < TradeSession.OFFER_SLOTS; i++) {
            inv.setItem(TradeSlots.ownSlot(i), ownOffer[i]);
        }

        // Other player's offer (read-only, right)
        ItemStack[] otherOffer = session.getOffer(other);
        for (int i = 0; i < TradeSession.OFFER_SLOTS; i++) {
            inv.setItem(TradeSlots.otherSlot(i), otherOffer[i]);
        }

        // Divider column
        ItemStack divider = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int row = 0; row < 6; row++) {
            inv.setItem(row * 9 + TradeSlots.DIVIDER_COLUMN, divider);
        }

        // Row 4 filler
        ItemStack rowFiller = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int col = 0; col < 9; col++) {
            if (col == TradeSlots.DIVIDER_COLUMN) continue;
            inv.setItem(36 + col, rowFiller);
        }

        // Bottom row filler
        ItemStack bottomFiller = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < 54; slot++) {
            if (slot == TradeSlots.CONFIRM_BUTTON || slot == TradeSlots.CANCEL_BUTTON || slot == TradeSlots.OTHER_STATUS) continue;
            inv.setItem(slot, bottomFiller);
        }

        boolean ownConfirmed = session.isConfirmed(viewer);
        boolean otherConfirmed = session.isOtherConfirmed(viewer);

        Material confirmMat = ownConfirmed ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE;
        List<String> confirmLore = ownConfirmed
                ? List.of("&7Click to un-ready.", "", "&aYou are ready to trade.")
                : List.of("&7Click when you're happy", "&7with the trade to lock it in.", "", "&eWaiting on you.");
        inv.setItem(TradeSlots.CONFIRM_BUTTON, item(confirmMat, ownConfirmed ? "&aConfirm Trade &7(Ready)" : "&eConfirm Trade", confirmLore));

        inv.setItem(TradeSlots.CANCEL_BUTTON, item(Material.BARRIER, "&cCancel Trade",
                List.of("&7Click to cancel this trade.", "&7Your items will be returned.")));

        Material statusMat = otherConfirmed ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        String otherName = other != null && Bukkit.getPlayer(other) != null ? Bukkit.getPlayer(other).getName() : "The other player";
        inv.setItem(TradeSlots.OTHER_STATUS, item(statusMat,
                otherConfirmed ? "&a" + otherName + " is ready!" : "&c" + otherName + " is not ready yet",
                List.of()));
    }

    private ItemStack pane(Material material, String name) {
        return item(material, name, List.of());
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(Msg.color(name));
        if (!lore.isEmpty()) {
            meta.setLore(lore.stream().map(Msg::color).toList());
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
