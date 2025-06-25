package me.PSK1103.GUIMarketplaceDirectory.invholders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import me.PSK1103.GUIMarketplaceDirectory.utils.GUI.InventoryMaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MarketplaceBookHolder implements InventoryHolder {
    final List<Map<String,String>> shops;
    final InvType type;
    boolean paged;
    InventoryMaker pageMaker;
    InventoryMaker backMaker;

    public MarketplaceBookHolder(List<Map<String, String>> shops, InventoryMaker makeInstructions) {
        this.shops = shops!=null ? shops : new ArrayList<>();
        this.type = InvType.NORMAL;
        this.paged = false;
        this.pageMaker = makeInstructions;
    }

    public MarketplaceBookHolder(List<Map<String, String>> shops, InvType type, InventoryMaker makeInstructions) {
        this.shops = shops;
        this.type = type;
        this.paged = false;
        this.pageMaker = makeInstructions;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public List<Map<String, String>> getShops() {
        return shops;
    }

    public InvType getType() {
        return type;
    }

    public boolean isPaged() {
        return paged;
    }

    public void setPaged() {
        this.paged = true;
    }

    public Inventory makePreviousInventory() {
        if (backMaker != null) return backMaker.makeInventory();
        else return null;
    }

    public InventoryMaker getPreviousInventoryMaker() {
        return backMaker;
    } 

    public void setPreviousInventoryMaker(InventoryMaker maker) {
        backMaker = maker;
    } 

    public InventoryMaker getInventoryMaker() {
        return pageMaker;
    }
}
