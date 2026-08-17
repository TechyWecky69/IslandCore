package net.islandcore.plugin.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Holds the live state of a single trade between two players. Each side has
 * its own 16-slot offer area (4x4). Slots are indexed 0-15, row-major.
 */
public class TradeSession {

    public static final int OFFER_SLOTS = 16;

    private final UUID playerA;
    private final UUID playerB;

    private final ItemStack[] offerA = new ItemStack[OFFER_SLOTS];
    private final ItemStack[] offerB = new ItemStack[OFFER_SLOTS];

    private boolean confirmedA;
    private boolean confirmedB;

    /** Set once the trade has finished (completed or cancelled) so late events are ignored. */
    private boolean closed;

    public TradeSession(UUID playerA, UUID playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
    }

    public UUID getPlayerA() { return playerA; }
    public UUID getPlayerB() { return playerB; }

    public boolean involves(UUID uuid) {
        return playerA.equals(uuid) || playerB.equals(uuid);
    }

    public UUID other(UUID uuid) {
        if (playerA.equals(uuid)) return playerB;
        if (playerB.equals(uuid)) return playerA;
        return null;
    }

    public ItemStack[] getOffer(UUID uuid) {
        return playerA.equals(uuid) ? offerA : offerB;
    }

    public ItemStack[] getOtherOffer(UUID uuid) {
        return playerA.equals(uuid) ? offerB : offerA;
    }

    public void setConfirmed(UUID uuid, boolean confirmed) {
        if (playerA.equals(uuid)) confirmedA = confirmed;
        else if (playerB.equals(uuid)) confirmedB = confirmed;
    }

    public boolean isConfirmed(UUID uuid) {
        return playerA.equals(uuid) ? confirmedA : confirmedB;
    }

    public boolean isOtherConfirmed(UUID uuid) {
        return playerA.equals(uuid) ? confirmedB : confirmedA;
    }

    public boolean isBothConfirmed() {
        return confirmedA && confirmedB;
    }

    /** Clears both ready flags - used whenever either offer changes. */
    public void resetConfirmations() {
        confirmedA = false;
        confirmedB = false;
    }

    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }

    /** Returns everything currently offered by a player, skipping empty slots. */
    public java.util.List<ItemStack> nonEmptyOffer(UUID uuid) {
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (ItemStack stack : getOffer(uuid)) {
            if (stack != null && stack.getType() != org.bukkit.Material.AIR) {
                list.add(stack);
            }
        }
        return list;
    }

    /** Gives everything a player offered to the other player's inventory, dropping overflow at their feet. */
    public static void giveItems(Player recipient, java.util.List<ItemStack> items) {
        for (ItemStack stack : items) {
            if (stack == null || stack.getType() == org.bukkit.Material.AIR) continue;
            var leftover = recipient.getInventory().addItem(stack);
            for (ItemStack over : leftover.values()) {
                recipient.getWorld().dropItemNaturally(recipient.getLocation(), over);
            }
        }
    }
}
