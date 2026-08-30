package GUIMarketplaceDirectory.shoprepos.json.items;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import GUIMarketplaceDirectory.utils.MyChatColor;
import net.kyori.adventure.text.Component;

public class SellableItemList extends ItemList {
    private Integer price;
    private String qty;

    private Boolean outOfStock;
    private LocalDateTime outOfStockSince;
    private String outOfStockBy;

    public SellableItemList() {
        super();
    }

    public SellableItemList(String itemName, String qty, int price, BlockBuilder blockBuilder) {
        super(itemName, blockBuilder);
        this.qty = qty;
        this.price = price;
        updateItemStack(blockBuilder);
    }

    public SellableItemList(ItemStack item) {
        super(item);
        this.qty = "";
        this.price = 0;
    }

    protected ItemStack makeItemStack(BlockBuilder blockBuilder) {
        ItemStack itemStack = super.makeItemStack(blockBuilder);
        ItemMeta meta = itemStack.getItemMeta();

        List<Component> lore = new ArrayList<>(2);
        String qtyString = "";
        String[] parts = qty.split(":");
        if (Integer.parseInt(parts[0]) > 0)
            qtyString = parts[0] + " shulker";
        else if (Integer.parseInt(parts[1]) > 0)
            qtyString = parts[1] + " stack";
        else if (Integer.parseInt(parts[2]) > 0)
            qtyString = parts[2];

        if (price > 0 && !qtyString.isEmpty()) {
            lore.add(Component.text("§6" + qtyString + " §ffor §3" + price + " diamond" + (price == 1 ? "" : "s")));
        } else if(price < 0) {
            this.price = -1;
            lore.add(Component.text(MyChatColor.GRAY + "Price hidden or variable"));
        } else if(price == 0) {
            lore.add(Component.text(MyChatColor.GREEN + "Free!"));
        }
        meta.lore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    // getters and setters
    public Integer getPrice() {
        if (price == null) return 0;
        else return price;
    }

    public void setPrice(int price) {
        this.price = price;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    public String getQty() {
        if (qty == null) return "";
        else return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }
}
