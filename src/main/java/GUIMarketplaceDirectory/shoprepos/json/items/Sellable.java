package GUIMarketplaceDirectory.shoprepos.json.items;

import java.time.LocalDateTime;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import GUIMarketplaceDirectory.shoprepos.json.Shop;
import GUIMarketplaceDirectory.shoprepos.json.items.ItemList.BlockBuilder;

public interface Sellable extends Displayable {
    public ItemStack getItemWithShop(BlockBuilder blockBuilder, String shopLocColor);

    // getters and setters
    public default Material getName() {return null;};
    public void setName(Material name);
    public Shop getShop();
    public void setShop(Shop shop);
    public Integer getPrice();
    public void setPrice(int price);
    public String getQty();
    public void setQty(String qty);

    public default String getCustomName() {return null;};
    public default ExtraInfo getExtraInfo() {return null;}

    public Boolean getInStock();
    public void setInStock(Boolean inStock);
    public LocalDateTime getOutOfStockSince();
    public void setOutOfStockSince(LocalDateTime outOfStockSince);
    public String getOutOfStockByName();
    public void setOutOfStockByName(String outOfStockByName);
    public String getOutOfStockByUuid();
    public void setOutOfStockByUuid(String outOfStockByUuid);
}
