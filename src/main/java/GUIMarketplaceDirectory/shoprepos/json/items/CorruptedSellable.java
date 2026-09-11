package GUIMarketplaceDirectory.shoprepos.json.items;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import GUIMarketplaceDirectory.shoprepos.json.Shop;

@JsonSerialize(using = CorruptedSellable.CorruptedSellableSerializer.class)
public class CorruptedSellable implements Sellable {
    private Material name;
    private Shop shop;
    private Integer price;
    private String qty;

    private Boolean inStock;
    private LocalDateTime outOfStockSince;
    private String outOfStockByName;
    private String outOfStockByUuid;
    private final TreeNode json;

    private String stackTrace;

    public CorruptedSellable(TreeNode json, String stackTrace) {
        this.json = json;
        this.stackTrace = stackTrace;
    }

    public CorruptedSellable(TreeNode json) {
        this.json = json;
    }

    public TreeNode getJson() {
        return json;
    }

    @Override
    public ItemStack getItem(ItemList.BlockBuilder blockBuilder) { //TODO make better
        ItemStack item = new ItemStack(Material.ACACIA_BOAT);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(getStackTrace().split("\n")));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public ItemStack getItemWithShop(ItemList.BlockBuilder blockBuilder, String shopLocColor) {
        return new ItemStack(Material.ACACIA_BOAT);
    }

    @Override
    public Material getName() {
        return name;
    }

    @Override
    public void setName(Material name) {
        this.name = name;
    }

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
    }

    @Override
    public String getQty() {
        if (qty == null) return "";
        else return qty;
    }

    @Override
    public void setQty(String qty) {
        this.qty = qty;
    }

    @Override
    public Boolean getInStock() {
        if (this.inStock == null) return true;
        return this.inStock;
    }

    @Override
    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
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

    public String getStackTrace() {
        return stackTrace == null ? "" : stackTrace;
    }

    public static class CorruptedSellableSerializer extends JsonSerializer<CorruptedSellable> {
        @Override
        public void serialize(CorruptedSellable corruptedSellable, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeTree(corruptedSellable.getJson());
        }
    }
}
