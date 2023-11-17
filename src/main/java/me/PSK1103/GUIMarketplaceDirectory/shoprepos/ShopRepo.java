package me.PSK1103.GUIMarketplaceDirectory.shoprepos;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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

    String addShop(String name, String desc, String owner, String uuid, String loc, String displayItem);

    String getOwner(String key);

    boolean getIsInitOwner(String uuid);

    void stopInitOwner(String uuid);

    void stopShopEdit(String uuid);

    void setDisplayItem(Player player, String materialName);
    void setLocation(Player player, String location);    
    void setDescription(Player player, String description);
    void addOwner(String uuid, OfflinePlayer player);

    int startSettingDescription(String uuid, String key);
    int startSettingLocation(String uuid, String key);
    int startAddingOwner(String uuid, String key);
    int startSettingDisplayItem(String uuid, String key);
    int startRemovingShop(String uuid, String key);

    boolean getIsEditingShop(String uuid, String key);
    boolean getIsAddingOwner(String key);
    boolean getIsUserAddingOwner(String uuid);    
    boolean isShopUnderEditOrAdd(String key);

    void saveShops();

    int initItemAddition(String uuid, String key, String name, ItemStack itemStack);

    void initShopOwnerAddition(String uuid);

    EditType getEditType(String uuid);

    void setQty(String qty, String uuid);

    void setPrice(int price, String uuid);

    boolean isAddingItem(String uuid);

    void stopEditing(String uuid);

    boolean isShopOwner(String uuid, String key);

    void approveChange(Player player, String uuid);
    void rejectChange(String uuid);
    boolean isChangeLocked(String key); 
    boolean isChangeLocked(String key, ModerationType kind);
    boolean hasUserLockedChanges(String uuid); 
    boolean hasUserLockedChanges(String uuid, ModerationType kind); 
    void unlockChange(String uuid);
    void lockChange(String uuid, String key, ModerationType kind); 

    void submitNewDescription(String uuid, String newDesc);
    void submitNewDisplayItem(String uuid, String newDisplayItem);
    void submitNewLocation(String uuid, String newLoc);
    void submitNewOwner(String uuid, String newUuid, String name);

    void cancelNewDescription(String uuid, String key);
    void cancelNewDisplayItem(String uuid, String key);
    void cancelNewLocation(String uuid, String key);
    void cancelNewOwner(String uuid, String key);

    void approveShop(Player player, String key);
    void removeShop(Player player, String uuid);
    void unlockShop(String uuid);
    boolean isShopLocked(String key);
    boolean hasUserLockedShop(String uuid);
    void lockShop(String uuid, String key);
    boolean isUserSettingMarkers(String uuid);
    void unlockSettingMarkers(String uuid);

    String addShopSet();
    String deleteShopSet();
    String addShopMarker(String key);
    String deleteShopMarker(String key);
    String appendShopMarkerDescription(String key);
    String resetShopMarkerDescription(String key);
    void addAllShopMarkers(Player player);
    void updateShopMarker(Player player);
    void initShopMarkerAddition(Player player);

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

    void lookupShop(Player player, String key);

    void lookupAllShops(Player player);
}
