package me.PSK1103.GUIMarketplaceDirectory.shoprepos;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.json.ItemList;
import java.util.List;
import java.util.Map;

public interface ShopRepo {
    public enum EditType {
        NOT_UNDER_ADD, NOT_UNDER_EDIT, ADD_OWNER, ADD_SHOP, SET_DISPLAY_ITEM, SET_DESCRIPTION, SHOP_OWNER_ADDITION, COREPROTECT_RADIUS, SET_LOCATION;
    }

    public enum ModerationType {
        APPROVE_CHANGE, REJECT_CHANGE, DELETE_SHOP, SET_MARKERS;
    }

    String addShopAsOwner(String name, String desc, String owner, String uuid, String loc, String displayItem);

    String getOwner(String key);

    boolean setDisplayItem(Player player, String shopKey, String materialName);
    boolean setLocation(Player player, String ShopKey, String location);    
    boolean setDescription(Player player, String key, String description);
    void addOwner(String shopKey, OfflinePlayer player);

    void saveShops();

    boolean addItemToShop(ItemList item, String shopkey);

    boolean isShopOwner(String uuid, String key);

    boolean approveChange(Player player, String shopKey);
    boolean rejectChange(String shopKey);

    void submitNewDescription(String uuid, String shopkey,  String newDesc);
    void submitNewDisplayItem(String uuid, String shopkey, String newDisplayItem);
    void submitNewLocation(String uuid, String shopKey, String newLoc);
    void submitNewOwner(String shopKey, String newUuid, String name);

    void cancelNewDescription(String uuid, String key);
    void cancelNewDisplayItem(String uuid, String key);
    void cancelNewLocation(String uuid, String key);
    void cancelNewOwner(String uuid, String key);

    void approveShop(Player player, String key);
    boolean removeShop(Player player, String shopKey);

    Map<String, String> getSpecificShopDetails(String key);
    Map<String, String> getSpecificChangeDetails(String key);
    List<Map<String, String>> getShopDetails();
    List<Map<String, String>> getPendingShopDetails();
    List<Map<String, String>> getPendingChangesDetails();

    List<Object> getShopInv(String key);

    void findBetterAlternative(Player player, String key, int pos);

    String getShopName(String key);

    List<Map<String, String>> getRefinedShopsByName(String searchKey);

    List<ItemStack> getMatchingItems(String key, String itemName);

    void removeMatchingItems(String key, String itemName);

    void removeItem(String key, ItemStack item);

    List<Map<String, String>> getRefinedShopsByPlayer(String searchKey);

    Map<String, Object> findItem(String searchKey);

    boolean shopExist(String shopKey);
}
