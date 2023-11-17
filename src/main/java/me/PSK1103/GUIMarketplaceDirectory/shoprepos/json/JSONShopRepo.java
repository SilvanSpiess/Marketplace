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
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
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
        this.owners = owners;
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
    private final Map<String, Shop> shops;
    private final Map<String, Shop> pendingShops;
    private final Map<String, Shop> pendingChanges;
    private final Map<String, Shop> waitingShops;
    private final GUIMarketplaceDirectory plugin;
    private final HashMap<String, String> shopsUnderAdd;
    private final HashMap<String, EditType> shopsUnderEdit;
    private final HashMap<String, String> userModeratingShop;
    private final HashMap<String, ModerationType> shopUnderModeration;
    private final HashMap<String, ModerationType> userSettingMarkers;
    private final HashMap<String, ItemList> itemToAdd;
    private final HashMap<String, String> shopsAwaitingResponse;

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
        shopsUnderAdd = new HashMap<>();
        shopsAwaitingResponse = new HashMap<>();
        userModeratingShop = new HashMap<>();
        userSettingMarkers = new HashMap<>();
        shopUnderModeration = new HashMap<>();
        shopsUnderEdit = new HashMap<>();
        waitingShops = new HashMap<>();
        itemToAdd = new HashMap<>();
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
    public String addShop(String name, String desc, String owner, String uuid, String loc, String displayItem) {
        String key = "" + System.currentTimeMillis() + uuid;
        Shop shop = new Shop(name, desc, owner, uuid, key, loc);
        Material material = Material.matchMaterial(displayItem);
        if(material != null && !materialsWithoutTextures.contains(material)) {
            shop.setDisplayItem(displayItem);
        }
        else shop.setDisplayItem("WRITTEN_BOOK");
        waitingShops.put(uuid, shop);
        shopsUnderEdit.put(key, EditType.ADD_SHOP);
        shopsUnderAdd.put(uuid, key);
        return key;
    }

    @Override
    public String getOwner(String key) {
        return shops.get(key).getOwner();
    }

    @Override
    public boolean getIsInitOwner(String uuid) {
        return waitingShops.containsKey(uuid);
    }

    @Override
    public void stopInitOwner(String uuid) {
        waitingShops.remove(uuid);
        if (shopsUnderAdd.containsKey(uuid)) {
            shopsUnderEdit.remove(shopsUnderAdd.get(uuid));
            shopsUnderAdd.remove(uuid);
        }
    }

    @Override
    public void stopShopEdit(String uuid) {
        if (shopsUnderAdd.containsKey(uuid)) {
            shopsUnderEdit.remove(shopsUnderAdd.get(uuid));
            shopsUnderAdd.remove(uuid);
        }
    }

    @Override
    public int startSettingDescription(String uuid, String key) {
        if (shopsUnderAdd.containsKey(uuid) && !shopsUnderEdit.containsKey(key))
            return 0;
        if (!shops.containsKey(key) && !pendingShops.containsKey(key))
            return -1;
        shopsUnderAdd.put(uuid, key);    
        shopsUnderEdit.put(key, EditType.SET_DESCRIPTION);
        return 1;
    }

    @Override
    public int startSettingLocation(String uuid, String key) {
        if (shopsUnderAdd.containsKey(uuid) && !shopsUnderEdit.containsKey(key))
            return 0;
        if (!shops.containsKey(key) && !pendingShops.containsKey(key))
            return -1;
        shopsUnderAdd.put(uuid, key);
        shopsUnderEdit.put(key, EditType.SET_LOCATION);
        return 1;
    }

    @Override
    public int startAddingOwner(String uuid, String key) {
        if (shopsUnderAdd.containsKey(uuid) && !shopsUnderEdit.containsKey(key))
            return 0;
        if (!shops.containsKey(key) && !pendingShops.containsKey(key))
            return -1;
        shopsUnderAdd.put(uuid, key);
        shopsUnderEdit.put(key, EditType.ADD_OWNER);
        return 1;
    }

    @Override
    public int startSettingDisplayItem(String uuid, String key) {
        if (shopsUnderAdd.containsKey(uuid) && !shopsUnderEdit.containsKey(key))
            return 0;
        if (!shops.containsKey(key) && !pendingShops.containsKey(key))
            return -1;
        shopsUnderAdd.put(uuid, key);
        shopsUnderEdit.put(key, EditType.SET_DISPLAY_ITEM);
        return 1;
    }

    @Override
    public int startRemovingShop(String uuid, String key) {
        if (shopsUnderAdd.containsKey(uuid) && !shopsUnderEdit.containsKey(key))
            return 0;
        if (!shops.containsKey(key) && !pendingShops.containsKey(key))
            return -1;
        shopsAwaitingResponse.put(uuid, key);
        return 1;
    }

    @Override
    public boolean getIsEditingShop(String uuid, String key) {
        return shopsUnderAdd.containsKey(uuid) || shopsUnderAdd.containsValue(key);
    }

    @Override
    public boolean getIsAddingOwner(String key) {
        return shopsUnderAdd.containsValue(key) && shopsUnderEdit.containsKey(key);
    }

    @Override
    public boolean getIsUserAddingOwner(String uuid) {
        return shopsUnderAdd.containsKey(uuid) && shopsUnderEdit.containsKey(shopsUnderAdd.get(uuid)) || waitingShops.containsKey(uuid);
    }

    @Override
    public void addOwner(String uuid, OfflinePlayer player) {
        if (waitingShops.containsKey(uuid)) {
            Shop shop = waitingShops.get(uuid);
            shop.setOwner(player.getName());
            shop.setUuid(player.getUniqueId().toString());
            shop.addOwner(player.getUniqueId().toString(), player.getName());
            if (plugin.getCustomConfig().directoryModerationEnabled())
                pendingShops.put(shop.getKey(), shop);
            else
                shops.put(shop.getKey(), shop);

            waitingShops.remove(uuid);
            shopsUnderAdd.remove(uuid);
            shopsUnderEdit.remove(shop.getKey());
            saveShops();
        } else {
            if (shopsUnderAdd.containsKey(uuid)) {
                if (pendingShops.containsKey(shopsUnderAdd.get(uuid))) {
                    pendingShops.get(shopsUnderAdd.get(uuid)).addOwner(player.getUniqueId().toString(), player.getName());
                } else if (shops.containsKey(shopsUnderAdd.get(uuid))) {
                    shops.get(shopsUnderAdd.get(uuid)).addOwner(player.getUniqueId().toString(), player.getName());
                }
                shopsUnderEdit.remove(shopsUnderAdd.get(uuid));
                shopsUnderAdd.remove(uuid);
                saveShops();
            }
        }
    }

    @Override
    public void setDisplayItem(Player player, String materialName) {
        String uuid = player.getUniqueId().toString();
        if (shopsUnderAdd.containsKey(uuid)) {
            if (pendingShops.containsKey(shopsUnderAdd.get(uuid))) {
                pendingShops.get(shopsUnderAdd.get(uuid)).setDisplayItem(materialName);
            } else if (shops.containsKey(shopsUnderAdd.get(uuid))) {
                shops.get(shopsUnderAdd.get(uuid)).setDisplayItem(materialName);
            }
            shopsUnderEdit.remove(shopsUnderAdd.get(uuid));
            shopsUnderAdd.remove(uuid);
            saveShops();
        }
    }

    @Override
    public void setLocation(Player player, String location) {
        String uuid = player.getUniqueId().toString();
        if (shopsUnderAdd.containsKey(uuid)) {
            if (pendingShops.containsKey(shopsUnderAdd.get(uuid))) {
                pendingShops.get(shopsUnderAdd.get(uuid)).setLoc(location);
            } else if (shops.containsKey(shopsUnderAdd.get(uuid))) {
                shops.get(shopsUnderAdd.get(uuid)).setLoc(location);
                if(plugin.getCustomConfig().getEnableDynmapMarkers())           
                    updateShopMarker(player);
            }            
            shopsUnderEdit.remove(shopsUnderAdd.get(uuid));
            shopsUnderAdd.remove(uuid);
            saveShops();
        }
    }

    @Override
    public void setDescription(Player player, String description) {
        String uuid = player.getUniqueId().toString();
        if (shopsUnderAdd.containsKey(uuid)) {
            if (pendingShops.containsKey(shopsUnderAdd.get(uuid))) {
                pendingShops.get(shopsUnderAdd.get(uuid)).setDesc(description);
            } else if (shops.containsKey(shopsUnderAdd.get(uuid))) {
                shops.get(shopsUnderAdd.get(uuid)).setDesc(description);
                if(plugin.getCustomConfig().getEnableDynmapMarkers())           
                    updateShopMarker(player);
            }
            shopsUnderEdit.remove(shopsUnderAdd.get(uuid));
            shopsUnderAdd.remove(uuid);
            saveShops();
        }
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
    
    //creates an empty set of markers
    //Example: /dmarker addset shops maxzoom:6 minzoom:4
    public String addShopSet() {
        return "dmarker addset label:" + plugin.getCustomConfig().getShopSetName().replaceAll(" ", "_") + 
               " maxzoom:" + plugin.getCustomConfig().getMaxZoomDynmap() + 
               " minzoom:" + plugin.getCustomConfig().getMinZoomDynmap();
    }

    //deletes the existing shops set
    //Example: /dmarker deleteset shops 
    public String deleteShopSet() {
        return "dmarker deleteset label:" + plugin.getCustomConfig().getShopSetName().replaceAll(" ", "_");
    }

    //creates a new marker for the specified shop
    //Example: /dmarker add shop_name icon:shop_icon set:shops x:-23 y:78 z:-345 world:world
    public String addShopMarker(String key) {
        String[] splitLoc = shops.get(key).getLoc().split(","); 
        if(splitLoc.length == 2) {
            return "dmarker add label:" + shops.get(key).getName().replaceAll(" ", "_").
                                                                   replaceAll("'", "").
                                                                   replaceAll("&", "and") +
                   " icon:" + plugin.getCustomConfig().getShopIconName() +
                   " set:" + plugin.getCustomConfig().getShopSetName() +
                   " x:" + splitLoc[0] + " y:64 z:" + splitLoc[1] + " world:world";
        }
        else {
            return "dmarker add label:" + shops.get(key).getName().replaceAll(" ", "_").
                                                                   replaceAll("'", "").
                                                                   replaceAll("&", "and") +
                   " icon:" + plugin.getCustomConfig().getShopIconName() +
                   " set:" + plugin.getCustomConfig().getShopSetName() +
                   " x:" + splitLoc[0] + " y:" + splitLoc[1] + " z:" + splitLoc[2] + " world:world";
        }        
    }
    
    //creates an empty set of markers
    //Example: /dmarker delete shop_name set:shops
    public String deleteShopMarker(String key) {
        return "dmarker delete label:" + shops.get(key).getName().replaceAll(" ", "_").
                                                                  replaceAll("'", "").
                                                                  replaceAll("&", "and") + 
               " set:" + plugin.getCustomConfig().getShopSetName();
    }

    //appends a description to the specified marker (shop)
    //Example: /dmarker appenddesc shop_name set:shops desc:"Shop by shop_owner, shop_desc"
    public String appendShopMarkerDescription(String key) {
        return "dmarker appenddesc label:" + shops.get(key).getName().replaceAll(" ", "_").
                                                                      replaceAll("'", "").
                                                                      replaceAll("&", "and") + 
               " set:" + plugin.getCustomConfig().getShopSetName() +
               " desc:\"Shop by " + shops.get(key).getOwner() +
               ", " + shops.get(key).getDesc() + "\"";
    }

    //resets the description of the specified marker (shop)
    public String resetShopMarkerDescription(String key) {
        return "dmarker resetdesc " + shops.get(key).getName().replaceAll(" ", "_").
                                                               replaceAll("'", "").
                                                               replaceAll("&", "and") + 
               " set:" + plugin.getCustomConfig().getShopSetName();
    }

    public void updateShopMarker(Player player) {
        String key = shopsUnderAdd.get(player.getUniqueId().toString());
        CommandExecutor commandExecutor = new CommandExecutor(player, deleteShopMarker(key), addShopMarker(key), appendShopMarkerDescription(key));
        Bukkit.getScheduler().runTask(plugin, commandExecutor);
    }

    public void addAllShopMarkers(Player player) {
        player.sendMessage(ChatColor.GREEN + "Starting to create all dynmap shop markers");
        CommandExecutor commandExecutor0 = new CommandExecutor(player, deleteShopSet(), addShopSet());
        Bukkit.getScheduler().runTask(plugin, commandExecutor0);
        for(String key : shops.keySet()) {
            CommandExecutor commandExecutor = new CommandExecutor(player, deleteShopMarker(key), addShopMarker(key), appendShopMarkerDescription(key));
            Bukkit.getScheduler().runTask(plugin, commandExecutor);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } 
        }
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
    public boolean isShopUnderEditOrAdd(String key) {
        return itemToAdd.containsKey(key) || shopsUnderEdit.containsKey(key);
    }

    @Override
    public int initItemAddition(String uuid, String key, String name, ItemStack itemStack) {
        int res = 1;
        if (!shops.containsKey(key) && !pendingShops.containsKey(key))
            return -1;

        if (shopsUnderAdd.containsKey(uuid) || itemToAdd.containsKey(key)) {
            return 0;
        }

        ItemList item = new ItemList(name, itemStack.getItemMeta(), this.plugin);

        if (name.contains("SHULKER_BOX")) {
            if (itemStack.getItemMeta() instanceof BlockStateMeta) {
                BlockStateMeta im = (BlockStateMeta) itemStack.getItemMeta();
                if (im.getBlockState() instanceof ShulkerBox) {
                    ShulkerBox shulker = (ShulkerBox) im.getBlockState();

                    List<Map<String, Object>> contents = new ArrayList<>();

                    for (int i = 0; i < 27; i++) {
                        ItemStack itemStack1 = shulker.getSnapshotInventory().getItem(i);
                        if (itemStack1 == null || itemStack1.getType() == Material.AIR)
                            continue;
                        String n = itemStack1.getType().getKey().getKey().toUpperCase(Locale.ROOT);

                        Map<String, Object> content = new HashMap<>();
                        content.put("name", itemStack1.getType().getKey().getKey().toUpperCase());
                        content.put("quantity", itemStack1.getAmount());

                        if (itemStack1.getType() == Material.PLAYER_HEAD) {
                            SkullMeta skullMeta = (SkullMeta) itemStack1.getItemMeta();
                            Map<String, Object> extraInfo = new HashMap<>();
                            OfflinePlayer whoSkull = skullMeta.getOwningPlayer();
                            if(whoSkull != null) {
                                extraInfo.put("name", skullMeta.getOwningPlayer().getName());
                            }
                            if (skullMeta.getOwnerProfile() != null && 
                                skullMeta.getOwnerProfile().getTextures() != null && 
                                skullMeta.getOwnerProfile().getTextures().getSkin() != null) {
                                extraInfo.put("skin", skullMeta.getOwnerProfile().getTextures().getSkin().toString());
                                extraInfo.put("profileId", skullMeta.getOwnerProfile().getUniqueId().toString());
                            }
                            item.extraInfo = extraInfo;
                            item.customType = "head";
                        } else if (n.contains("POTION")) {
                            PotionMeta potionMeta = (PotionMeta) itemStack1.getItemMeta();
                            Map<String, Object> data = new HashMap<>();
                            PotionData potionType = potionMeta.getBasePotionData();
                            data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
                            data.put("upgraded", potionType.isUpgraded());
                            data.put("extended", potionType.isExtended());
                            content.put("extraInfo", data);
                            content.put("customType", "potion");
                        } else if (n.contains("FIREWORK_ROCKET")) {
                            FireworkMeta rocketMeta = (FireworkMeta) itemStack1.getItemMeta();
                            List<Object> effects = new ArrayList<>();
                            rocketMeta.getEffects().forEach(fireworkEffect -> {
                                Map<String, Object> effect = new HashMap<>();
                                effect.put("type", fireworkEffect.getType());
                                effect.put("flicker", fireworkEffect.hasFlicker());
                                effect.put("trail", fireworkEffect.hasTrail());
                                List<Integer> colors = new ArrayList<>();
                                List<Integer> fadeColors = new ArrayList<>();
                                fireworkEffect.getColors().forEach(color -> colors.add(color.asRGB()));
                                fireworkEffect.getFadeColors().forEach(fadeColor -> fadeColors.add(fadeColor.asRGB()));
                                effect.put("colors", colors);
                                effect.put("fadeColors", fadeColors);
                                effects.add(effect);
                            });
                            Map<String, Object> fireworksData = new HashMap<>();
                            fireworksData.put("flight", Integer.toString(rocketMeta.getPower()));
                            fireworksData.put("effects", effects);
                            content.put("extraInfo", fireworksData);
                            content.put("customType", "rocket");
                        } else if (n.contains("TIPPED_ARROW")) {
                            PotionMeta potionMeta = (PotionMeta) itemStack1.getItemMeta();
                            Map<String, Object> data = new HashMap<>();
                            PotionData potionType = potionMeta.getBasePotionData();
                            data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
                            data.put("upgraded", potionType.isUpgraded());
                            data.put("extended", potionType.isExtended());
                            content.put("extraInfo", data);
                            content.put("customType", "tippedArrow");
                        } else if (n.contains("BANNER")) {
                            BannerMeta bannerMeta = (BannerMeta) itemStack1.getItemMeta();
                            List<Object> patterns = new ArrayList<>();
                            bannerMeta.getPatterns().forEach(pattern -> {
                                Map<String, Object> patternData = new HashMap<>();
                                patternData.put("color", pattern.getColor().name().toUpperCase());
                                patternData.put("type", pattern.getPattern().name().toUpperCase());
                                patterns.add(patternData);
                            });
                            Map<String, Object> info = new HashMap<>();
                            info.put("patterns", patterns);
                            content.put("extraInfo", info);
                            content.put("customType", "banner");
                        } else if(itemStack1.getType() == Material.ENCHANTED_BOOK) {
                            Map<String,String> storedEnchants = new HashMap<>();
                            ((EnchantmentStorageMeta) itemStack1.getItemMeta()).getStoredEnchants().forEach((enchantment, integer) -> storedEnchants.put(enchantment.getKey().getKey(),integer.toString()));
                            Map<String, Object> extraInfo = new HashMap<>();
                            extraInfo.put("storedEnchants",storedEnchants);
                            content.put("extraInfo", extraInfo);
                            content.put("customType", "enchantedBook");
                        } else if(itemStack1.getType() == Material.AXOLOTL_BUCKET) {
                            AxolotlBucketMeta axolotlMeta = (AxolotlBucketMeta) itemStack1.getItemMeta();
                            Map<String, Object> extraInfo = new HashMap<>();
                            extraInfo.put("type", axolotlMeta.getVariant().toString());
                            content.put("extraInfo", extraInfo);
                            content.put("customType", "axolotl");
                        } else if(itemStack1.getType() == Material.WRITABLE_BOOK || itemStack1.getType() == Material.WRITTEN_BOOK) {
                            BookMeta writtenBookMeta = (BookMeta) itemStack1.getItemMeta();
                            Map<String, Object> extraInfo = new HashMap<>();
                            if (writtenBookMeta.hasAuthor()) {
                                extraInfo.put("author", writtenBookMeta.getAuthor());
                            }
                            if (writtenBookMeta.hasGeneration()) {
                                extraInfo.put("generation", writtenBookMeta.getGeneration().toString());
                            }
                            if (writtenBookMeta.hasTitle()) {
                                extraInfo.put("title", writtenBookMeta.getTitle());
                            }
                            content.put("extraInfo", extraInfo);
                            content.put("customType", "writtenBook");
                        } else if(itemStack1.getType() == Material.CROSSBOW) {
                            CrossbowMeta crossbowMeta = (CrossbowMeta) itemStack1.getItemMeta();
                            Map<String, Object> extraInfo = new HashMap<>();
                            extraInfo.put("loaded", crossbowMeta.getChargedProjectiles().get(0).getType().toString());
                            if (crossbowMeta.getChargedProjectiles().get(0).getType() == Material.TIPPED_ARROW) {
                                extraInfo.put("tipped", Integer.toString(((PotionMeta) crossbowMeta.getChargedProjectiles().get(0)).getColor().asRGB()));
                            }
                            content.put("extraInfo", extraInfo);
                            content.put("customType", "crossbow");
                        } else if(n.contains("BOOTS") || n.contains("LEGGINGS") || n.contains("CHESTPLATE") || n.contains("HELMET")) {
                            ArmorMeta armorMeta = (ArmorMeta) itemStack1.getItemMeta();
                            Map<String, Object> extraInfo = new HashMap<>();               
                                
                            if(armorMeta.getTrim() != null) {
                                extraInfo.put("trimMaterial", armorMeta.getTrim().getMaterial().getKey().toString());
                                extraInfo.put("trimPattern", armorMeta.getTrim().getPattern().getKey().toString());
                            }
                            if(n.contains("LEATHER")) {
                                extraInfo.put("color", Integer.toString(((ColorableArmorMeta) itemStack1.getItemMeta()).getColor().asRGB()));
                            }
                            content.put("extraInfo", extraInfo);
                            content.put("customType", "armor");
                        } else if(itemStack1.getType() == Material.FILLED_MAP) {
                            MapMeta mapMeta = (MapMeta) itemStack1.getItemMeta();
                            Map<String, Object> extraInfo = new HashMap<>();
                            extraInfo.put("id", Integer.toString(mapMeta.getMapId()));
                            content.put("extraInfo", extraInfo);
                            content.put("customType", "filledMap");
                        } else if(itemStack1.getType() == Material.GOAT_HORN) {
                            MusicInstrumentMeta goatHornMeta = (MusicInstrumentMeta) itemStack1.getItemMeta();
                            Map<String, Object> extraInfo = new HashMap<>();
                            extraInfo.put("instrument", goatHornMeta.getInstrument().getKey().toString());
                            content.put("extraInfo", extraInfo);
                            content.put("customType", "goatHorn");
                        } else if(itemStack1.getType() == Material.SUSPICIOUS_STEW) {
                            SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta) itemStack1.getItemMeta();
                            Map<String, Object> extraInfo = new HashMap<>();
                            extraInfo.put("effect", suspiciousStewMeta.getCustomEffects().get(0).getType().getName());
                            content.put("extraInfo", extraInfo);
                            content.put("customType", "suspiciousStew");
                        } else if(itemStack1.getType() == Material.TROPICAL_FISH_BUCKET) {
                            TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) itemStack1.getItemMeta();
                            Map<String, Object> extraInfo = new HashMap<>();
                            extraInfo.put("color", tropicalFishBucketMeta.getBodyColor().toString());
                            extraInfo.put("pattern", tropicalFishBucketMeta.getPattern().toString());
                            extraInfo.put("patternColor", tropicalFishBucketMeta.getPatternColor().toString());
                            content.put("extraInfo", extraInfo);
                            content.put("customType", "tropicalFishBucket");
                        }

                        contents.add(content);
                    }
                    item.extraInfo = new HashMap<>();
                    item.extraInfo.put("contents", contents);
                    item.customType = "shulker";
                }
            }
        } else if (itemStack.getType() == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
            Map<String, Object> extraInfo = new HashMap<>();
            OfflinePlayer whoSkull = skullMeta.getOwningPlayer();
            if(whoSkull != null){
                extraInfo.put("name", skullMeta.getOwningPlayer().getName());
            }
            if(skullMeta.getOwnerProfile() != null && 
                skullMeta.getOwnerProfile().getTextures() != null && 
                skullMeta.getOwnerProfile().getTextures().getSkin() != null) {
                    extraInfo.put("skin", skullMeta.getOwnerProfile().getTextures().getSkin().toString());
                    extraInfo.put("profileId", skullMeta.getOwnerProfile().getUniqueId().toString());
            }
            item.extraInfo = extraInfo;
            item.customType = "head";
        } else if (name.contains("POTION")) {
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            Map<String, Object> data = new HashMap<>();
            PotionData potionType = potionMeta.getBasePotionData();
            data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
            data.put("upgraded", potionType.isUpgraded());
            data.put("extended", potionType.isExtended());
            item.extraInfo = data;
            item.customType = "potion";
        } else if (name.contains("FIREWORK_ROCKET")) {
            FireworkMeta rocketMeta = (FireworkMeta) itemStack.getItemMeta();
            List<Object> effects = new ArrayList<>();
            rocketMeta.getEffects().forEach(fireworkEffect -> {
                Map<String, Object> effect = new HashMap<>();
                effect.put("type", fireworkEffect.getType());
                effect.put("flicker", fireworkEffect.hasFlicker());
                effect.put("trail", fireworkEffect.hasTrail());
                List<Integer> colors = new ArrayList<>();
                List<Integer> fadeColors = new ArrayList<>();
                fireworkEffect.getColors().forEach(color -> colors.add(color.asRGB()));
                fireworkEffect.getFadeColors().forEach(fadeColor -> fadeColors.add(fadeColor.asRGB()));
                effect.put("colors", colors);
                effect.put("fadeColors", fadeColors);
                effects.add(effect);
            });
            Map<String, Object> fireworksData = new HashMap<>();
            fireworksData.put("flight", Integer.toString(rocketMeta.getPower()));
            fireworksData.put("effects", effects);
            item.extraInfo = fireworksData;
            item.customType = "rocket";
        } else if (name.contains("TIPPED_ARROW")) {
            PotionMeta potionMeta = (PotionMeta) itemStack.getItemMeta();
            Map<String, Object> data = new HashMap<>();
            PotionData potionType = potionMeta.getBasePotionData();
            data.put("effect", Integer.valueOf(potionType.getType().ordinal()).toString());
            data.put("upgraded", potionType.isUpgraded());
            data.put("extended", potionType.isExtended());
            item.extraInfo = data;
            item.customType = "tippedArrow";
        } else if (name.endsWith("BANNER")) {
            BannerMeta bannerMeta = (BannerMeta) itemStack.getItemMeta();
            List<Object> patterns = new ArrayList<>();
            bannerMeta.getPatterns().forEach(pattern -> {
                Map<String, Object> patternData = new HashMap<>();
                patternData.put("color", pattern.getColor().name().toUpperCase());
                patternData.put("type", pattern.getPattern().name().toUpperCase());
                patterns.add(patternData);
            });
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("patterns", patterns);
            item.customType = "banner";
        }
        else if(itemStack.getType() == Material.ENCHANTED_BOOK) {
            Map<String,String> storedEnchants = new HashMap<>();
            ((EnchantmentStorageMeta) itemStack.getItemMeta()).getStoredEnchants().forEach((enchantment, integer) -> storedEnchants.put(enchantment.getKey().getKey(),integer.toString()));
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("storedEnchants",storedEnchants);
            item.customType = "enchantedBook";
        } else if(itemStack.getType() == Material.AXOLOTL_BUCKET) {
            AxolotlBucketMeta axolotlMeta = (AxolotlBucketMeta) itemStack.getItemMeta();
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("type", axolotlMeta.getVariant().toString());
            item.customType = "axolotl";
        } else if(itemStack.getType() == Material.WRITABLE_BOOK || itemStack.getType() == Material.WRITTEN_BOOK) {
            BookMeta writtenBookMeta = (BookMeta) itemStack.getItemMeta();
            item.extraInfo = new HashMap<>();
            if (writtenBookMeta.hasAuthor()) {
                item.extraInfo.put("author", writtenBookMeta.getAuthor());
            }
            if (writtenBookMeta.hasGeneration()) {
                item.extraInfo.put("generation", writtenBookMeta.getGeneration().toString());
            }
            if (writtenBookMeta.hasTitle()) {
                item.extraInfo.put("title", writtenBookMeta.getTitle());
            }
            item.customType = "writtenBook";
        } else if(itemStack.getType() == Material.CROSSBOW) {
            CrossbowMeta crossbowMeta = (CrossbowMeta) itemStack.getItemMeta();
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("loaded", crossbowMeta.getChargedProjectiles().get(0).getType().toString());
            if (crossbowMeta.getChargedProjectiles().get(0).getType() == Material.TIPPED_ARROW) {
                item.extraInfo.put("tipped", Integer.toString(((PotionMeta) crossbowMeta.getChargedProjectiles().get(0)).getColor().asRGB()));
            }
            item.customType = "crossbow";
        } else if(name.contains("BOOTS") || name.contains("LEGGINGS") || name.contains("CHESTPLATE") || name.contains("HELMET")) {
            ArmorMeta armorMeta = (ArmorMeta) itemStack.getItemMeta();
                item.extraInfo = new HashMap<>();               
                
            if(armorMeta.getTrim() != null) {
                item.extraInfo.put("trimMaterial", armorMeta.getTrim().getMaterial().getKey().toString());
                item.extraInfo.put("trimPattern", armorMeta.getTrim().getPattern().getKey().toString());
            }
            if(name.contains("LEATHER")) {
                item.extraInfo.put("color", Integer.toString(((ColorableArmorMeta) itemStack.getItemMeta()).getColor().asRGB()));
            }
            item.customType = "armor";
        } else if(itemStack.getType() == Material.FILLED_MAP) {
            MapMeta mapMeta = (MapMeta) itemStack.getItemMeta();
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("id", Integer.toString(mapMeta.getMapId()));
            item.customType = "filledMap";
        } else if(itemStack.getType() == Material.GOAT_HORN) {
            MusicInstrumentMeta goatHornMeta = (MusicInstrumentMeta) itemStack.getItemMeta();
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("instrument", goatHornMeta.getInstrument().getKey().toString());
            item.customType = "goatHorn";
        } else if(itemStack.getType() == Material.SUSPICIOUS_STEW) {
            SuspiciousStewMeta suspiciousStewMeta = (SuspiciousStewMeta) itemStack.getItemMeta();
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("effect", suspiciousStewMeta.getCustomEffects().get(0).getType().getName());
            item.customType = "suspiciousStew";
        } else if(itemStack.getType() == Material.TROPICAL_FISH_BUCKET) {
            TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) itemStack.getItemMeta();
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("color", tropicalFishBucketMeta.getBodyColor().toString());
            item.extraInfo.put("pattern", tropicalFishBucketMeta.getPattern().toString());
            item.extraInfo.put("patternColor", tropicalFishBucketMeta.getPatternColor().toString());
            item.customType = "tropicalFishBucket";
        } else if(itemStack.getType() == Material.DECORATED_POT) {
            /*
            DecoratedPot decoratedPot = (DecoratedPot) ((BlockStateMeta) itemStack.getItemMeta()).getBlockState();
            item.extraInfo = new HashMap<>();
            Iterator<NamespacedKey> it = decoratedPot.getPersistentDataContainer().getKeys().iterator();
            while (it.hasNext()) {
                NamespacedKey currKey = it.next();
                item.extraInfo.put("shards" + currKey.getKey(), currKey.getKey());
                item.extraInfo.put("exists", "found smth");
            }
            //item.extraInfo.put("shards", decoratedPot.getPersistentDataContainer().isEmpty() ? "has keys" : "no keys");
            //decoratedPot.getPersistentDataContainer().getKeys().toString()
            item.customType = "decoratedPot";
            */
            /* failed getMetadata approach
            DecoratedPot decoratedPot = (DecoratedPot) ((BlockStateMeta) itemStack.getItemMeta()).getBlockState();
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("shards", decoratedPot.getMetadata("Leonne is sexy").get(0).asString());
            item.customType = "decoratedPot";
            */
            /* Silvans failed getShards
            DecoratedPot decoratedPot = (DecoratedPot) ((BlockStateMeta) itemStack.getItemMeta()).getBlockState();
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("shards", decoratedPot.getShards().toString());
            item.customType = "decoratedPot";
            */
            /* Leonnes failed blockdata
            DecoratedPot decoratedPot = (DecoratedPot) ((BlockDataMeta) itemStack.getItemMeta()).getBlockData(Material.DECORATED_POT);
            item.extraInfo = new HashMap<>();
            item.extraInfo.put("shards", decoratedPot.getAsString());
            item.customType = "decoratedPot";
            */
        } 

        Map<Enchantment,Integer> enchants = itemStack.getEnchantments();
        if(!enchants.isEmpty()) {
            if(item.extraInfo==null)
                item.extraInfo = new HashMap<>();
            Map<String,String> codedEnchants = new HashMap<>();
            Iterator<Map.Entry<Enchantment,Integer>> enchantIterator = enchants.entrySet().iterator();
            while (enchantIterator.hasNext()) {
                Map.Entry<Enchantment,Integer> enchant = enchantIterator.next();
                if (enchant.getValue().intValue() >= enchant.getKey().getStartLevel() && enchant.getValue().intValue() <= enchant.getKey().getMaxLevel() && enchant.getKey().canEnchantItem(itemStack)) {
                    codedEnchants.put(enchant.getKey().getKey().getKey() , enchant.getValue().toString());
                } else res = 2;
            }
            item.extraInfo.put("enchants", codedEnchants);
        } 

        shopsUnderAdd.put(uuid, key);
        itemToAdd.put(key, item);
        return res;
    }

    @Override
    public void initShopOwnerAddition(String uuid) {
        shopsUnderEdit.put(shopsUnderAdd.get(uuid), EditType.SHOP_OWNER_ADDITION);
    }


    @Override
    public EditType getEditType(String uuid) {
        if (!shopsUnderAdd.containsKey(uuid))
            return EditType.NOT_UNDER_ADD;

        return shopsUnderEdit.getOrDefault(shopsUnderAdd.get(uuid), EditType.NOT_UNDER_EDIT);
    }

    @Override
    public void setQty(String qty, String uuid) {
        itemToAdd.get(shopsUnderAdd.get(uuid)).setQty(qty);
    }

    @Override
    public void setPrice(int price, String uuid) {
        itemToAdd.get(shopsUnderAdd.get(uuid)).setPrice(price);
        if (shops.containsKey(shopsUnderAdd.get(uuid)))
            shops.get(shopsUnderAdd.get(uuid)).addToInv(itemToAdd.get(shopsUnderAdd.get(uuid)));
        else if (pendingShops.containsKey(shopsUnderAdd.get(uuid)))
            pendingShops.get(shopsUnderAdd.get(uuid)).addToInv(itemToAdd.get(shopsUnderAdd.get(uuid)));

        itemToAdd.remove(shopsUnderAdd.get(uuid));
        shopsUnderAdd.remove(uuid);
        saveShops();
    }

    @Override
    public boolean isAddingItem(String uuid) {
        return shopsUnderAdd.containsKey(uuid) && !waitingShops.containsKey(uuid) && !shopsUnderEdit.containsKey(shopsUnderAdd.get(uuid));
    }

    @Override
    public void stopEditing(String uuid) {
        itemToAdd.remove(shopsUnderAdd.get(uuid));
        shopsUnderAdd.remove(uuid);
    }

    @Override
    public boolean isShopOwner(String uuid, String key) {
        return (shops.containsKey(key) && (shops.get(key).getUuid().equals(uuid) || shops.get(key).getOwners().containsKey(uuid))) || (pendingShops.containsKey(key) && (pendingShops.get(key).getUuid().equals(uuid) || pendingShops.get(key).getOwners().containsKey(uuid)));
    }

    @Override
    public void approveChange(Player player, String uuid) {
        if(!userModeratingShop.containsKey(uuid) || shopUnderModeration.get(userModeratingShop.get(uuid)) != ModerationType.APPROVE_CHANGE) {
            return;
        }
        String key = userModeratingShop.get(uuid);
        Shop officialShop;
        if (shops.containsKey(key)) {
            officialShop = shops.get(key);
        } else if (pendingShops.containsKey(key)){
            officialShop = pendingShops.get(key);
        } else return;
        officialShop.setDesc(pendingChanges.get(key).getDesc());
        officialShop.setOwners(pendingChanges.get(key).getOwners());
        officialShop.setLoc(pendingChanges.get(key).getLoc());
        officialShop.setDisplayItem(pendingChanges.get(key).getDisplayItem());
        if(plugin.getCustomConfig().getEnableDynmapMarkers()) {
            CommandExecutor commandExecutor = new CommandExecutor(player, deleteShopMarker(key), addShopMarker(key), appendShopMarkerDescription(key) );
            Bukkit.getScheduler().runTask(plugin, commandExecutor);
            player.sendMessage(ChatColor.GREEN + "Change approved and Dynmap marker updated");
        }
        else player.sendMessage(ChatColor.GREEN + "Change approved!");
        pendingChanges.remove(key);
        unlockChange(uuid);
        saveShops();
    }

    @Override
    public void rejectChange(String uuid) {
        if(userModeratingShop.containsKey(uuid) && shopUnderModeration.get(userModeratingShop.get(uuid)) == ModerationType.REJECT_CHANGE) {
            pendingChanges.remove(userModeratingShop.get(uuid));
            unlockChange(uuid);
            saveShops();
        }
    }

    @Override
    public boolean isChangeLocked(String key) {
        return userModeratingShop.containsValue(key);    
    }

    @Override
    public boolean isChangeLocked(String key, ModerationType kind) {
        return userModeratingShop.containsValue(key) && shopUnderModeration.get(key) == kind;    
    }

    @Override
    public boolean hasUserLockedChanges(String uuid) {
        return userModeratingShop.containsKey(uuid);
    }

    @Override
    public boolean hasUserLockedChanges(String uuid, ModerationType kind) {
        return userModeratingShop.containsKey(uuid) && shopUnderModeration.get(userModeratingShop.get(uuid)) == kind;
    }
    
    @Override
    public void initShopMarkerAddition(Player player) {
        userSettingMarkers.put(player.getUniqueId().toString(), ModerationType.SET_MARKERS);
    }

    @Override
    public boolean isUserSettingMarkers(String uuid) {
        return userSettingMarkers.containsKey(uuid) && userSettingMarkers.get(uuid) == ModerationType.SET_MARKERS;
    }

    @Override
    public void unlockSettingMarkers(String uuid) {
        userSettingMarkers.remove(uuid);
    }

    @Override
    public void unlockChange(String uuid) {
        shopUnderModeration.remove(userModeratingShop.get(uuid));
        userModeratingShop.remove(uuid);
    }
    
    @Override
    public void lockChange(String uuid, String key, ModerationType kind) {
        shopUnderModeration.put(key, kind);
        userModeratingShop.put(uuid, key);
    } 
    
    @Override
    public void submitNewDescription(String uuid, String newDesc) {
        String key = shopsUnderAdd.get(uuid);
        Shop currentShop;
        if (pendingChanges.containsKey(key)) {
            currentShop = pendingChanges.get(key);
        } else if (pendingShops.containsKey(key)) {
            currentShop = pendingShops.get(key);
        } else if (shops.containsKey(key)) {
            currentShop = shops.get(key);
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
        pendingChanges.put(key, changedShop);
        saveShops();
    }

    @Override
    public void submitNewDisplayItem(String uuid, String newDisplayItem) {
        String key = shopsUnderAdd.get(uuid);
        Shop currentShop;
        if (pendingChanges.containsKey(key)) {
            currentShop = pendingChanges.get(key);
        } else if (pendingShops.containsKey(key)) {
            currentShop = pendingShops.get(key);
        } else if (shops.containsKey(key)) {
            currentShop = shops.get(key);
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
        pendingChanges.put(key, changedShop);
        saveShops();
    }

    @Override
    public void submitNewLocation(String uuid, String newLoc) {
        String key = shopsUnderAdd.get(uuid);
        Shop currentShop;
        if (pendingChanges.containsKey(key)) {
            currentShop = pendingChanges.get(key);
        } else if (pendingShops.containsKey(key)) {
            currentShop = pendingShops.get(key);
        } else if (shops.containsKey(key)) {
            currentShop = shops.get(key);
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
        pendingChanges.put(key, changedShop);
        saveShops();
    }

    @Override
    public void submitNewOwner(String uuid, String newUuid, String name) {
        String key = shopsUnderAdd.get(uuid);
        Shop currentShop;
        if (pendingChanges.containsKey(key)) {
            currentShop = pendingChanges.get(key);
        } else if (pendingShops.containsKey(key)) {
            currentShop = pendingShops.get(key);
        } else if (shops.containsKey(key)) {
            currentShop = shops.get(key);
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
        pendingChanges.put(key, changedShop);
        stopInitOwner(uuid);
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
    public void approveShop(Player player, String key) {
        if (pendingShops.containsKey(key)) {
            shops.put(key, pendingShops.get(key));
            if(plugin.getCustomConfig().getEnableDynmapMarkers()) {
                CommandExecutor commandExecutor = new CommandExecutor(player, addShopMarker(key), appendShopMarkerDescription(key) );
                Bukkit.getScheduler().runTask(plugin, commandExecutor); 
                player.sendMessage(ChatColor.GREEN + "Shop approved and Dynmap marker created");      
            } 
            else player.sendMessage(ChatColor.GREEN + "Shop approved");
            pendingShops.remove(key);
            saveShops();
        }
    }

    @Override
    public void removeShop(Player player, String uuid) {
        String key = shopsAwaitingResponse.get(uuid);              
        if(shops.containsKey(key)) {            
            if(plugin.getCustomConfig().getEnableDynmapMarkers()) {
                CommandExecutor commandExecutor = new CommandExecutor(player, deleteShopMarker(key));
                Bukkit.getScheduler().runTask(plugin, commandExecutor);       
            }    
            shops.remove(key);                    
        } else if(pendingShops.containsKey(key))
            pendingShops.remove(key);
        if(plugin.getCustomConfig().getEnableDynmapMarkers()) 
            player.sendMessage(ChatColor.GREEN + "Shop and Dynmap marker removed successfully!");        
        else player.getPlayer().sendMessage(ChatColor.GREEN + "Removed shop successfully");               
        saveShops();
        unlockShop(uuid);
    } 
    
    @Override 
    public void unlockShop(String uuid) {
        if(shopsAwaitingResponse.containsKey(uuid))
            shopsAwaitingResponse.remove(uuid);
        if(userModeratingShop.containsKey(uuid))
            userModeratingShop.remove(uuid);
    } 

    @Override 
    public boolean isShopLocked(String key) {
        return shopsAwaitingResponse.containsValue(key);
    }

    @Override
    public boolean hasUserLockedShop(String uuid) {
        return shopsAwaitingResponse.containsKey(uuid);
    }

    @Override
    public void lockShop(String uuid, String key) {
        shopsAwaitingResponse.put(uuid, key);
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
                            if(((item.item.getType() == Material.POTION && itemList.item.getType() == Material.POTION) || (item.item.getType() == Material.LINGERING_POTION && itemList.item.getType() == Material.LINGERING_POTION) || (item.item.getType() == Material.TIPPED_ARROW && itemList.item.getType() == Material.TIPPED_ARROW)) && ((PotionMeta)item.item.getItemMeta()).getBasePotionData().getType().ordinal() != ((PotionMeta)itemList.item.getItemMeta()).getBasePotionData().getType().ordinal())
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

class CommandExecutor implements Runnable {
    private String[] commands;
    private Player player;
    public CommandExecutor(Player player, String... commands) {
        this.commands = commands;
        this.player = player;
    }
    @Override
    public void run() {
        for(String command : commands) {
            Bukkit.getServer().dispatchCommand(player, command);
        }
    }
}