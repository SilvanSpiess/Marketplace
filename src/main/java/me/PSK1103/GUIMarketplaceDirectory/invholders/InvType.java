package me.PSK1103.GUIMarketplaceDirectory.invholders;

/*
     * Opens the Shop directory, with different lore/funcionality added to the shopItems, depending on the command used
     * - (type 1) pending -> shows all pending shops
     * - (type 2) review -> removes shops
     * - (type 3) recover -> recovers shopOwner book
     * - (type 4) lookup -> coreprotect -> ShopEvents.java ln 348
     * - (type 5) add_item -> add duplicate item menu
     * - (type 6) search -> search menu
     */
public enum InvType {
    NORMAL, SHOP_MENU, INV_EDIT, PENDING_APPROVALS, PENDING_CHANGES, REVIEW, RECOVER, ADD_ITEM, SEARCH;
}
