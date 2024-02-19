package me.PSK1103.GUIMarketplaceDirectory.shoprepos.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.utils.CoreProtectLookup;
import me.PSK1103.GUIMarketplaceDirectory.utils.Metrics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

class Shop {
    private String name;
    private String loc;
    private String desc;
    private Map<String, String> owners;
    private String owner, uuid;
    private String key;
    private String displayItem;
    private List<ItemList> inv;

    public Shop() {
    }

    public Shop(String name, String desc, String owner, String uuid, String key, String loc) {
        this.name = name;
        this.desc = desc;
        this.owner = owner;
        this.owners = new HashMap<>();
        this.owners.put(uuid, owner);
        this.uuid = uuid;
        this.key = key;
        this.loc = loc;
        this.inv = new ArrayList<>();
        this.displayItem = "WRITTEN_BOOK";
    }

    public void setDisplayItem(String displayItem) {
        this.displayItem = displayItem;
    }

    public void setLoc(String loc) {
        this.loc = loc;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setOwners(Map<String, String> owners) {
        this.owners = new HashMap<>();
        this.owners.putAll(owners);
    }

    public void addOwner(String uuid, String owner) {
        this.owners.put(uuid, owner);
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setInv(List<ItemList> inv) {
        this.inv = inv;
    }

    public void addToInv(ItemList item) {
        inv.add(item);
    }

    public String getName() {
        return name;
    }

    public String getLoc() {
        return loc;
    }

    public String getDesc() {
        return desc;
    }

    public String getOwner() {
        return owner;
    }

    public String getDisplayItem() {
        return displayItem;
    }

    public Map<String, String> getOwners() {
        return owners;
    }

    public String getUuid() {
        return uuid;
    }

    public String getKey() {
        return key;
    }

    public List<ItemList> getInv() {
        return inv == null ? new ArrayList<>() : inv;
    }
}

public class JSONShopRepo implements ShopRepo {
    private final GUIMarketplaceDirectory plugin;
    
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

    private final Logger logger;

    public JSONShopRepo(GUIMarketplaceDirectory plugin) {
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
            player.sendMessage(ChatColor.GREEN + "Updated Dynmap marker");
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
            player.sendMessage(ChatColor.GREEN + "Updated Dynmap marker");
        }    
        
        saveShops();
        return true;
    }

    @Override
    public void saveShops() {
        if(shops == null)
            return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin,() -> {
            JSONParser parser = new JSONParser();
            try {
                File shopFile = plugin.getShops();
                assert shopFile != null;
                parser.parse(new FileReader(shopFile));
                JSONObject data = new JSONObject();
                JSONArray shopJSONs = new JSONArray();
                JSONArray pendingShopJSONs = new JSONArray();
                JSONArray pendingChangesJSONs = new JSONArray();
                shops.forEach((s, shop1) -> {
                    JSONObject shopJSON = convertToJSON(shop1);
                    JSONArray items = new JSONArray();
                    shop1.getInv().forEach(itemList -> items.add(convertToJSON(itemList)));
                    shopJSON.put("items", items);
                    shopJSONs.add(shopJSON);
                });
                pendingShops.forEach((s, shop1) -> {
                    JSONObject shopJSON = convertToJSON(shop1);
                    JSONArray items = new JSONArray();
                    shop1.getInv().forEach(itemList -> items.add(convertToJSON(itemList)));
                    shopJSON.put("items", items);
                    pendingShopJSONs.add(shopJSON);
                });
                pendingChanges.forEach((s, shop1) -> pendingChangesJSONs.add(convertToJSON(shop1)));

                data.put("shops", shopJSONs);
                data.put("pendingShops", pendingShopJSONs);
                data.put("pendingChanges", pendingChangesJSONs);

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(data.toJSONString());
                String prettyJsonString = gson.toJson(je);

                FileWriter fw = new FileWriter(shopFile);
                fw.write(prettyJsonString);
                fw.flush();
                fw.close();

            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        });
    }

    private Shop loadShopData(JSONObject shopJSON) {
        Shop shop = new Shop(shopJSON.get("name").toString(), 
                             shopJSON.get("desc").toString(), 
                             shopJSON.get("owner").toString(), 
                             shopJSON.get("uuid").toString(), 
                             shopJSON.get("key").toString(), 
                             shopJSON.get("loc").toString());
        if (shopJSON.containsKey("owners")) {
            Map<String, String> owners = new Gson().fromJson(shopJSON.get("owners").toString(),
                    new TypeToken<HashMap<String, String>>() {
                    }.getType());

            shop.setOwners(owners);
        }
        if(shopJSON.containsKey("displayItem")) {
            shop.setDisplayItem(shopJSON.get("displayItem").toString());
        }
        return shop;
    }

    private void loadShopInventory(JSONObject shopJSON, Shop shop) {
        JSONArray itemsArray = ((JSONArray) shopJSON.get("items"));
        for (Object o : itemsArray) {
            try {
                JSONObject itemJSON = ((JSONObject) o);
                ItemList item = new ItemList(itemJSON.get("name").toString(), itemJSON.get("qty").toString(), Integer.parseInt(itemJSON.get("price").toString()), this.plugin);
                if (itemJSON.get("customName") != null)
                    item.setCustomName(itemJSON.get("customName").toString());
                if (itemJSON.containsKey("extraInfo") && itemJSON.containsKey("customType")) {
                    JSONObject extraData = ((JSONObject) itemJSON.get("extraInfo"));
                    HashMap<String, Object> headInfo = new Gson().fromJson(extraData.toString(), HashMap.class);
                    item.setExtraInfo(headInfo, itemJSON.getOrDefault("customType","").toString());
                }
                shop.addToInv(item);
            } catch (ClassCastException | NullPointerException e) {
                if (e instanceof ClassCastException)
                    logger.severe("Malformed shops.json, cannot add item");
                if (e instanceof NullPointerException)
                    logger.warning("Key value(s) missing, item won't be created");
                e.printStackTrace();
            }
        }
    }
    private boolean initShopsFromJSON() {
        File shopFile = plugin.getShops();
        try {
            JSONParser parser = new JSONParser();
            assert shopFile != null;
            JSONObject data = (JSONObject) parser.parse(new FileReader(shopFile));
            JSONArray shopJSONs = ((JSONArray) data.get("shops"));
            JSONArray pendingShopJSONs = ((JSONArray) data.get("pendingShops"));
            JSONArray pendingChangesJSONs = ((JSONArray) data.get("pendingChanges"));
            if (shopJSONs == null) shopJSONs = new JSONArray();
            if (pendingShopJSONs == null) pendingShopJSONs = new JSONArray();
            if (pendingChangesJSONs == null) pendingChangesJSONs = new JSONArray();
            /*
             * Loads all the normal shops in the directory (shops.json) 
             */
            if (shopJSONs.size() > 0) {
                for (Object json : shopJSONs) {
                    try {
                        JSONObject shopJSON = ((JSONObject) json);
                        //load the shop data (name, desc, owner, uuid, key, loc, display item) from the JSON
                        Shop shop = loadShopData(shopJSON);
                        //load the shop inventory from the JSON
                        loadShopInventory(shopJSON, shop);
                        shops.put(shopJSON.get("key").toString(), shop);
                    } catch (ClassCastException | NumberFormatException | NullPointerException e) {
                        if (e instanceof ClassCastException || e instanceof NumberFormatException)
                            logger.severe("Malformed shops.json, cannot add shop");
                        if (e instanceof NullPointerException)
                            logger.warning("Key value(s) missing, shop won't be created");
                        e.printStackTrace();
                    }
                }
            } else
                logger.warning("No shops in directory");
            /*
             * Loads all the shops that are pending
             */
            if (pendingShopJSONs.size() > 0) {
                for (Object json : pendingShopJSONs) {
                    try {
                        JSONObject shopJSON = ((JSONObject) json);
                        //load the shop data (name, desc, owner, uuid, key, loc, display item) from the JSON
                        Shop shop = loadShopData(shopJSON);
                        //load the shop inventory from the JSON
                        loadShopInventory(shopJSON, shop);                        
                        pendingShops.put(shopJSON.get("key").toString(), shop);
                    } catch (ClassCastException | NumberFormatException | NullPointerException e) {
                        if (e instanceof ClassCastException || e instanceof NumberFormatException)
                            logger.warning("Malformed shops.json, cannot add shop");
                        if (e instanceof NullPointerException)
                            logger.warning("Key value(s) missing, shop won't be created");
                        e.printStackTrace();
                    }
                }
            }
            /*
             * Loads all the shops that have changes pending
             */
            if (pendingChangesJSONs.size() > 0) {
                for (Object json : pendingChangesJSONs) {
                    try {
                        JSONObject shopJSON = ((JSONObject) json);
                        //load the shop data (name, desc, owner, uuid, key, loc, display item) from the JSON
                        Shop shop = loadShopData(shopJSON);                    
                        pendingChanges.put(shopJSON.get("key").toString(), shop);
                    } catch (ClassCastException | NumberFormatException | NullPointerException e) {
                        if (e instanceof ClassCastException || e instanceof NumberFormatException)
                            logger.warning("Malformed shops.json, cannot add shop");
                        if (e instanceof NullPointerException)
                            logger.warning("Key value(s) missing, shop won't be created");
                        e.printStackTrace();
                    }
                }
            }
            return true;
        } catch (IOException | ParseException | ClassCastException | NullPointerException e) {
            if (e instanceof ParseException || e instanceof ClassCastException)
                plugin.getLogger().severe("Malformed shops.json, cannot initiate shops");
            if (e instanceof NullPointerException)
                plugin.getLogger().warning("Key value(s) missing, shop or item won't be created");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean addItemToShop(ItemList item, String shopkey) {
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
            player.sendMessage(ChatColor.GREEN + "Dynmap marker updated");
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
                player.sendMessage(ChatColor.GREEN + "Shop approved and Dynmap marker created");      
            } 
            else player.sendMessage(ChatColor.GREEN + "Shop approved");
            pendingShops.remove(shopKey);
            saveShops();
        }
    }

    public boolean removeShop(Player player, String shopKey) {  
        if(pendingChanges.containsKey(shopKey))
            pendingChanges.remove(shopKey);          
        if(shops.containsKey(shopKey)) {            
            if(plugin.getCustomConfig().getEnableDynmapMarkers()) {
                player.sendMessage(ChatColor.GRAY + "removing dynmap marker");
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

    private JSONObject convertToJSON(Shop shop) {
        JSONObject shopJSON = new JSONObject();
        shopJSON.put("name", shop.getName());
        shopJSON.put("desc", shop.getDesc());
        shopJSON.put("owner", shop.getOwner());
        shopJSON.put("owners", shop.getOwners());
        shopJSON.put("uuid", shop.getUuid());
        shopJSON.put("key", shop.getKey());
        shopJSON.put("loc", shop.getLoc());
        shopJSON.put("displayItem",shop.getDisplayItem());
        return shopJSON;
    }

    private JSONObject convertToJSON(ItemList itemList) {
        JSONObject item = new JSONObject();
        item.put("name", itemList.item.getType().getKey().getKey().toUpperCase());
        item.put("price", Integer.valueOf(itemList.price).toString());
        item.put("qty", itemList.qty);
        if (itemList.item.getItemMeta().hasDisplayName())
            item.put("customName", (itemList.item.getItemMeta().getDisplayName()));
        if (itemList.extraInfo != null && itemList.extraInfo.size() > 0) {
            item.put("extraInfo", itemList.extraInfo);
        }
        if (itemList.customType != null && itemList.customType.length() > 0) {
            item.put("customType", itemList.customType);
        }
        return item;
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

    public List<Map<String, String>> getShopDetails() {
        List<Map<String, String>> detailsList = new ArrayList<>();
        shops.forEach((s, shop) -> detailsList.add(convertToMap(shop)));
        return detailsList;
    }

    public List<Map<String, String>> getPendingShopDetails() {
        List<Map<String, String>> detailsList = new ArrayList<>();
        pendingShops.forEach((s, shop) -> detailsList.add(convertToMap(shop)));
        return detailsList;
    }

    public List<Map<String, String>> getPendingChangesDetails() {
        List<Map<String, String>> detailsList = new ArrayList<>();
        pendingChanges.forEach((s, shop) -> detailsList.add(convertToMap(shop)));
        return detailsList;
    }

    public List<Object> getShopInv(String key) {
        Shop shop = null;
        if (shops.containsKey(key))
            shop = shops.get(key);
        else if (pendingShops.containsKey(key))
            shop = pendingShops.get(key);

        List<Object> data = new ArrayList<>();
        List<ItemStack> inv = new ArrayList<>();
        List<Integer> itemIds = new ArrayList<>();

        if (shop == null) return data;

        shop.getInv().forEach(itemList -> {
            ItemStack item = itemList.item.clone();
            ItemMeta meta = item.getItemMeta();
            List<Component> lore = meta.lore() == null ? new ArrayList<>() : meta.lore();
            meta.lore(lore);
            item.setItemMeta(meta);
            inv.add(item);
            itemIds.add(-1);

        });
        data.add(inv);
        data.add(itemIds);
        return data;
    }

    public void findBetterAlternative(Player player, String key, int pos) {
        ItemList item = shops.get(key).getInv().get(pos);
        String name = item.name;
        double value = 0;
        if(item.price<=0) {
            value = Integer.MAX_VALUE;
        }
        else {
            String[] parts1 = item.qty.split(":");
            if (Integer.parseInt(parts1[0]) > 0)
                value = Double.parseDouble(parts1[0]) * 1728;
            else if (Integer.parseInt(parts1[1]) > 0)
                value = Double.parseDouble(parts1[1]) * 64;
            else if (Integer.parseInt(parts1[2]) > 0)
                value = Double.parseDouble(parts1[2]);
            value /= item.price;
        }
        final boolean[] found = {false};
        double finalValue = value;
        shops.forEach((s, shop) ->
                shop.getInv().forEach(itemList -> {
                    if (itemList.name.equals(name)) {
                        if(plugin.getCustomConfig().filterAlternatives()) {
                            if(((item.item.getType() == Material.POTION && itemList.item.getType() == Material.POTION) || (item.item.getType() == Material.LINGERING_POTION && itemList.item.getType() == Material.LINGERING_POTION) || (item.item.getType() == Material.TIPPED_ARROW && itemList.item.getType() == Material.TIPPED_ARROW)) && ((PotionMeta)item.item.getItemMeta()).getBasePotionType() != ((PotionMeta)itemList.item.getItemMeta()).getBasePotionType())
                                return;
                            if (item.item.getType() == Material.ENCHANTED_BOOK && item.extraInfo.containsKey("storedEnchants") && itemList.item.getType() == Material.ENCHANTED_BOOK && itemList.extraInfo.containsKey("storedEnchants") && ((EnchantmentStorageMeta)item.item.getItemMeta()).getStoredEnchants().keySet().stream().noneMatch(enchantment -> ((EnchantmentStorageMeta)itemList.item.getItemMeta()).getStoredEnchants().containsKey(enchantment)))
                                return;
                        }
                        double val = 0;
                        if (itemList.price <= 0)
                            val = Integer.MAX_VALUE;
                        else {
                            String[] parts = itemList.qty.split(":");
                            if (Integer.parseInt(parts[0]) > 0)
                                val = Double.parseDouble(parts[0]) * 1728;
                            else if (Integer.parseInt(parts[1]) > 0)
                                val = Double.parseDouble(parts[1]) * 64;
                            else if (Integer.parseInt(parts[2]) > 0)
                                val = Double.parseDouble(parts[2]);
                            val /= itemList.price;
                        }

                        if (val > finalValue) {
                            player.sendMessage(ChatColor.GOLD + shop.getName() + ChatColor.WHITE + " has a better deal: " + ((TextComponent) itemList.getItem().lore().get(0)).content());
                            found[0] = true;
                        }
                    }
                })
        );
        if (!found[0]) {
            player.sendMessage("No better alternatives found");
        }
    }

    public String getShopName(String key) {
        return shops.containsKey(key) ? shops.get(key).getName() : pendingShops.containsKey(key) ? pendingShops.get(key).getName() : "";
    }

    public List<Map<String, String>> getRefinedShopsByName(String searchKey) {
        return shops.values().stream().filter(shop -> shop.getName().toLowerCase().trim().contains(searchKey.toLowerCase().trim())).map(shop -> convertToMap(shop)).toList();
    }

    public List<ItemStack> getMatchingItems(String key, String itemName) {
        Shop shop = shops.getOrDefault(key, pendingShops.get(key));
        if(shop == null)
            return null;
        List<ItemStack> items = new ArrayList<>();
        shop.getInv().forEach(itemList -> {
            if (itemList.name.equalsIgnoreCase(itemName))
                items.add(itemList.item);
        });
        return items;
    }

    public void removeMatchingItems(String key, String itemName) {
        Shop shop = shops.getOrDefault(key, pendingShops.get(key));
        shop.setInv(shop.getInv().stream().filter(itemList -> !itemList.name.equals(itemName)).collect(Collectors.toList()));
        saveShops();
    }

    public void removeItem(String key, ItemStack item) {
        Shop shop = shops.getOrDefault(key, pendingShops.get(key));
        shop.setInv(shop.getInv().stream().filter(itemList -> itemList.getItem().getType() != item.getType() || !((TextComponent) item.getItemMeta().lore().get(0)).content().equals(((TextComponent) itemList.item.getItemMeta().lore().get(0)).content())).collect(Collectors.toList()));
        saveShops();
    }

    public List<Map<String, String>> getRefinedShopsByPlayer(String searchKey) {
        return shops.values().stream()
            .filter(shop -> shop.getOwners().values().stream().map(owner -> owner.toLowerCase().trim().contains(searchKey.toLowerCase().trim()))
            .reduce(false, (x, y) -> x || y)).map(shop -> convertToMap(shop)).toList();
    }

    public Map<String, Object> findItem(String searchKey) {
        List<ItemStack> items = new ArrayList<>();
        List<Map<String,String>> shopIds = new ArrayList<>();
        shops.forEach((s, shop) -> {
            List<ItemList> inv = shop.getInv();
            inv.forEach(itemList -> {
                if (itemList.name.replace('_', ' ').toLowerCase().trim().contains(searchKey.toLowerCase().trim())) {
                    ItemStack itemToAdd = itemList.item.clone();
                    ItemMeta meta = itemToAdd.getItemMeta();
                    List<Component> lore = meta.lore() != null ? meta.lore() : new ArrayList<>();
                    lore.add(Component.text(ChatColor.GREEN + "From " + shop.getName()));
                    lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,plugin.getCustomConfig().getDefaultShopLocColor() + shop.getLoc())));
                    lore.add(Component.text(ChatColor.YELLOW + "Right-click to view this shop"));
                    meta.lore(lore);
                    itemToAdd.setItemMeta(meta);
                    items.add(itemToAdd);
                    Map<String,String> shopData = new HashMap<>();
                    shopData.put("name",shop.getName());
                    shopData.put("id",shop.getKey());
                    shopIds.add(shopData);
                } else if (itemList.customName.length() > 0 && itemList.customName.toLowerCase().trim().contains(searchKey.toLowerCase().trim())) {
                    ItemStack itemToAdd = itemList.item.clone();
                    ItemMeta meta = itemToAdd.getItemMeta();
                    List<Component> lore = meta.lore() != null ? meta.lore() : new ArrayList<>();
                    lore.add(Component.text(ChatColor.GREEN + "From " + shop.getName()));
                    lore.add(Component.text(ChatColor.YELLOW + "Right-click to view this shop"));
                    meta.lore(lore);
                    itemToAdd.setItemMeta(meta);
                    items.add(itemToAdd);
                    Map<String,String> shopData = new HashMap<>();
                    shopData.put("name",shop.getName());
                    shopData.put("id",shop.getKey());
                    shopIds.add(shopData);
                }
            });
        });
        Map<String,Object> searchResults = new HashMap<>();
        searchResults.put("items",items);
        searchResults.put("shops",shopIds);
        return searchResults;
    }

    private void addShopCountMetric() {
        plugin.getMetrics().addCustomChart(new Metrics.SingleLineChart("shop_items", () -> shops.values().stream().mapToInt(shop -> shop.getInv().size()).sum()));
        plugin.getMetrics().addCustomChart(new Metrics.SingleLineChart("shops", shops::size));
    }

    @Override
    public void lookupShop(Player player, String key) {
        String timeString = plugin.getCustomConfig().getLookupTime();
        java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile("(\\d+[m|h|s|d|M])");
        Matcher timeMatcher = timePattern.matcher(timeString);
        int time = 0;
        try {
            while(timeMatcher.find()) {
                String type = timeMatcher.group(2);
                int value = Integer.parseInt(timeMatcher.group(1));
                time += value *
                        switch (type) {
                            case "s" -> 1;
                            case "m" -> 60;
                            case "h" -> 3600;
                            case "d" -> 86400;
                            case "M" -> 3592000;
                            default -> throw new IllegalStateException("Unexpected value: " + type);
                        };
            }
        }
        catch (Exception ignored) {
            time = 604800;
        }
        Shop shop = shops.get(key);
        String loc = shop.getLoc();
        Matcher locMatcher = java.util.regex.Pattern.compile("(-?\\d+),(-?\\d+),(-?\\d+)").matcher(loc);
        int x,y,z;
        if(locMatcher.find()) {            
            if(locMatcher.group(3)==null) {
                x = Integer.parseInt(locMatcher.group(1));
                y = 64;
                z = Integer.parseInt(locMatcher.group(2));
            }
            else {
                x = Integer.parseInt(locMatcher.group(1));
                y = Integer.parseInt(locMatcher.group(2));
                z = Integer.parseInt(locMatcher.group(3));
            }                
        }
        else {
            x = player.getLocation().getBlockX();            
            y = player.getLocation().getBlockY();
            z = player.getLocation().getBlockZ();
        }        
        Location location = new Location(player.getWorld(), x, y, z);
        int radius = plugin.getCustomConfig().getDefaultLookupRadius();
        //int interactions = plugin.getCustomConfig().getInteractions();
        List<String> owners = new ArrayList<>(shop.getOwners().values());
        new CoreProtectLookup(plugin).lookup(player, owners, location, time, radius);
    }

    @Override
    public void lookupAllShops(Player player) {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        shops.forEach((k, shop) -> {
            class LookupThread implements Runnable{
                @Override
                public void run() {
                    player.sendMessage(ChatColor.GREEN + "For " + ChatColor.GOLD + shop.getName());
                    lookupShop(player, shop.getKey());
                }
            }
            executorService.submit(new LookupThread());
        });
        executorService.shutdown();
    }
}