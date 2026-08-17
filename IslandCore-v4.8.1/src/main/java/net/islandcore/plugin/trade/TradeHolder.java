package net.islandcore.plugin.trade;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Identifies one player's view of a trade GUI. Each side of a trade gets its
 * own Inventory + holder (rather than sharing one Inventory object) so that
 * "my offer" can always render on the left for whoever is looking at it.
 */
public final class TradeHolder implements InventoryHolder {

    private final TradeSession session;
    private final UUID viewer;
    private Inventory inventory;

    public TradeHolder(TradeSession session, UUID viewer) {
        this.session = session;
        this.viewer = viewer;
    }

    public TradeSession getSession() { return session; }
    public UUID getViewer() { return viewer; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
