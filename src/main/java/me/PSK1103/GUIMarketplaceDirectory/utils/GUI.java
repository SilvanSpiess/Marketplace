package me.PSK1103.GUIMarketplaceDirectory.utils;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.invholders.InvType;
import me.PSK1103.GUIMarketplaceDirectory.invholders.MarketplaceBookHolder;
import me.PSK1103.GUIMarketplaceDirectory.invholders.ShopInvHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.ChatPaginator;

import java.util.*;
import java.util.logging.Logger;

/*
 * we skimmed down GUI.java from 533 lines to 442 by making 2 functions that handle repeating logic.
 */

public class GUI {
    private final GUIMarketplaceDirectory plugin;
    private final HashMap<String,String> colors;
    private final Logger logger;

    public GUI(GUIMarketplaceDirectory plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger();
        colors = new HashMap<>();
        colors.put("name",plugin.getCustomConfig().getDefaultShopNameColor());
        colors.put("desc",plugin.getCustomConfig().getDefaultShopDescColor());
        colors.put("owner",plugin.getCustomConfig().getDefaultShopOwnerColor());
        colors.put("loc",plugin.getCustomConfig().getDefaultShopLocColor());
        colors.put("u-loc",plugin.getCustomConfig().getDefaultShopULocColor());
        colors.put("dynmap",plugin.getCustomConfig().getDefaultShopDynmapColor());
    }

    /*
     * checks if the player sent a confirmation message
     */
    public void sendConfirmationMessage(Player player, String msg) {
        Component yes = Component.text(ChatColor.GOLD + "" + ChatColor.BOLD + "Y").clickEvent(net.kyori.adventure.text.event.ClickEvent.clickEvent(net.kyori.adventure.text.event.ClickEvent.Action.RUN_COMMAND,"Y"));
        Component no = Component.text(ChatColor.GOLD + "" + ChatColor.BOLD + "N").clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND,"N"));
        player.sendMessage(Component.text(msg + " (").color(NamedTextColor.YELLOW).append(yes).append(Component.text("/")).append(no).append(Component.text(")")).color(NamedTextColor.YELLOW));
    }

    private ItemStack makeShopDisplayItem(Map<String, String> shop, Component... clicks) {
        ItemStack shopItem;
        try {
            shopItem = new ItemStack(Material.getMaterial(shop.get("displayItem")));
        }
        catch (Exception e) {
            shopItem = new ItemStack(Material.WRITTEN_BOOK);
        }
        //Adds lore (info) to the shop display items
        ItemMeta shopMeta = shopItem.getItemMeta();
        shopMeta.displayName(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("name") + shop.get("name"))));
        List<String> l = new ArrayList<>(Arrays.asList(ChatPaginator.wordWrap(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("desc") + shop.get("desc")),30)));
        List<Component> lore = new ArrayList<>();
        l.forEach(s -> lore.add(Component.text(s)));
        String[] parts = shop.get("loc").split(",");
        if(Integer.parseInt(parts[1]) < plugin.getCustomConfig().getMaxUndergroundMarketLevel() && parts.length >= 3) {
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("loc") + parts[0] + ", " + colors.get("u-loc") + parts[1] + ", " + colors.get("loc") + parts[2])));    
        }
        else {
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("loc") + shop.get("loc"))));
        }
        lore.add(0, Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("owner") + shop.get("owners"))));
        for (Component click: clicks) {
            lore.add(click);
        }
        shopMeta.lore(lore);
        shopMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        shopMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        shopItem.setItemMeta(shopMeta);
        return shopItem;
    }

    /*
     * Opens the directory of all/ the selected shops
     */
    public void openShopDirectory(Player player) {
        List<Map<String,String>> shops = plugin.getShopRepo().getShopDetails();
        //Creates the directory of all the selected shops (first page)
        Inventory shopDirectory = Bukkit.createInventory(new MarketplaceBookHolder(shops), Math.min(9*(shops.size()/9 + (shops.size()%9 == 0 ? 0 : 1)),54) + (shops.size() == 0 ? 9 : 0), Component.text("Marketplace Directory"));
        for(int i=0;i<(shops.size() > 54 ? 45 : shops.size());i++) {
            ItemStack shopItem = makeShopDisplayItem(shops.get(i), Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("dynmap") + "§oRight click to see this shop on Dynmap")));
            shopDirectory.setItem(i,shopItem);
        }
        if(shops.size() > 54) {
            ((MarketplaceBookHolder) shopDirectory.getHolder()).setPaged();
            //nextPage button
            ItemStack nextPage = makeDisplayItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Next Page"));
            shopDirectory.setItem(50,nextPage);
            //prevPage button
            ItemStack prevPage = makeDisplayItem(Material.BARRIER, Component.text("Previous Page"));
            shopDirectory.setItem(48,prevPage);
            //namPage display
            ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page 1"));
            shopDirectory.setItem(49,pageNum);
        }
        player.openInventory(shopDirectory);
    }

    private ItemStack addItemLore(ItemStack item, Component... lore) {        
        ItemMeta itemMeta = item.getItemMeta();
        List<Component> allLore = itemMeta.lore();
        allLore.addAll(Arrays.asList(lore));
        itemMeta.lore(allLore);
        item.setItemMeta(itemMeta);
        return item;
    }

    /*
     * Opens the inventory of the selected shop
     */
    public void openShopInventory(Player player, String key,String name,InvType type) {
        List<Object> res = plugin.getShopRepo().getShopInv(key);
        List<ItemStack> inv = (List<ItemStack>) res.get(0);
        List<Integer> itemIds = (List<Integer>) res.get(1);
        //Adding lore to all the items in this the shop
        inv.forEach(item -> {
            if(type == InvType.INV_EDIT) {
                addItemLore(item, Component.text(ChatColor.GOLD + "§oRight click to find a better deal"), 
                                  Component.text(ChatColor.RED + "Shift click to delete this item"));
            }
            else addItemLore(item, Component.text(ChatColor.GOLD + "§oRight click to find a better deal"));
        });
        //Creates the inventory of a shop with the items listed (first page)
        Inventory shopInventory;
        if(plugin.getShopRepo().getPendingShopDetails().stream().map(m->m.get("key").equals(key)).reduce(false, (x, y) -> x || y)) {
            shopInventory = Bukkit.createInventory(new ShopInvHolder(key,type,inv, itemIds),Math.min(9*(inv.size()/9),45) + 9, Component.text(name+" §5§o(pending)"));
        }
        else shopInventory = Bukkit.createInventory(new ShopInvHolder(key,type,inv, itemIds),Math.min(9*(inv.size()/9),45) + 9, Component.text(name));
        for(int i=0;i<Math.min(inv.size(),45);i++) {            
            shopInventory.setItem(i,inv.get(i));            
        }
        if(inv.size() == 0) {
            //empty shop display
            ItemStack empty = makeDisplayItem(Material.BARRIER, Component.text(ChatColor.RED + "This shop is empty!"));
            shopInventory.setItem(4,empty);
        }
        //goBack button
        ItemStack back = makeDisplayItem(Material.ARROW, Component.text(ChatColor.YELLOW + "Go Back"));
        shopInventory.setItem(Math.min(9*(inv.size()/9),45) + 8,back);
        if(inv.size()>45) {
            ((ShopInvHolder) shopInventory.getHolder()).setPaged();
            //nextPage button
            ItemStack nextPage = makeDisplayItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Next Page"));
            shopInventory.setItem(50,nextPage);
            //prevPage button
            ItemStack prevPage = makeDisplayItem(Material.BARRIER, Component.text("Previous Page"));
            shopInventory.setItem(48,prevPage);
            //numPage display
            ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page 1"));
            shopInventory.setItem(49,pageNum);
        }
        player.openInventory(shopInventory);
    }
    
    //calls updateInvPage to load next inventory page
    public void nextInvPage(Player player, int currPage) {
        updateInvPage(player, currPage);
    }

    //calls updateInvPage to load previous inventory page
    public void prevInvPage(Player player, int currPage) {
        currPage-=2;
        updateInvPage(player, currPage);
    }

    /*
     * loads the next OR previous page of the inventory of a shop
     */
    public void updateInvPage(Player player, int currPage) {        
        //Creates the inventory of a shop with the items listed (any page)
        Inventory pageInv = player.getOpenInventory().getTopInventory();
        ShopInvHolder holder = (ShopInvHolder) pageInv.getHolder();
        List<ItemStack> inv = holder.getInv();
        
        //int type = holder.getType();
        pageInv.clear();
        for(int i=0;i<Math.min(inv.size(),(currPage+1)*45)-currPage*45;i++) {
            pageInv.setItem(i,inv.get(i+currPage*45));
        }
        //nextPage button
        //ItemStack nextPage = makeDisplayItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Next Page"));
        ItemStack nextPage = inv.size() > (currPage+1)*45 ? makeDisplayItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Next Page")): makeDisplayItem(Material.BARRIER, Component.text("Next Page"));  
        pageInv.setItem(50,nextPage);
        //prevPage button
        //ItemStack prevPage = makeDisplayItem(Material.ORANGE_STAINED_GLASS_PANE, Component.text("Previous Page"));
        ItemStack prevPage = currPage > 0 ? makeDisplayItem(Material.ORANGE_STAINED_GLASS_PANE, Component.text("Previous Page")) : makeDisplayItem(Material.BARRIER, Component.text("Previous Page"));
        pageInv.setItem(48,prevPage);
        //currPage display
        ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page " + (currPage+1)));
        pageInv.setItem(49,pageNum);
        //goBack Button
        ItemStack back = makeDisplayItem(Material.ARROW, Component.text(ChatColor.YELLOW + "Go Back"));
        pageInv.setItem(53,back);
        player.updateInventory();
    }

    /*
     * function that makes an ItemStack displayItem, which makes it ready to be added as DiplayItem in calling function
     */
    private ItemStack makeDisplayItem(Material material, Component displayName){
        ItemStack displayItem = new ItemStack(material);
        ItemMeta meta = displayItem.getItemMeta();
        meta.displayName(displayName);     
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    /*
     * loads the next page of shops in the inventory
     */
    public void updatePage(Player player, int page) {
        //Creates the inventory of a certain page with the shops listed
        Inventory pageInv = player.getOpenInventory().getTopInventory();
        MarketplaceBookHolder holder = (MarketplaceBookHolder) pageInv.getHolder();
        List<Map<String,String>> shops = holder.getShops();
        InvType type = holder.getType();
        pageInv.clear();
        for(int i=0;i<Math.min(shops.size(),(page+1)*45)-page*45;i++) {
            ItemStack shopItem;
            if(type == InvType.NORMAL) {
                shopItem = makeShopDisplayItem(shops.get(i+page*45), Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("dynmap") + "§oRight click to see this shop on Dynmap")));
            }else if(type == InvType.PENDING) {
                shopItem = makeShopDisplayItem(shops.get(i+page*45), Component.text(ChatColor.AQUA + "Shift click to view"),
                                                                     Component.text(ChatColor.GREEN + "Right click to approve"), 
                                                                     Component.text(ChatColor.RED + "Left click to reject"));
            } else if (type == InvType.REVIEW) {
                shopItem = makeShopDisplayItem(shops.get(i+page*45), Component.text(ChatColor.RED + "Right click to delete"));
            } else if(type == InvType.RECOVER) {
                shopItem = makeShopDisplayItem(shops.get(i+page*45), Component.text(ChatColor.AQUA + "Right click to recover"));
            } else {
                shopItem = makeShopDisplayItem(shops.get(i+page*45));
            }
            pageInv.setItem(i,shopItem);
        }
        //nextPage button
        ItemStack nextPage = shops.size() > (page+1)*45 ? makeDisplayItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Next Page")): makeDisplayItem(Material.BARRIER, Component.text("Next Page"));
        pageInv.setItem(50,nextPage);
        //prevPage button
        ItemStack prevPage = page > 0 ? makeDisplayItem(Material.ORANGE_STAINED_GLASS_PANE, Component.text("Previous Page")) : makeDisplayItem(Material.BARRIER, Component.text("Previous Page"));
        pageInv.setItem(48,prevPage);
        //currPage display
        ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page " + (page+1)));
        pageInv.setItem(49,pageNum);
        player.updateInventory();
    }

    /*
     * loads the next page of shops in the inventory
     */
    public void nextPage(Player player, int currPage) {
        //Creates the inventory of the next page with the shops listed
        updatePage(player, currPage);
    }

    /*
     * loads the previous page of shops in the inventory
     */
    public void prevPage(Player player, int currPage) {
        updatePage(player, currPage-2);
    }

    /*
    * With type 6, the searched shops are being listed in the inventory
    */
    public void openRefinedShopPageByName(Player player,String searchKey) {

        List<Map<String,String>> refinedShops = plugin.getShopRepo().getRefinedShopsByName(searchKey);
        //Checks if any shops were found
        if(refinedShops.size() == 0) {
            player.sendMessage(ChatColor.RED + "No shops with matching name found");
            return;
        }
        //Creates the inventory with the shops listed
        Inventory refinedShopDirectory = Bukkit.createInventory(new MarketplaceBookHolder(refinedShops),Math.min(9*(refinedShops.size()/9 + (refinedShops.size() % 9 == 0 ? 0 : 1)),54), Component.text("Search results"));
        for(int i=0;i< (Math.min(refinedShops.size(), 54));i++) {
            ItemStack shopItem = makeShopDisplayItem(refinedShops.get(i), Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("dynmap") + "§oRight click to see this shop on Dynmap")));
            refinedShopDirectory.setItem(i,shopItem);
        }
        player.openInventory(refinedShopDirectory);
    }

    /*
    * With type 6, the shops owned by a player are being listed in the inventory
    */
    public void openRefinedShopPageByPlayer(Player player,String searchKey) {

        List<Map<String,String>> refinedShops = plugin.getShopRepo().getRefinedShopsByPlayer(searchKey);
        //Checks if any players with shops in the directory were found
        if(refinedShops.size() == 0) {
            player.sendMessage(ChatColor.RED + "No shops with matching name found");
            return;
        }
        //Creates the inventory with the shops listed
        Inventory refinedShopDirectory = Bukkit.createInventory(new MarketplaceBookHolder(refinedShops),Math.min(9*(refinedShops.size()/9 + (refinedShops.size() % 9 == 0 ? 0 : 1)),54), Component.text("Search results"));
        for(int i=0;i< (Math.min(refinedShops.size(), 54));i++) {
            ItemStack shopItem = makeShopDisplayItem(refinedShops.get(i), Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("dynmap") + "§oRight click to see this shop on Dynmap")));
            refinedShopDirectory.setItem(i,shopItem);
        }
        player.openInventory(refinedShopDirectory);
    }

    /*
     * With type 6, searched items get listed in the inventory
     */
    public void openRefinedItemInventory(Player player, String searchKey) {
        Map<String,Object> searchResults = plugin.getShopRepo().findItem(searchKey);
        List<ItemStack> refinedItems = (List<ItemStack>) searchResults.get("items");
        List<Map<String,String>> shops = (List<Map<String,String>>) searchResults.get("shops");
        if(refinedItems.size() == 0) {
            player.sendMessage(ChatColor.RED + "No items with matching name found");
            return;
        }
        Inventory refinedItemInv = Bukkit.createInventory(new ShopInvHolder("", InvType.SEARCH, refinedItems,null, searchKey).setShops(shops),Math.min(9*(refinedItems.size()/9 + ((refinedItems.size()%9) == 0 ? 0 : 1)),54), Component.text("Search results"));
        for(int i=0;i<Math.min(refinedItems.size(),45);i++) {
            refinedItemInv.setItem(i,refinedItems.get(i));
        }
        if(refinedItems.size()>45) {
            ((ShopInvHolder) refinedItemInv.getHolder()).setPaged();
            //nextPage button
            ItemStack nextPage = makeDisplayItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Next Page"));
            refinedItemInv.setItem(50,nextPage);
            //prevPage button
            ItemStack prevPage = makeDisplayItem(Material.BARRIER, Component.text("Previous Page"));
            refinedItemInv.setItem(48,prevPage);
            //numPage display
            ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page 1"));
            refinedItemInv.setItem(49,pageNum);
        }
        player.openInventory(refinedItemInv);
    }

    /*
     * Opens the Shop directory, with different lore/funcionality added to the shopItems, depending on the command used
     * - (type 1) pending -> shows all pending shops
     * - (type 2) review -> removes shops
     * - (type 3) recover -> recovers shopOwner book
     * - (type 4) lookup -> coreprotect -> ShopEvents.java ln 348
     * - (type 5) seems to be inactive -> ShopEvents.java ln 365
     */
    public void openShopDirectoryModerator(Player moderator,InvType type) {
        List<Map<String,String>> shops = type == InvType.PENDING ? plugin.getShopRepo().getPendingShopDetails() : plugin.getShopRepo().getShopDetails();
        Inventory shopDirectory = Bukkit.createInventory(new MarketplaceBookHolder(shops, type), Math.min(9*(shops.size()/9 + (shops.size()%9 == 0 ? 0 : 1)),54) + (shops.size() == 0 ? 9 : 0), Component.text("Marketplace Directory"));
        for(int i=0;i<(shops.size() > 54 ? 45 : shops.size());i++) {
            ItemStack shopItem;
            if(type == InvType.PENDING) {
                shopItem = makeShopDisplayItem(shops.get(i), Component.text(ChatColor.AQUA + "Shift click to view"), 
                                                             Component.text(ChatColor.GREEN + "Right click to approve"), 
                                                             Component.text(ChatColor.RED + "Left click to reject"));
            }
            else if (type == InvType.REVIEW) {
                shopItem = makeShopDisplayItem(shops.get(i), Component.text(ChatColor.RED + "Right click to remove"));
            }
            else if(type == InvType.RECOVER) {
                shopItem = makeShopDisplayItem(shops.get(i), Component.text(ChatColor.AQUA + "Right click to recover")); //Maybe send recovering message in chat, to notify player
            }
            else if(type == InvType.LOOKUP) {
                shopItem = makeShopDisplayItem(shops.get(i), Component.text(ChatColor.AQUA + "Right click to check activity")); //confusing, todo later
            }
            else if(type == InvType.ADD_ITEM) {
                shopItem = makeShopDisplayItem(shops.get(i), Component.text(ChatColor.AQUA + "Right click to set lookup radius")); //confusing, todo later
            } else {
                shopItem = makeShopDisplayItem(shops.get(i));
            }
            shopDirectory.setItem(i,shopItem);
        }

        //if there are more than 54 pending shops, the shops get put on additional pages
        if(shops.size() > 54) {
            ((MarketplaceBookHolder) shopDirectory.getHolder()).setPaged();
            //nextPage button
            ItemStack nextPage = makeDisplayItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Next Page"));
            shopDirectory.setItem(50,nextPage);
            //prevPage button
            ItemStack prevPage = makeDisplayItem(Material.BARRIER, Component.text("Previous Page"));
            shopDirectory.setItem(48,prevPage);
            //currPage display
            ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page 1"));
            shopDirectory.setItem(49,pageNum);
        }
        if(shops.size() == 0) {
            logger.info("No pending shop");
        }
        moderator.openInventory(shopDirectory);
    }

    /*
     * Opens the inventory, that lets the shopownwer do the following:
     * - add another owner
     * - set a new display item
     * - delete the shop
     */
    public void openShopEditMenu(Player player, String key) {
        String name = plugin.getShopRepo().getShopName(key);        
        //creates the inventory for this menu
        ShopInvHolder currentShopView = new ShopInvHolder(key, InvType.SHOP_MENU, null, null);
        //Adds this shop to a list with one entry, to open its inventory if rawSloth 4 is clicked
        List<Map<String,String>> shop = new LinkedList<>();
        shop.add(plugin.getShopRepo().getSpecificShopDetails(key));
        currentShopView.setShops(shop);
        Inventory shopEditMenuInv = Bukkit.createInventory(currentShopView,18, Component.text(name));
        //setDescription button
        ItemStack setDescription = makeDisplayItem(Material.PAPER, Component.text(ChatColor.GOLD + "" + ChatColor.ITALIC + "Set description"));
        shopEditMenuInv.setItem(1,setDescription);
        //see shop button
        ItemStack seeShop = makeShopDisplayItem(plugin.getShopRepo().getSpecificShopDetails(key), 
                                                Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("dynmap") + "§oRight click to see this shop on Dynmap")));
        shopEditMenuInv.setItem(4,seeShop);
        //setDescription button
        ItemStack setLocation = makeDisplayItem(Material.COMPASS, Component.text(ChatColor.GOLD + "" + ChatColor.ITALIC + "Set location"));
        shopEditMenuInv.setItem(7,setLocation);
        //setDisplayItem button
        ItemStack setDisplayItem = makeDisplayItem(Material.WRITABLE_BOOK, Component.text(ChatColor.GOLD + "" + ChatColor.ITALIC + "Set display item"));
        shopEditMenuInv.setItem(10,setDisplayItem);
        //addOwner button
        ItemStack addOwner = makeDisplayItem(Material.BEACON, Component.text(ChatColor.GOLD + "" + ChatColor.ITALIC + "Add owner"));
        shopEditMenuInv.setItem(13,addOwner);
        //removeShop button
        ItemStack removeShop = makeDisplayItem(Material.FLINT_AND_STEEL, Component.text(ChatColor.RED + "" + ChatColor.ITALIC + "Delete shop"));
        shopEditMenuInv.setItem(16,removeShop);
        player.openInventory(shopEditMenuInv);
    }

    /*
     * For adding same item that already exists in shop, show ItemAddMenu
     */
    public void openItemAddMenu(Player player, String key, List<ItemStack> matchingItems, ItemStack itemToAdd) {
        //creates the inventory for this menu, adjustable to the amount of same items in the shop
        Inventory itemAddMenuInv = Bukkit.createInventory(new ShopInvHolder(key, itemToAdd.clone(), InvType.ADD_ITEM),Math.min(54,9 + 9*(matchingItems.size()/9 + (matchingItems.size()%9 == 0 ? 0 : 1))),Component.text("Adding Item..."));
        for(int i = 0;i<Math.min(matchingItems.size(),45);i++) {
            ItemStack iTA = matchingItems.get(i).clone();
            ItemMeta meta = iTA.getItemMeta();
            List<Component> lore = meta.lore();
            if(lore!=null)
                lore.add(Component.text(ChatColor.RED + "Right click to remove"));
            meta.lore(lore);
            iTA.setItemMeta(meta);
            itemAddMenuInv.setItem(i,iTA);
        }
        //addItem button
        ItemStack addItem = makeDisplayItem(Material.ENDER_PEARL, Component.text(ChatColor.GREEN + "" + ChatColor.ITALIC + "Add item"));
        itemAddMenuInv.setItem(itemAddMenuInv.getSize()-7,addItem);
        //removeAllItems button
        ItemStack removeAllItems = makeDisplayItem(Material.FLINT_AND_STEEL, Component.text(ChatColor.RED + "" + ChatColor.ITALIC + "Remove all items"));
        itemAddMenuInv.setItem(itemAddMenuInv.getSize()-3,removeAllItems);
        player.openInventory(itemAddMenuInv);
    }
}