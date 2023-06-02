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
    final int type;
    boolean paged;
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
        this.type = 0;
        this.item = null;
        shops = null;
        paged = false;
    }

    public ShopInvHolder(String key,int type,List<ItemStack> items, List<Integer> itemIds) {
        super();
        this.key = key;
        this.type = type;
        this.items = items;
        this.itemIds = itemIds;
        this.item = null;
        shops = null;
        paged = false;
    }

    public ShopInvHolder(String key, ItemStack item, int type) {
        this.key = key;
        this.item = item;
        this.type = type;
        shops = null;
        paged = false;
    }

    public ShopInvHolder setShops(List<Map<String,String>> shops) {
        this.shops = shops;
        return this;
    }

    public List<Map<String, String>> getShops() {
        return shops;
    }

    public int getType() {
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
}
