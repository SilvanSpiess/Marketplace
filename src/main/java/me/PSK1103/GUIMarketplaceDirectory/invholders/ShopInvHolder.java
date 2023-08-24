package me.PSK1103.GUIMarketplaceDirectory.invholders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class ShopInvHolder implements InventoryHolder {
    final String key;
    final ItemStack item;
    List<Map<String,String>> shops;
    List<ItemStack> items;
    List<Integer> itemIds;
    final InvType type;
    boolean paged;
    boolean filtered;
    String searchKey;
    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public String getKey() {
        return key;
    }
    public int getItemId(int pos) {
        return itemIds.get(pos) == -1 ? pos : itemIds.get(pos);
    }

    public ShopInvHolder(String key) {
        super();
        this.key = key;
        this.type = InvType.NORMAL;
        this.item = null;
        shops = null;
        paged = false;
        filtered = false;        
    }

    public ShopInvHolder(String key, InvType type, List<ItemStack> items, List<Integer> itemIds, String searchKey) {
        super();
        this.key = key;
        this.type = type;
        this.items = items;
        this.itemIds = itemIds;
        this.item = null;
        shops = null;
        paged = false;
        filtered = true;  
        this.searchKey = searchKey;
    }

    public ShopInvHolder(String key, InvType type, List<ItemStack> items, List<Integer> itemIds) {
        super();
        this.key = key;
        this.type = type;
        this.items = items;
        this.itemIds = itemIds;
        this.item = null;
        shops = null;
        paged = false;
        filtered = false;  
    }

    public ShopInvHolder(String key, ItemStack item, InvType type) {
        this.key = key;
        this.item = item;
        this.type = type;
        shops = null;
        paged = false;
        filtered = false;  
    }

    public ShopInvHolder setShops(List<Map<String,String>> shops) {
        this.shops = shops;
        return this;
    }

    public List<Map<String, String>> getShops() {
        return shops;
    }

    public InvType getType() {
        return type;
    }

    public ItemStack getItem() {
        return item;
    }

    public List<ItemStack> getInv() {
        return items;
    }

    public void setPaged() {
        this.paged = true;
    }

    public boolean isPaged() {
        return paged;
    }
    
    public boolean getFiltered() {
        return filtered;
    }

    public String getSearchKey() {
        return searchKey;
    }
}
