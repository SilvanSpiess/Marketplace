package GUIMarketplaceDirectory.shoprepos.json.items;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffectType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import GUIMarketplaceDirectory.shoprepos.json.Shop;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.DyeColorDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.DyeColorSerializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.EnchantmentDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.EnchantmentKeyDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.EnchantmentKeySerializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.EnchantmentSerializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.LocalDateTimeDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.LocalDateTimeSerializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.MusicInstrumentDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.MusicInstrumentSerializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.PatternTypeDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.PatternTypeSerializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.PotionEffectTypeDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.PotionEffectTypeSerializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.TrimMaterialDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.TrimMaterialSerializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.TrimPatternDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ExtraInfo.TrimPatternSerializer;
import GUIMarketplaceDirectory.utils.MyChatColor;
import net.kyori.adventure.text.Component;

@JsonInclude(Include.NON_NULL)
public class SellableItemList extends ItemList {
    @JsonIgnore
    private Shop shop;
    private Integer price;
    private String qty;

    private Boolean inStock;
    private LocalDateTime outOfStockSince;
    private String outOfStockByName;
    private String outOfStockByUuid;

    @JsonIgnore
    private TreeNode backupJson;
    @JsonIgnore
    private boolean validated = false;
    @JsonIgnore
    private boolean corrupted = false;

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

    public boolean isCorrupted() {
        if (validated) return corrupted;
        else return true; //TODO check if makes sense
    }

    protected ItemStack makeItemStack(BlockBuilder blockBuilder) {
        try {
            ItemStack item = makeDefaultItemStack(blockBuilder);
            validated = true;
            return item;
        } catch (Exception e) {
            corrupted = true;
            return backupItemStack(e.getMessage());
        }
    }
    
    private ItemStack makeDefaultItemStack(BlockBuilder blockBuilder) {
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

    private ItemStack backupItemStack(String errorString) {
        ItemStack item = new ItemStack(Material.ACACIA_BOAT);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(Arrays.asList(errorString.split("\n")));
        item.setItemMeta(meta);
        return item;
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
    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

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

    public Boolean getInStock() {
        if (this.inStock == null) return true;
        return this.inStock;
    }

    public void setInStock(Boolean inStock) {
        this.inStock = inStock;
        if (this.item != null && this.blockBuilder != null) updateItemStack(blockBuilder);
    }

    public LocalDateTime getOutOfStockSince() {
        return this.outOfStockSince;
    }

    public void setOutOfStockSince(LocalDateTime outOfStockSince) {
        this.outOfStockSince = outOfStockSince;
    }

    public String getOutOfStockByName() {
        return this.outOfStockByName;
    }

    public void setOutOfStockByName(String outOfStockByName) {
        this.outOfStockByName = outOfStockByName;
    }

    public String getOutOfStockByUuid() {
        return this.outOfStockByUuid;
    }

    public void setOutOfStockByUuid(String outOfStockByUuid) {
        this.outOfStockByUuid = outOfStockByUuid;
    }

    public TreeNode getBackupJson() {
        return this.backupJson;
    }

    public void setBackupJson(TreeNode backupJson) {
        this.backupJson = backupJson;
    }

    public static class SellableItemListDeserializer extends JsonDeserializer<SellableItemList> {
        private static ObjectMapper simpleMapper = getItemListMapper();

        @Override
        public SellableItemList deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            TreeNode json = p.readValueAsTree();

            try {

                SellableItemList sellable = simpleMapper.treeToValue(json, SellableItemList.class);
                sellable.setBackupJson(json);
                return sellable;
            } catch (JsonProcessingException e) {
                SellableItemList sellable = new SellableItemList();
                sellable.setBackupJson(json);
                return sellable;
            }
        }

        private static ObjectMapper getItemListMapper() {
            ObjectMapper mapper = new ObjectMapper();
            ItemList.equipObjectMapper(mapper);
            return mapper;
        }
    }

    public static class SellableItemListSerializer extends JsonSerializer<SellableItemList> {
        @Override
        public void serialize(SellableItemList corruptedSellable, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (corruptedSellable.isCorrupted()) jgen.writeTree(corruptedSellable.getBackupJson());
            else jgen.writeObject(corruptedSellable); //TODO check if works
        }
    }

    public static void equipObjectMapper(ObjectMapper mapper) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Material.class, new MaterialSerializer());
        module.addDeserializer(Material.class, new MaterialDeserializer());
        module.addKeySerializer(Enchantment.class, new EnchantmentKeySerializer());
        module.addKeyDeserializer(Enchantment.class, new EnchantmentKeyDeserializer());
        module.addSerializer(Enchantment.class, new EnchantmentSerializer());
        module.addDeserializer(Enchantment.class, new EnchantmentDeserializer());
        module.addSerializer(PotionEffectType.class, new PotionEffectTypeSerializer());
        module.addDeserializer(PotionEffectType.class, new PotionEffectTypeDeserializer());
        module.addSerializer(MusicInstrument.class, new MusicInstrumentSerializer());
        module.addDeserializer(MusicInstrument.class, new MusicInstrumentDeserializer());
        module.addSerializer(DyeColor.class, new DyeColorSerializer());
        module.addDeserializer(DyeColor.class, new DyeColorDeserializer());
        module.addSerializer(PatternType.class, new PatternTypeSerializer());
        module.addDeserializer(PatternType.class, new PatternTypeDeserializer());
        module.addSerializer(TrimPattern.class, new TrimPatternSerializer());
        module.addDeserializer(TrimPattern.class, new TrimPatternDeserializer());
        module.addSerializer(TrimMaterial.class, new TrimMaterialSerializer());
        module.addDeserializer(TrimMaterial.class, new TrimMaterialDeserializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        module.addDeserializer(SellableItemList.class, new SellableItemListDeserializer());
        mapper.registerModule(module);
    }
}
