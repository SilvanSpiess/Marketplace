package me.PSK1103.GUIMarketplaceDirectory.invholders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import me.PSK1103.GUIMarketplaceDirectory.utils.GUI.InventoryMaker;

import java.util.List;

public class ShopInvHolder implements InventoryHolder {
    final String key;
    ItemStack item;
    List<String> shops;
    List<ItemStack> items;
    final InvType type;
    boolean paged;
    boolean filtered;
    String searchKey;
    InventoryMaker pageMaker;
    InventoryMaker backMaker;

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public String getKey() {
        return key;
    }

    public ShopInvHolder(String key, InventoryMaker makeInstructions) {
        super();
        this.key = key;
        this.type = InvType.NORMAL;
        this.item = null;
        shops = null;
        paged = false;
        filtered = false;
        this.pageMaker = makeInstructions;
    }

    public ShopInvHolder(String key, InvType type, InventoryMaker makeInstructions) {
        super();
        this.key = key;
        this.type = type;
        this.item = null;
        shops = null;
        paged = false;
        filtered = false;
        this.pageMaker = makeInstructions;   
    }

    public ShopInvHolder(String key, InvType type, List<ItemStack> items, InventoryMaker makeInstructions) {
        super();
        this.key = key;
        this.type = type;
        this.items = items;
        this.item = null;
        shops = null;
        paged = false;
        filtered = false;
        this.pageMaker = makeInstructions;
    }

    public ShopInvHolder setShops(List<String> shops) {
        this.shops = shops;
        return this;
    }

    public List<String> getShops() {
        return shops;
    }

    public InvType getType() {
        return type;
    }

    public void setItem(ItemStack item) {
        this.item = item;
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
