package me.PSK1103.GUIMarketplaceDirectory.shoprepos;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public interface ShopRepo {
    String addShopAsOwner(String name, String desc, String owner, String uuid, String loc, String displayItem);

    String addShop(String name, String desc, String owner, String uuid, String loc, String displayItem);

    String getOwner(String key);

    boolean getIsInitOwner(String uuid);

    void stopInitOwner(String uuid);

    int startAddingOwner(String uuid, String key);

    int startSettingDisplayItem(String uuid, String key);

    int startRemovingShop(String uuid, String key);

    boolean getIsEditingShop(String uuid, String key);

    boolean getIsAddingOwner(String key);

    boolean getIsUserAddingOwner(String uuid);

    void addOwner(String uuid, OfflinePlayer player);

    void setDisplayItem(String uuid, String materialName);

    void saveShops();

    boolean isShopUnderEditOrAdd(String key);

    int initItemAddition(String uuid, String key, String name, ItemStack itemStack);

    void initShopOwnerAddition(String uuid);

    int getEditType(String uuid);

    void setQty(String qty, String uuid);

    void setPrice(int price, String uuid);

    boolean isAddingItem(String uuid);

    void stopEditing(String uuid);

    boolean isShopOwner(String uuid, String key);

    void approveShop(String key);

    void rejectShop(String uuid);

    void cancelRejectShop(String uuid);

    boolean isShopRejecting(String key);

    boolean isUserRejectingShop(String uuid);

    void addShopToRejectQueue(String uuid, String key);

    void removeShop(String uuid);

    void cancelRemoveShop(String uuid);

    boolean isShopRemoving(String key);

    boolean isUserRemovingShop(String uuid);

    void addShopToRemoveQueue(String uuid, String key);

    List<Map<String, String>> getShopDetails();

    List<Map<String, String>> getPendingShopDetails();

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
