package net.islandcore.plugin.trade;

/**
 * ALL trade GUI slot positions live here.
 *
 * 6-row (54 slot) inventory, standard Bukkit slot numbering:
 *
 *  0   1   2   3   4   5   6   7   8
 *  9  10  11  12  13  14  15  16  17
 * 18  19  20  21  22  23  24  25  26
 * 27  28  29  30  31  32  33  34  35
 * 36  37  38  39  40  41  42  43  44
 * 45  46  47  48  49  50  51  52  53
 *
 * Columns 0-3 (rows 0-3) are the *viewer's own* offer - 16 slots.
 * Column 4 is a divider.
 * Columns 5-8 (rows 0-3) mirror the *other player's* offer - 16 slots, read-only.
 * Row 4 is a status/divider row.
 * Row 5 holds the confirm button (own half, bottom-right = column 3), a
 * cancel button in the middle, and a read-only indicator for whether the
 * other player has confirmed (their half, bottom-left = column 5).
 */
public final class TradeSlots {

    private TradeSlots() {}

    public static final int SIZE = 54;

    /** Top-left slot of each 4x4 offer grid; offer index 0-15 maps to OWN_ORIGIN + row*9 + col. */
    public static final int OWN_ORIGIN = 0;
    public static final int OTHER_ORIGIN = 5;
    public static final int OFFER_COLS = 4;
    public static final int OFFER_ROWS = 4;

    public static final int DIVIDER_COLUMN = 4;

    public static final int CONFIRM_BUTTON = 48;   // row 5, col 3 - bottom-right of own half
    public static final int CANCEL_BUTTON = 49;    // row 5, col 4 - centre
    public static final int OTHER_STATUS = 50;     // row 5, col 5 - bottom-left of other half

    /** Converts an offer index (0-15) into the slot in the viewer's own grid. */
    public static int ownSlot(int offerIndex) {
        int row = offerIndex / OFFER_COLS;
        int col = offerIndex % OFFER_COLS;
        return OWN_ORIGIN + row * 9 + col;
    }

    /** Converts an offer index (0-15) into the slot in the mirrored "other player" grid. */
    public static int otherSlot(int offerIndex) {
        int row = offerIndex / OFFER_COLS;
        int col = offerIndex % OFFER_COLS;
        return OTHER_ORIGIN + row * 9 + col;
    }

    /** Returns the offer index (0-15) for a slot in the own grid, or -1 if it isn't one. */
    public static int offerIndexForOwnSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 0 || row >= OFFER_ROWS) return -1;
        if (col < 0 || col >= OFFER_COLS) return -1;
        return row * OFFER_COLS + col;
    }

    public static boolean isOwnSlot(int slot) {
        return offerIndexForOwnSlot(slot) != -1;
    }

    public static boolean isOtherSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row >= 0 && row < OFFER_ROWS && col >= OTHER_ORIGIN && col < OTHER_ORIGIN + OFFER_COLS;
    }
}
