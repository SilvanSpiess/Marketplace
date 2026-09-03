package GUIMarketplaceDirectory.shoprepos.json.items;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import GUIMarketplaceDirectory.shoprepos.json.Shop;
import GUIMarketplaceDirectory.utils.MyChatColor;
import net.kyori.adventure.text.Component;

@JsonInclude(Include.NON_NULL)
public class SellableItemList extends ItemList implements Sellable {
    @JsonIgnore
    private Shop shop;
    private Integer price;
    private String qty;

    private Boolean inStock;
    private LocalDateTime outOfStockSince;
    private String outOfStockByName;
    private String outOfStockByUuid;

    public SellableItemList() {
        super();
    }

    public SellableItemList(Material material, String qty, int price, BlockBuilder blockBuilder) {
        super(material, blockBuilder);
        this.qty = qty;
        this.price = price;
        updateItemStack(blockBuilder);
    }

    public SellableItemList(ItemStack item) {
        super(item);
        this.qty = "";
        this.price = 0;
    }

    @Override
    protected ItemStack makeItemStack(BlockBuilder blockBuilder) {
        ItemStack itemStack = super.makeItemStack(blockBuilder);
        ItemMeta meta = itemStack.getItemMeta();

        List<Component> lore = new ArrayList<>(2);

        // out of stock message
        if (!this.getInStock()) {
            lore.add(Component.text(MyChatColor.RED + "§lOUT OF STOCK"));
        }

        // example: x stacks for y diamonds
        String qtyString = getQuantityString();

        if (price > 0 && !qtyString.isEmpty()) {
            lore.add(Component.text("§6" + qtyString + " §ffor §3" + price + " diamond" + (price == 1 ? "" : "s")));
        } else if(price < 0) {
            this.price = -1;
            lore.add(Component.text(MyChatColor.GRAY + "Price hidden or variable"));
        } else if(price == 0) {
            lore.add(Component.text(MyChatColor.GREEN + "Free!"));
        }

        meta.lore(lore);
        meta.getCustomModelDataComponent().setColors(java.util.List.of(Color.fromRGB(80, 80, 80)));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @JsonIgnore
    public String getQuantityString() {
        String qtyString = "";
        String[] parts = qty.split(":");
        if (Integer.parseInt(parts[0]) > 0)
            qtyString = parts[0] + " shulker";
        else if (Integer.parseInt(parts[1]) > 0)
            qtyString = parts[1] + " stack";
        else if (Integer.parseInt(parts[2]) > 0)
            qtyString = parts[2];
        return qtyString;
    }


    @Override
    @JsonIgnore
    public ItemStack getItemWithShop(BlockBuilder blockBuilder, String shopLocColor) {
        ItemStack itemStack = super.getItem(blockBuilder);
        ItemMeta meta = itemStack.getItemMeta();
        List<Component> lore = meta.lore() != null ? meta.lore() : new ArrayList<>();
        lore.add(Component.text(MyChatColor.GREEN + "From " + shop.getName()));
        lore.add(Component.text(shopLocColor + shop.getLoc()));
        meta.lore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    // getters and setters
    @Override
    public Shop getShop() {
        return shop;
    }

    @Override
    public void setShop(Shop shop) {
        this.shop = shop;
    }

    @Override
    public Integer getPrice() {
        if (price == null) return 0;
        else return price;
    }

    @Override
    public void setPrice(int price) {
        this.price = price;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    @Override
    public String getQty() {
        if (qty == null) return "";
        else return qty;
    }

    @Override
    public void setQty(String qty) {
        this.qty = qty;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    @Override
    public Boolean getInStock() {
        if (this.inStock == null) return true;
        return this.inStock;
    }

    @Override
    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    @Override
    public LocalDateTime getOutOfStockSince() {
        return this.outOfStockSince;
    }

    @Override
    public void setOutOfStockSince(LocalDateTime outOfStockSince) {
        this.outOfStockSince = outOfStockSince;
    }

    @Override
    public String getOutOfStockByName() {
        return this.outOfStockByName;
    }

    @Override
    public void setOutOfStockByName(String outOfStockByName) {
        this.outOfStockByName = outOfStockByName;
    }

    @Override
    public String getOutOfStockByUuid() {
        return this.outOfStockByUuid;
    }

    @Override
    public void setOutOfStockByUuid(String outOfStockByUuid) {
        this.outOfStockByUuid = outOfStockByUuid;
    }

}
