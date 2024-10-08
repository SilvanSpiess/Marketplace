package me.PSK1103.GUIMarketplaceDirectory.invholders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MarketplaceBookHolder implements InventoryHolder {
    final List<Map<String,String>> shops;
    final InvType type;
    boolean paged;

    public MarketplaceBookHolder(List<Map<String, String>> shops) {
        this.shops = shops!=null ? shops : new ArrayList<>();
        this.type = InvType.NORMAL;
        paged = false;
    }

    public MarketplaceBookHolder(List<Map<String, String>> shops, InvType type) {
        this.shops = shops;
        this.type = type;
        this.paged = false;
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
}
