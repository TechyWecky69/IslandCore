package net.islandcore.plugin.trade;

import net.islandcore.plugin.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class TradeListener implements Listener {

    private final TradeManager tradeManager;
    private final TradeGUI gui;

    public TradeListener(TradeManager tradeManager, TradeGUI gui) {
        this.tradeManager = tradeManager;
        this.gui = gui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof TradeHolder holder)) return;

        TradeSession session = holder.getSession();
        if (session.isClosed()) return;

        event.setCancelled(true);

        UUID viewer = holder.getViewer();
        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        if (rawSlot >= topSize) {
            // Clicked in the player's own real inventory (bottom half) - move that item into the trade.
            handleAddFromPlayerInventory(player, session, viewer, event.getSlot(), event.getClick());
            return;
        }

        if (rawSlot == TradeSlots.CONFIRM_BUTTON) {
            toggleConfirm(session, viewer);
            return;
        }

        if (rawSlot == TradeSlots.CANCEL_BUTTON) {
            cancelTrade(session, "Trade cancelled.");
            return;
        }

        if (TradeSlots.isOwnSlot(rawSlot)) {
            handleRemoveFromOffer(player, session, viewer, rawSlot);
            return;
        }

        // Other player's half is always read-only.
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TradeHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof TradeHolder holder)) return;

        TradeSession session = holder.getSession();
        if (session.isClosed()) return;

        // Either party closing the trade window (esc, inventory command, etc) cancels the whole trade.
        cancelTrade(session, player.getName() + " closed the trade window.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        TradeSession session = tradeManager.getSession(player.getUniqueId());
        if (session == null || session.isClosed()) return;

        cancelTrade(session, player.getName() + " left the game.");
    }

    private void handleAddFromPlayerInventory(Player player, TradeSession session, UUID viewer, int slot, ClickType click) {
        if (slot < 0) return;

        ItemStack clicked = player.getInventory().getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemStack[] offer = session.getOffer(viewer);
        int freeIndex = -1;
        for (int i = 0; i < offer.length; i++) {
            if (offer[i] == null || offer[i].getType() == Material.AIR) {
                freeIndex = i;
                break;
            }
        }

        if (freeIndex == -1) {
            Msg.send(player, "&c⚠ &7Your trade offer is full!");
            return;
        }

        ItemStack toOffer = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT
                ? clicked.clone()
                : clicked.clone();

        offer[freeIndex] = toOffer;
        player.getInventory().setItem(slot, null);

        session.resetConfirmations();
        gui.refresh(session);
    }

    private void handleRemoveFromOffer(Player player, TradeSession session, UUID viewer, int rawSlot) {
        int offerIndex = TradeSlots.offerIndexForOwnSlot(rawSlot);
        if (offerIndex == -1) return;

        ItemStack[] offer = session.getOffer(viewer);
        ItemStack stack = offer[offerIndex];
        if (stack == null || stack.getType() == Material.AIR) return;

        offer[offerIndex] = null;

        var leftover = player.getInventory().addItem(stack);
        for (ItemStack over : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), over);
        }

        session.resetConfirmations();
        gui.refresh(session);
    }

    private void toggleConfirm(TradeSession session, UUID viewer) {
        session.setConfirmed(viewer, !session.isConfirmed(viewer));
        gui.refresh(session);

        if (session.isBothConfirmed()) {
            completeTrade(session);
        }
    }

    private void completeTrade(TradeSession session) {
        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());

        if (a == null || b == null) {
            cancelTrade(session, "Trade cancelled - a player went offline.");
            return;
        }

        List<ItemStack> givenByA = session.nonEmptyOffer(a.getUniqueId());
        List<ItemStack> givenByB = session.nonEmptyOffer(b.getUniqueId());

        session.setClosed(true);
        tradeManager.endSession(session);

        TradeSession.giveItems(b, givenByA);
        TradeSession.giveItems(a, givenByB);

        a.closeInventory();
        b.closeInventory();

        Msg.send(a, "&a✓ &7Trade with &e" + b.getName() + " &7completed!");
        Msg.send(b, "&a✓ &7Trade with &e" + a.getName() + " &7completed!");

        tradeManager.logTrade(a, b, givenByA, givenByB);
    }

    /** Safety net for plugin reload/shutdown so items placed in an open trade are never lost. */
    public void cancelAllTrades(String reason) {
        for (TradeSession session : tradeManager.allSessions()) {
            cancelTrade(session, reason);
        }
    }

    private void cancelTrade(TradeSession session, String reason) {
        if (session.isClosed()) return;
        session.setClosed(true);
        tradeManager.endSession(session);

        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());

        if (a != null) {
            returnItems(a, session.getOffer(session.getPlayerA()));
            Msg.send(a, "&c⚠ &7" + reason);
            a.closeInventory();
        }
        if (b != null) {
            returnItems(b, session.getOffer(session.getPlayerB()));
            Msg.send(b, "&c⚠ &7" + reason);
            b.closeInventory();
        }
    }

    private void returnItems(Player player, ItemStack[] offer) {
        for (int i = 0; i < offer.length; i++) {
            ItemStack stack = offer[i];
            if (stack == null || stack.getType() == Material.AIR) continue;
            offer[i] = null;

            var leftover = player.getInventory().addItem(stack);
            for (ItemStack over : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), over);
            }
        }
    }
}
