package GUIMarketplaceDirectory.shoprepos.json;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffectType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import GUIMarketplaceDirectory.shoprepos.ShopRepo;
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
import GUIMarketplaceDirectory.shoprepos.json.items.ItemList.MaterialDeserializer;
import GUIMarketplaceDirectory.shoprepos.json.items.ItemList.MaterialSerializer;
import GUIMarketplaceDirectory.shoprepos.json.items.Sellable;
import GUIMarketplaceDirectory.shoprepos.json.items.SellableDeserializer;
import GUIMarketplaceDirectory.utils.Metrics;
import GUIMarketplaceDirectory.utils.MyChatColor;
import net.kyori.adventure.text.TextComponent;

public class JSONShopRepo implements ShopRepo {
    private final GUIMarketplaceDirectory plugin;

    public static ObjectMapper mapper;
    private final Logger logger;
    
    private final Map<String, Shop> shops;
    private final Map<String, Shop> pendingShops;
    private final Map<String, Shop> pendingChanges;

    private static final EnumSet<Material> materialsWithoutTextures = EnumSet.noneOf(Material.class);

    static {
        materialsWithoutTextures.addAll(Arrays.asList(Material.LAVA, 
                                                      Material.WATER, 
                                                      Material.BUBBLE_COLUMN,
                                                      Material.PISTON_HEAD,
                                                      Material.MOVING_PISTON,
                                                      Material.AIR,
                                                      Material.ATTACHED_MELON_STEM, 
                                                      Material.ATTACHED_PUMPKIN_STEM));
    }


    public JSONShopRepo(GUIMarketplaceDirectory plugin) {
        mapper = new ObjectMapper();
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
        module.addDeserializer(Sellable.class, new SellableDeserializer());
        mapper.registerModule(module);

        this.shops = new HashMap<>();
        this.pendingShops = new HashMap<>();
        this.pendingChanges = new HashMap<>();

        this.plugin = plugin;
        this.logger = plugin.getLogger();
        if (initShopsFromJSON()) {
            logger.info("Shops loaded");
            if (plugin.getCustomConfig().bstatsEnabled())
                addShopCountMetric();
        } else {
            logger.severe("Error while loading shops, disabling GUIMD");
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    @Override
    public String addShopAsOwner(String name, String desc, String owner, String uuid, String loc, String displayItem) {
        String key = "" + System.currentTimeMillis() + uuid;
        Shop shop = new Shop(name, desc, owner, uuid, key, loc);
        Material material = Material.matchMaterial(displayItem);
        if(material != null && !materialsWithoutTextures.contains(material)) {
            shop.setDisplayItem(displayItem);
        }
        else shop.setDisplayItem("WRITTEN_BOOK");
        if (plugin.getCustomConfig().directoryModerationEnabled())
            pendingShops.put(key, shop);
        else
            shops.put(key, shop);

        saveShops();
        return key;
    }

    @Override
    public String getOwner(String key) {
        return shops.get(key).getOwner();
    }

    public boolean shopExist(String shopKey) {
        return shops.containsKey(shopKey) || pendingShops.containsKey(shopKey);
    }

    public boolean isPendingShop(String shopKey) {
        return pendingShops.containsKey(shopKey);
    }

    public boolean hasPendingChanges(String shopKey) {
        return pendingChanges.containsKey(shopKey);
    }

    @Override
    public void addOwner(String shopKey, OfflinePlayer player) {
        if (pendingShops.containsKey(shopKey)) {
            pendingShops.get(shopKey).addOwner(player.getUniqueId().toString(), player.getName());
        } else if (shops.containsKey(shopKey)) {
            shops.get(shopKey).addOwner(player.getUniqueId().toString(), player.getName());
        }
        saveShops();
    }

    @Override
    public boolean setDisplayItem(Player player, String shopKey, String materialName) {
        Shop shop;
        if (shops.containsKey(shopKey)) {
            shop = shops.get(shopKey);
        } else if (pendingShops.containsKey(shopKey)) {
            shop = pendingShops.get(shopKey);
        } else {
            return false;
        }
        shop.setDisplayItem(materialName);
        saveShops();
        return true;
    }

    @Override
    public boolean setLocation(Player player, String shopKey, String location) {
        Shop shop;
        if (shops.containsKey(shopKey)) {
            shop = shops.get(shopKey);
        } else if (pendingShops.containsKey(shopKey)) {
            shop = pendingShops.get(shopKey);
        } else {
            return false;
        }
        shop.setLoc(location);        
        if(plugin.getCustomConfig().getEnableDynmapMarkers()) {          
            plugin.getDynmapMarkerHandler().updateShopMarkerCommand(player, shopKey);
            player.sendMessage(MyChatColor.GREEN + "Updated Dynmap marker");
        }
            
        saveShops();
        return true;
    }

    @Override
    public boolean setDescription(Player player, String shopKey, String description) {
        Shop shop;
        if (shops.containsKey(shopKey)) {
            shop = shops.get(shopKey);
        } else if (pendingShops.containsKey(shopKey)) {
            shop = pendingShops.get(shopKey);
        } else {
            return false;
        }
        shop.setDesc(description);        
        if(plugin.getCustomConfig().getEnableDynmapMarkers()) {
            plugin.getDynmapMarkerHandler().updateShopMarkerCommand(player, shopKey);
            player.sendMessage(MyChatColor.GREEN + "Updated Dynmap marker");
        }    
        
        saveShops();
        return true;
    }

    @Override
    public void saveShops() {
        if(shops == null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
            try {
                ObjectNode rootNode = mapper.createObjectNode();
                rootNode.put("shops", mapper.valueToTree(shops.values()));
                rootNode.put("pendingShops", mapper.valueToTree(pendingShops.values()));
                rootNode.put("pendingChanges", mapper.valueToTree(pendingChanges.values()));

                File shopFile = plugin.getShops();

                mapper.writerWithDefaultPrettyPrinter().writeValue(shopFile, rootNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean initShopsFromJSON() {
        File shopFile = plugin.getShops();
        try {
            JsonNode rootNode = mapper.readTree(shopFile);
            JsonNode shopsNode = rootNode.path("shops");
            if (shopsNode.isArray() && shopsNode.size() > 0) {
                shopsNode.forEach(shopJson -> {
                    try {
                        Shop shop = mapper.treeToValue(shopJson, Shop.class);
                        shops.put(shop.getKey(), shop);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
            }
            JsonNode pendingShopsNode = rootNode.path("pendingShops");
            if (pendingShopsNode.isArray() && pendingShopsNode.size() > 0) {
                pendingShopsNode.forEach(shopJson -> {
                    try {
                        Shop shop = mapper.treeToValue(shopJson, Shop.class);
                        shops.put(shop.getKey(), shop);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
            }
            JsonNode changesNode = rootNode.path("pendingChanges");
            if (changesNode.isArray() && changesNode.size() > 0) {
                changesNode.forEach(shopJson -> {
                    try {
                        Shop shop = mapper.treeToValue(shopJson, Shop.class);
                        shops.put(shop.getKey(), shop);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                });
            }
            return true;
        } catch (IOException | ClassCastException | NullPointerException e) {
            if (e instanceof ClassCastException)
                plugin.getLogger().severe("Malformed shops.json, cannot initiate shops");
            if (e instanceof NullPointerException)
                plugin.getLogger().warning("Key value(s) missing, shop or item won't be created");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean addItemToShop(Sellable item, String shopkey) {
        Shop shop;
        if (shops.containsKey(shopkey)) {
            shop = shops.get(shopkey);
        } else if (pendingShops.containsKey(shopkey)) {
            shop = pendingShops.get(shopkey);
        } else return false;
        shop.addToInv(item);
        saveShops();
        return true;
    }

    @Override
    public boolean isShopOwner(String uuid, String key) {
        return (shops.containsKey(key) && (shops.get(key).getUuid().equals(uuid) || shops.get(key).getOwners().containsKey(uuid))) || (pendingShops.containsKey(key) && (pendingShops.get(key).getUuid().equals(uuid) || pendingShops.get(key).getOwners().containsKey(uuid)));
    }

    @Override
    public Map<String, Integer> getShopsOfPlayerWithOutOfStockCount(String uuid) {
        Map<String, Integer> ownedShopsWithItemsOutOfStock = new HashMap<>();
        // my Leonne can turn this into a stream
        for (Shop shop : shops.values()) {            
            if (shop.getUuid().equals(uuid)) {
                int count = shop.countOutOfStockItems(shop);
                ownedShopsWithItemsOutOfStock.put(shop.getKey(), count);
            }
        }
        for (Shop shop : pendingShops.values()) {            
            if (shop.getUuid().equals(uuid)) {
                int count = shop.countOutOfStockItems(shop);
                ownedShopsWithItemsOutOfStock.put(shop.getKey(), count);
            }
        }
        for (Shop shop : pendingChanges.values()) {            
            if (shop.getUuid().equals(uuid)) {
                int count = shop.countOutOfStockItems(shop);
                ownedShopsWithItemsOutOfStock.put(shop.getKey(), count);
            }
        }
        return ownedShopsWithItemsOutOfStock;
    }

    @Override
    public boolean approveChange(Player player, String shopKey) {
        if (!pendingChanges.containsKey(shopKey)) {
            return false;
        }
        Shop officialShop;
        if (shops.containsKey(shopKey)) {
            officialShop = shops.get(shopKey);
        } else if (pendingShops.containsKey(shopKey)){
            officialShop = pendingShops.get(shopKey);
        } else return false;
        officialShop.setDesc(pendingChanges.get(shopKey).getDesc());
        officialShop.setOwners(pendingChanges.get(shopKey).getOwners());
        officialShop.setLoc(pendingChanges.get(shopKey).getLoc());
        officialShop.setDisplayItem(pendingChanges.get(shopKey).getDisplayItem());
        if(plugin.getCustomConfig().getEnableDynmapMarkers()) {
            plugin.getDynmapMarkerHandler().updateShopMarkerCommand(player, shopKey);
            player.sendMessage(MyChatColor.GREEN + "Dynmap marker updated");
        }
        pendingChanges.remove(shopKey);
        saveShops();
        return true;
    }

    @Override
    public boolean rejectChange(String shopKey) {
        if(pendingChanges.containsKey(shopKey)) {
            pendingChanges.remove(shopKey);
            saveShops();
        }
        return true;
    }
    
    @Override
    public void submitNewDescription(String uuid, String shopkey, String newDesc) {
        Shop currentShop;
        if (pendingChanges.containsKey(shopkey)) {
            currentShop = pendingChanges.get(shopkey);
        } else if (pendingShops.containsKey(shopkey)) {
            currentShop = pendingShops.get(shopkey);
        } else if (shops.containsKey(shopkey)) {
            currentShop = shops.get(shopkey);
        } else {
            return;
        }
        Shop changedShop = new Shop(currentShop.getName(), 
                                    newDesc, 
                                    currentShop.getOwner(), 
                                    currentShop.getUuid(), 
                                    currentShop.getKey(), 
                                    currentShop.getLoc());
        changedShop.setOwners(currentShop.getOwners());
        changedShop.setDisplayItem(currentShop.getDisplayItem());
        pendingChanges.put(shopkey, changedShop);
        saveShops();
    }

    @Override
    public void submitNewDisplayItem(String uuid, String shopkey, String newDisplayItem) {
        Shop currentShop;
        if (pendingChanges.containsKey(shopkey)) {
            currentShop = pendingChanges.get(shopkey);
        } else if (pendingShops.containsKey(shopkey)) {
            currentShop = pendingShops.get(shopkey);
        } else if (shops.containsKey(shopkey)) {
            currentShop = shops.get(shopkey);
        } else {
            return;
        }
        Shop changedShop = new Shop(currentShop.getName(), 
                                    currentShop.getDesc(), 
                                    currentShop.getOwner(), 
                                    currentShop.getUuid(), 
                                    currentShop.getKey(), 
                                    currentShop.getLoc());
        changedShop.setOwners(currentShop.getOwners());
        changedShop.setDisplayItem(newDisplayItem);
        pendingChanges.put(shopkey, changedShop);
        saveShops();
    }

    @Override
    public void submitNewLocation(String uuid, String shopKey, String newLoc) {
        Shop currentShop;
        if (pendingChanges.containsKey(shopKey)) {
            currentShop = pendingChanges.get(shopKey);
        } else if (pendingShops.containsKey(shopKey)) {
            currentShop = pendingShops.get(shopKey);
        } else if (shops.containsKey(shopKey)) {
            currentShop = shops.get(shopKey);
        } else {
            return;
        }
        Shop changedShop = new Shop(currentShop.getName(), 
                                    currentShop.getDesc(), 
                                    currentShop.getOwner(), 
                                    currentShop.getUuid(), 
                                    currentShop.getKey(), 
                                    newLoc);
        changedShop.setOwners(currentShop.getOwners());
        changedShop.setDisplayItem(currentShop.getDisplayItem());
        pendingChanges.put(shopKey, changedShop);
        saveShops();
    }

    @Override
    public void submitNewOwner(String shopKey, String newUuid, String name) {
        Shop currentShop;
        if (pendingChanges.containsKey(shopKey)) {
            currentShop = pendingChanges.get(shopKey);
        } else if (pendingShops.containsKey(shopKey)) {
            currentShop = pendingShops.get(shopKey);
        } else if (shops.containsKey(shopKey)) {
            currentShop = shops.get(shopKey);
        } else {
            return;
        }
        Shop changedShop = new Shop(currentShop.getName(), 
                                    currentShop.getDesc(), 
                                    currentShop.getOwner(), 
                                    currentShop.getUuid(), 
                                    currentShop.getKey(), 
                                    currentShop.getLoc());
        changedShop.setOwners(currentShop.getOwners());
        changedShop.addOwner(newUuid, name);
        changedShop.setDisplayItem(currentShop.getDisplayItem());
        pendingChanges.put(shopKey, changedShop);
        saveShops();
    }

     @Override
    public void cancelNewDescription(String uuid, String key) {
        Shop changedShop = pendingChanges.get(key);
        Shop originalShop;
        if (shops.containsKey(key)) originalShop = shops.get(key);
        else if (pendingShops.containsKey(key)) originalShop = pendingShops.get(key);
        else return;
        changedShop.setDesc(originalShop.getDesc());
        if(originalShop.getOwners().equals(changedShop.getOwners()) && 
            originalShop.getDesc().equals(changedShop.getDesc()) && 
            originalShop.getLoc().equals(changedShop.getLoc()) && 
            originalShop.getDisplayItem().equals(changedShop.getDisplayItem())) {
            pendingChanges.remove(key);
        } 
        saveShops();
    }

    @Override
    public void cancelNewDisplayItem(String uuid, String key) {
        Shop changedShop = pendingChanges.get(key);
        Shop originalShop;
        if (shops.containsKey(key)) originalShop = shops.get(key);
        else if (pendingShops.containsKey(key)) originalShop = pendingShops.get(key);
        else return;
        changedShop.setDisplayItem(originalShop.getDisplayItem());
        if(originalShop.getOwners().equals(changedShop.getOwners()) && 
           originalShop.getDesc().equals(changedShop.getDesc()) && 
           originalShop.getLoc().equals(changedShop.getLoc()) && 
           originalShop.getDisplayItem().equals(changedShop.getDisplayItem())) {
            pendingChanges.remove(key);
        } 
        saveShops();
    }

    @Override
    public void cancelNewLocation(String uuid, String key) {
        Shop changedShop = pendingChanges.get(key);
        Shop originalShop;
        if (shops.containsKey(key)) originalShop = shops.get(key);
        else if (pendingShops.containsKey(key)) originalShop = pendingShops.get(key);
        else return;
        changedShop.setLoc(originalShop.getLoc());
        if(originalShop.getOwners().equals(changedShop.getOwners()) && 
           originalShop.getDesc().equals(changedShop.getDesc()) && 
           originalShop.getLoc().equals(changedShop.getLoc()) && 
           originalShop.getDisplayItem().equals(changedShop.getDisplayItem())) {
            pendingChanges.remove(key);
        } 
        saveShops();
    }

    @Override
    public void cancelNewOwner(String uuid, String key) {
        Shop changedShop = pendingChanges.get(key);
        Shop originalShop;
        if (shops.containsKey(key)) originalShop = shops.get(key);
        else if (pendingShops.containsKey(key)) originalShop = pendingShops.get(key);
        else return;
        changedShop.setOwners(originalShop.getOwners());
        if(originalShop.getOwners().equals(changedShop.getOwners()) && 
           originalShop.getDesc().equals(changedShop.getDesc()) && 
           originalShop.getLoc().equals(changedShop.getLoc()) && 
           originalShop.getDisplayItem().equals(changedShop.getDisplayItem())) {
            pendingChanges.remove(key);
        } 
        saveShops();
    }

    @Override
    public void approveShop(Player player, String shopKey) {
        if (pendingShops.containsKey(shopKey)) {
            shops.put(shopKey, pendingShops.get(shopKey));
            if(plugin.getCustomConfig().getEnableDynmapMarkers()) {
                plugin.getDynmapMarkerHandler().addShopMarkerCommand(player, shopKey);
                player.sendMessage(MyChatColor.GREEN + "Shop approved and Dynmap marker created");      
            } 
            else player.sendMessage(MyChatColor.GREEN + "Shop approved");
            pendingShops.remove(shopKey);
            saveShops();
        }
    }

    @Override
    public boolean removeShop(Player player, String shopKey) {  
        if(pendingChanges.containsKey(shopKey))
            pendingChanges.remove(shopKey);          
        if(shops.containsKey(shopKey)) {            
            if(plugin.getCustomConfig().getEnableDynmapMarkers()) {
                player.sendMessage(MyChatColor.GRAY + "removing dynmap marker");
                plugin.getDynmapMarkerHandler().deleteShopMarkerCommand(player, shopKey);  
            }    
            shops.remove(shopKey);
        } else if(pendingShops.containsKey(shopKey)) {
            pendingShops.remove(shopKey);
        }
        saveShops();
        return true;
    } 

    private Map<String, String> convertToMap(Shop shop) {
            Map<String, String> details = new HashMap<>();
            details.put("name", shop.getName());
            details.put("desc", shop.getDesc());
            details.put("owners", String.join(", ", shop.getOwners().values()));
            details.put("loc", shop.getLoc());
            details.put("key", shop.getKey());
            details.put("displayItem",shop.getDisplayItem());
        return details;
    }
    
    @Override
    public Map<String, String> getSpecificShopDetails(String key) {
        if(shops.containsKey(key)) return convertToMap(shops.get(key));        
        else if(pendingShops.containsKey(key)) return convertToMap(pendingShops.get(key));        
        else return null;
    }

    @Override
    public Map<String, String> getSpecificChangeDetails(String key) {
        if(pendingChanges.containsKey(key)) return convertToMap(pendingChanges.get(key));      
        else return null;
    }

    @Override
    public List<Map<String, String>> getShopDetails() {
        List<Map<String, String>> detailsList = new ArrayList<>();
        shops.forEach((s, shop) -> detailsList.add(convertToMap(shop)));
        return detailsList;
    }

    @Override
    public List<Map<String, String>> getPendingShopDetails() {
        List<Map<String, String>> detailsList = new ArrayList<>();
        pendingShops.forEach((s, shop) -> detailsList.add(convertToMap(shop)));
        return detailsList;
    }

    @Override
    public List<Map<String, String>> getPendingChangesDetails() {
        List<Map<String, String>> detailsList = new ArrayList<>();
        pendingChanges.forEach((s, shop) -> detailsList.add(convertToMap(shop)));
        return detailsList;
    }

    @Override
    public List<Sellable> getShopInv(String key) {
        Shop shop = null;
        if (shops.containsKey(key))
            shop = shops.get(key);
        else if (pendingShops.containsKey(key))
            shop = pendingShops.get(key);

        List<Sellable> inv = new ArrayList<>();

        if (shop == null) return inv;

        shop.getItems().forEach(itemList -> {
            inv.add(itemList);
        });
        return inv;
    }

    private double getQuantityPerDiamond(String qtyString, int diamondPrice) {
        double value = 0;
        if(diamondPrice<=0) {
            value = Integer.MAX_VALUE;
        }
        else {
            String[] parts1 = qtyString.split(":");
            if (Integer.parseInt(parts1[0]) > 0)
                value = Double.parseDouble(parts1[0]) * 1728;
            else if (Integer.parseInt(parts1[1]) > 0)
                value = Double.parseDouble(parts1[1]) * 64;
            else if (Integer.parseInt(parts1[2]) > 0)
                value = Double.parseDouble(parts1[2]);
            value /= diamondPrice;
        }
        return value;
    }

    @Override
    public void findBetterAlternative(Player player, String key, int pos) {
        Sellable item;
        if (shops.containsKey(key)) {
            item = shops.get(key).getItems().get(pos);
        } else if (pendingShops.containsKey(key)) {
            item = pendingShops.get(key).getItems().get(pos);
        } else {
            player.sendMessage(MyChatColor.RED + "Error: Shop not found.");
            return;
        }
        
        Material name = item.getName();
        double value = getQuantityPerDiamond(item.getQty(), item.getPrice());
        final boolean[] found = {false};
        double finalValue = value;
        shops.forEach((s, shop) ->
                shop.getItems().forEach(itemList -> {
                    if (itemList.getName().equals(name)) {
                        if(plugin.getCustomConfig().filterAlternatives()) {
                            if (((item.getItem(plugin).getType() == Material.POTION && 
                                  itemList.getItem(plugin).getType() == Material.POTION) || 
                                 (item.getItem(plugin).getType() == Material.LINGERING_POTION && 
                                  itemList.getItem(plugin).getType() == Material.LINGERING_POTION) || 
                                 (item.getItem(plugin).getType() == Material.TIPPED_ARROW && 
                                  itemList.getItem(plugin).getType() == Material.TIPPED_ARROW)) && 
                                ((PotionMeta) item.getItem(plugin).getItemMeta()).getBasePotionType() != ((PotionMeta)itemList.getItem(plugin).getItemMeta()).getBasePotionType()
                               )
                                return;
                            if (item.getItem(plugin).getType() == Material.ENCHANTED_BOOK
                                && itemList.getExtraInfo() != null
                                && item.getExtraInfo().getStoredEnchants() != null 
                                && itemList.getItem(plugin).getType() == Material.ENCHANTED_BOOK 
                                && item.getExtraInfo() != null
                                && itemList.getExtraInfo().getStoredEnchants() != null 
                                && ((EnchantmentStorageMeta)item.getItem(plugin).getItemMeta()).getStoredEnchants().keySet().stream().noneMatch(enchantment -> ((EnchantmentStorageMeta)itemList.getItem(plugin).getItemMeta()).getStoredEnchants().containsKey(enchantment)))
                                return;
                        }
                        double val = getQuantityPerDiamond(itemList.getQty(), itemList.getPrice());
                        if (val > finalValue) {
                            player.sendMessage(MyChatColor.GOLD + shop.getName() + MyChatColor.WHITE + " has a better deal: " + ((TextComponent) itemList.getItem(plugin).lore().get(0)).content());
                            found[0] = true;
                        }
                    }
                })
        );
        if (!found[0]) {
            player.sendMessage("No better alternatives found");
        }
    }

    @Override
    public String getShopName(String key) {
        return shops.containsKey(key) ? shops.get(key).getName() : pendingShops.containsKey(key) ? pendingShops.get(key).getName() : "";
    }

    @Override
    public String getShopTitle(String key) {
        if(isPendingShop(key))
            return getShopName(key) + " §5§o(pending approvals)";
        else if(hasPendingChanges(key))
            return getShopName(key) + " §5§o(pending changes)";
        else 
            return getShopName(key);
    }

    @Override
    public List<Map<String, String>> getRefinedShopsByName(String searchKey) {
        return shops.values().stream().filter(shop -> shop.getName().toLowerCase().trim().contains(searchKey.toLowerCase().trim())).map(shop -> convertToMap(shop)).toList();
    }

    @Override
    public List<Sellable> getMatchingItems(String key, Material material) {
        Shop shop = shops.getOrDefault(key, pendingShops.get(key));
        if(shop == null)
            return null;
        List<Sellable> items = new ArrayList<>();
        shop.getItems().forEach(itemList -> {
            if (itemList.getName().equals(material))
                items.add(itemList);
        });
        return items;
    }

    @Override
    public void removeMatchingItems(String key, String itemName) {
        Shop shop = shops.getOrDefault(key, pendingShops.get(key));
        shop.setItems(shop.getItems().stream().filter(itemList -> !itemList.getName().equals(itemName)).collect(Collectors.toList()));
        saveShops();
    }

    @Override
    public void removeItem(String key, Sellable item) {
        Shop shop = shops.getOrDefault(key, pendingShops.get(key));
        shop.setItems(shop.getItems().stream()
            .filter(itemList -> 
                itemList.getItem(plugin).getType() != item.getItem(this.plugin).getType() 
                || !itemList.getPrice().equals(item.getPrice())
                || !itemList.getQty().equals(item.getQty()))
            .collect(Collectors.toList()));
        saveShops();
    }

    @Override
    public List<Map<String, String>> getRefinedShopsByPlayer(String searchKey) {
        return shops.values().stream()
            .filter(shop -> shop.getOwners().values().stream().map(owner -> owner.toLowerCase().trim().contains(searchKey.toLowerCase().trim()))
            .reduce(false, (x, y) -> x || y)).map(shop -> convertToMap(shop)).toList();
    }

    @Override
    public Map<String, Object> findItem(String searchKey) {
        List<Sellable> items = new ArrayList<>();
        List<String> shopKeys = new ArrayList<>();
        shops.forEach((s, shop) -> {
            List<Sellable> inv = shop.getItems();
            inv.forEach(itemList -> {
                if (itemList.getName().getKey().getKey().replace('_', ' ').toLowerCase().trim().contains(searchKey.toLowerCase().trim())) {
                    items.add(itemList);
                    shopKeys.add(shop.getKey());
                } else if (itemList.getCustomName() != null && itemList.getCustomName().length() > 0 && itemList.getCustomName().toLowerCase().trim().contains(searchKey.toLowerCase().trim())) {
                    items.add(itemList);
                    shopKeys.add(shop.getKey());
                }
            });
        });
        Map<String,Object> searchResults = new HashMap<>();
        searchResults.put("items", items);
        searchResults.put("shops", shopKeys);
        return searchResults;
    }

    private void addShopCountMetric() {
        plugin.getMetrics().addCustomChart(new Metrics.SingleLineChart("shop_items", () -> shops.values().stream().mapToInt(shop -> shop.getItems().size()).sum()));
        plugin.getMetrics().addCustomChart(new Metrics.SingleLineChart("shops", shops::size));
    }

    public void markItemOutOfStock(Sellable item, String name, String uuid, LocalDateTime currentTime) {
        item.setInStock(false);
        item.setOutOfStockByName(name);
        item.setOutOfStockByUuid(uuid);
        item.setOutOfStockSince(currentTime);
        saveShops();
    }

    public void markItemInStock(Sellable item) {
        item.setInStock(true);
        item.setOutOfStockByName(null);
        item.setOutOfStockByUuid(null);
        item.setOutOfStockSince(null);
        saveShops();
    }
}