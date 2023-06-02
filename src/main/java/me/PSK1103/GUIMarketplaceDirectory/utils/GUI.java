package me.PSK1103.GUIMarketplaceDirectory.utils;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
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

    /*
     * Opens the directory of all/ the selected shops
     */
    public void openShopDirectory(Player player) {
        List<Map<String,String>> shops = plugin.getShopRepo().getShopDetails();
        //Creates the directory of a all/ the selected shops (first page)
        Inventory shopDirectory = Bukkit.createInventory(new MarketplaceBookHolder(shops), Math.min(9*(shops.size()/9 + (shops.size()%9 == 0 ? 0 : 1)),54) + (shops.size() == 0 ? 9 : 0), Component.text("Marketplace Directory"));
        for(int i=0;i<(shops.size() > 54 ? 45 : shops.size());i++) {
            ItemStack shopItem;
            try {
                shopItem = new ItemStack(Material.getMaterial(shops.get(i).get("displayItem")));
            }
            catch (Exception e) {
                shopItem = new ItemStack(Material.WRITTEN_BOOK);
            }
            //Adds lore (info) to the shop display items
            ItemMeta shopMeta = shopItem.getItemMeta();
            shopMeta.displayName(Component.text(shops.get(i).get("name").contains("&") ? ChatColor.translateAlternateColorCodes('&',shops.get(i).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("name") + shops.get(i).get("name"))));
            List<String> l = new ArrayList<>(Arrays.asList(ChatPaginator.wordWrap(shops.get(i).get("desc").contains("&") ? ChatColor.translateAlternateColorCodes('&',shops.get(i).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("desc") + shops.get(i).get("desc")),30)));
            List<Component> lore = new ArrayList<>();
            l.forEach(s -> lore.add(Component.text(s)));
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("loc") + shops.get(i).get("loc"))));
            lore.add(0, Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("owner") + shops.get(i).get("owners"))));
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("dynmap") + "§oRight click to see this shop on Dynmap")));
            shopMeta.lore(lore);
            shopMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            shopItem.setItemMeta(shopMeta);
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
            shopDirectory.setItem(45,pageNum);
        }
        player.openInventory(shopDirectory);
    }

    /*
     * Opens the inventory of the selected shop
     */
    public void openShopInventory(Player player, String key,String name,int type) {

        List<Object> res = plugin.getShopRepo().getShopInv(key);
        List<ItemStack> inv = (List<ItemStack>) res.get(0);
        List<Integer> itemIds = (List<Integer>) res.get(1);

        //Creates the inventory of a shop with the items listed (first page)
        Inventory shopInventory = Bukkit.createInventory(new ShopInvHolder(key,type,inv, itemIds),Math.min(9*(inv.size()/9),45) + 9, Component.text(name));
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
            shopInventory.setItem(45,pageNum);
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
     * loads the next AND previous page of the inventory of a shop
     */
    public void updateInvPage(Player player, int currPage) {
        //Creates the inventory of a shop with the items listed (any page)
        Inventory pageInv = player.getOpenInventory().getTopInventory();
        ShopInvHolder holder = (ShopInvHolder) pageInv.getHolder();
        List<ItemStack> inv = holder.getInv();
        int type = holder.getType();
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
        pageInv.setItem(45,pageNum);
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
    public void nextPage(Player player, int currPage) {
        //Creates the inventory of the next page with the shops listed
        Inventory nextPageInv = player.getOpenInventory().getTopInventory();
        MarketplaceBookHolder holder = (MarketplaceBookHolder) nextPageInv.getHolder();
        List<Map<String,String>> shops = holder.getShops();
        int type = holder.getType();
        nextPageInv.clear();
        for(int i=0;i<Math.min(shops.size(),(currPage+1)*45)-currPage*45;i++) {
            ItemStack shopItem;
            try {
                shopItem = new ItemStack(Material.getMaterial(shops.get(i+currPage*45).get("displayItem")));
            }
            catch (Exception e) {
                shopItem = new ItemStack(Material.WRITTEN_BOOK);
            }
            //Adds general descriptive text to each shop
            ItemMeta shopMeta = shopItem.getItemMeta();
            shopMeta.displayName(Component.text(shops.get(i+currPage*45).get("name").contains("&") ? ChatColor.translateAlternateColorCodes('&',shops.get(i+currPage*45).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("name") +  shops.get(i+currPage*45).get("name"))));
            List<String> l = new ArrayList<>(Arrays.asList(ChatPaginator.wordWrap(shops.get(i+currPage*45).get("desc").contains("&") ? ChatColor.translateAlternateColorCodes('&',shops.get(i+currPage*45).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("desc") +  shops.get(i+currPage*45).get("desc")),30)));
            List<Component> lore = new ArrayList<>();
            l.forEach(s -> lore.add(Component.text(s)));
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("loc") +  shops.get(i+currPage*45).get("loc"))));
            lore.add(0, Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("owner") +  shops.get(i+currPage*45).get("owners"))));
            //adds different lore, based on the types:
            // - (type 0) normal -> shows all shops
            // - (type 1) pending -> shows all pending shops
            // - (type 2) review -> removes shops
            // - (type 3) recover -> recovers shopOwner book
            if(type == 0) {
                lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("dynmap") + "§oRight click to see this shop on Dynmap")));
            }else if(type == 1) {
                lore.add(Component.text(ChatColor.GREEN + "Right click to approve"));
                lore.add(Component.text(ChatColor.RED + "Left click to reject"));
            } else if (type == 2) {
                lore.add(Component.text(ChatColor.RED + "Right click to delete"));
            } else if(type == 3) {
                lore.add(Component.text(ChatColor.AQUA + "Right click to recover"));
            }
            shopMeta.lore(lore);
            shopMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            shopItem.setItemMeta(shopMeta);
            nextPageInv.setItem(i,shopItem);
        }
        //nextPage button
        ItemStack nextPage = shops.size() > (currPage+1)*45 ? makeDisplayItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Next Page")): makeDisplayItem(Material.BARRIER, Component.text("Next Page"));
        nextPageInv.setItem(50,nextPage);
        //prevPage button
        ItemStack prevPage = currPage > 0 ? makeDisplayItem(Material.ORANGE_STAINED_GLASS_PANE, Component.text("Previous Page")) : makeDisplayItem(Material.BARRIER, Component.text("Previous Page"));
        nextPageInv.setItem(48,prevPage);
        //currPage display
        ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page " + (currPage+1)));
        nextPageInv.setItem(45,pageNum);
        player.updateInventory();
    }

    /*
     * loads the previous page of shops in the inventory
     */
    public void prevPage(Player player, int currPage) {
        currPage-=2;
        //Creates the inventory of the previous page with the shops listed
        Inventory prevPageInv = player.getOpenInventory().getTopInventory();
        MarketplaceBookHolder holder = (MarketplaceBookHolder) prevPageInv.getHolder();
        List<Map<String,String>> shops = holder.getShops();
        int type = holder.getType();
        prevPageInv.clear();
        for(int i=0;i < 45;i++) {
            ItemStack shopItem;
            try {
                shopItem = new ItemStack(Material.getMaterial(shops.get(i+currPage*45).get("displayItem")));
            }
            catch (Exception e) {
                shopItem = new ItemStack(Material.WRITTEN_BOOK);
            }
            //Adds general descriptive text to each shop
            ItemMeta shopMeta = shopItem.getItemMeta();
            shopMeta.displayName(Component.text(shops.get(i+currPage*45).get("name").contains("&") ? ChatColor.translateAlternateColorCodes('&',shops.get(i+currPage*45).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("name") +  shops.get(i+currPage*45).get("name"))));
            List<String> l = new ArrayList<>(Arrays.asList(ChatPaginator.wordWrap(shops.get(i+currPage*45).get("desc").contains("&") ? ChatColor.translateAlternateColorCodes('&',shops.get(i+currPage*45).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("desc") +  shops.get(i+currPage*45).get("desc")),30)));
            List<Component> lore = new ArrayList<>();
            l.forEach(s -> lore.add(Component.text(s)));
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("loc") +  shops.get(i+currPage*45).get("loc"))));
            lore.add(0, Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("owner") +  shops.get(i+currPage*45).get("owners"))));
            //adds different lore, based on the types:
            // - (type 0) normal -> shows all shops
            // - (type 1) pending -> shows all pending shops
            // - (type 2) review -> removes shops
            // - (type 3) recover -> recovers shopOwner book
            if(type == 0) {
                lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("dynmap") + "§oRight click to see this shop on Dynmap")));
            }else if(type == 1) {
                lore.add(Component.text(ChatColor.GREEN + "Right click to approve"));
                lore.add(Component.text(ChatColor.RED + "Left click to reject"));
            } else if(type == 2) {
                lore.add(Component.text(ChatColor.RED + "Right click to delete"));
            } else if(type == 3) {
                lore.add(Component.text(ChatColor.AQUA + "Right click to recover"));
            }
            shopMeta.lore(lore);
            shopMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            shopItem.setItemMeta(shopMeta);
            prevPageInv.setItem(i,shopItem);
        }
        //nextPage button
        ItemStack nextPage = shops.size() > (currPage+1)*45 ? makeDisplayItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Next Page")): makeDisplayItem(Material.BARRIER, Component.text("Next Page"));
        prevPageInv.setItem(50,nextPage);
        //prevPage button
        ItemStack prevPage = currPage > 0 ? makeDisplayItem(Material.ORANGE_STAINED_GLASS_PANE, Component.text("Previous Page")) : makeDisplayItem(Material.BARRIER, Component.text("Previous Page"));
        prevPageInv.setItem(48,prevPage);
        //currPage display
        ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page " + (currPage+1)));
        prevPageInv.setItem(45,pageNum);
        player.updateInventory();
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
            ItemStack shopItem;
            try {
                shopItem = new ItemStack(Material.getMaterial(refinedShops.get(i).get("displayItem")));
            }
            catch (Exception e) {
                shopItem = new ItemStack(Material.WRITTEN_BOOK);
            }
            //Adds the lore (info) about the shop to be displayed
            ItemMeta shopMeta = shopItem.getItemMeta();
            shopMeta.displayName(Component.text(refinedShops.get(i).get("name").contains("&") ? ChatColor.translateAlternateColorCodes('&',refinedShops.get(i).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("name") + refinedShops.get(i).get("name"))));
            List<String> l = new ArrayList<>(Arrays.asList(ChatPaginator.wordWrap(refinedShops.get(i).get("desc").contains("&") ? ChatColor.translateAlternateColorCodes('&',refinedShops.get(i).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("desc") + refinedShops.get(i).get("desc")),30)));
            List<Component> lore = new ArrayList<>();
            l.forEach(s -> lore.add(Component.text(s)));
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("loc") + refinedShops.get(i).get("loc"))));
            lore.add(0, Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("owner") + refinedShops.get(i).get("owners"))));
            shopMeta.lore(lore);
            shopMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            shopItem.setItemMeta(shopMeta);
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
            ItemStack shopItem;
            try {
                shopItem = new ItemStack(Material.getMaterial(refinedShops.get(i).get("displayItem")));
            }
            catch (Exception e) {
                shopItem = new ItemStack(Material.WRITTEN_BOOK);
            }
            //Adds the lore (info) about the shop to be displayed
            ItemMeta shopMeta = shopItem.getItemMeta();
            shopMeta.displayName(Component.text(refinedShops.get(i).get("name").contains("&") ? ChatColor.translateAlternateColorCodes('&',refinedShops.get(i).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("name") + refinedShops.get(i).get("name"))));
            List<String> l = new ArrayList<>(Arrays.asList(ChatPaginator.wordWrap(refinedShops.get(i).get("desc").contains("&") ? ChatColor.translateAlternateColorCodes('&',refinedShops.get(i).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("desc") + refinedShops.get(i).get("desc")),30)));
            List<Component> lore = new ArrayList<>();
            l.forEach(s -> lore.add(Component.text(s)));
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("loc") + refinedShops.get(i).get("loc"))));
            lore.add(0, Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("owner") + refinedShops.get(i).get("owners"))));
            shopMeta.lore(lore);
            shopMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            shopItem.setItemMeta(shopMeta);
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
        Inventory refinedItemInv = Bukkit.createInventory(new ShopInvHolder("",6,null, null).setShops(shops),Math.min(9*(refinedItems.size()/9 + ((refinedItems.size()%9) == 0 ? 0 : 1)),54), Component.text("Search results"));
        for(int i=0;i<Math.min(refinedItems.size(),54);i++) {
            refinedItemInv.setItem(i,refinedItems.get(i));
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
    public void openShopDirectoryModerator(Player moderator,int type) {
        List<Map<String,String>> shops = type == 1 ? plugin.getShopRepo().getPendingShopDetails() : plugin.getShopRepo().getShopDetails();
        Inventory shopDirectory = Bukkit.createInventory(new MarketplaceBookHolder(shops,type), Math.min(9*(shops.size()/9 + (shops.size()%9 == 0 ? 0 : 1)),54) + (shops.size() == 0 ? 9 : 0), Component.text("Marketplace Directory"));
        for(int i=0;i<(shops.size() > 54 ? 45 : shops.size());i++) {
            ItemStack shopItem;
            try {
                shopItem = new ItemStack(Material.getMaterial(shops.get(i).get("displayItem")));
            }
            catch (Exception e) {
                shopItem = new ItemStack(Material.WRITTEN_BOOK);
            }
            ItemMeta shopMeta = shopItem.getItemMeta();
            shopMeta.setDisplayName(shops.get(i).get("name").contains("&") ? ChatColor.translateAlternateColorCodes('&',shops.get(i).get("name")) : ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("name") + shops.get(i).get("name")));
            List<String > l = new ArrayList<>(Arrays.asList(ChatPaginator.wordWrap(shops.get(i).get("desc").contains("&") ? ChatColor.translateAlternateColorCodes('&',shops.get(i).get("desc")) : (colors.get("desc") + shops.get(i).get("desc")),30)));
            List<Component> lore = new ArrayList<>();
            l.forEach(s -> lore.add(Component.text(s)));
            lore.add(Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("loc") + shops.get(i).get("loc"))));
            lore.add(0, Component.text(ChatColor.translateAlternateColorCodes(ChatColor.COLOR_CHAR,colors.get("owner") + shops.get(i).get("owners"))));
            if(type == 1) {
                lore.add(Component.text(ChatColor.AQUA + "Shift click to view"));
                lore.add(Component.text(ChatColor.GREEN + "Right click to approve"));
                lore.add(Component.text(ChatColor.RED + "Left click to reject"));
            }
            else if (type == 2) {
                lore.add(Component.text(ChatColor.RED + "Right click to remove"));
            }
            else if(type == 3) {
                lore.add(Component.text(ChatColor.AQUA + "Right click to recover")); //Maybe send recovering message in chat, to notify player
            }
            else if(type == 4) {
                lore.add(Component.text(ChatColor.AQUA + "Right click to check activity")); //confusing, todo later
            }
            else if(type == 5) {
                lore.add(Component.text(ChatColor.AQUA + "Right click to set lookup radius")); //confusing, todo later
            }
            shopMeta.lore(lore);
            shopMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            shopItem.setItemMeta(shopMeta);
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
            shopDirectory.setItem(45,pageNum);
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
        Inventory shopEditMenuInv = Bukkit.createInventory(new ShopInvHolder(key,4,null, null),9, Component.text(name));
        //addOwner button
        ItemStack addOwner = makeDisplayItem(Material.BEACON, Component.text(ChatColor.GOLD + "" + ChatColor.ITALIC + "Add owner"));
        shopEditMenuInv.setItem(1,addOwner);
        //setDisplayItem button
        ItemStack setDisplayItem = makeDisplayItem(Material.WRITABLE_BOOK, Component.text(ChatColor.GOLD + "" + ChatColor.ITALIC + "Set display item"));
        shopEditMenuInv.setItem(4,setDisplayItem);
        //removeShop button
        ItemStack removeShop = makeDisplayItem(Material.FLINT_AND_STEEL, Component.text(ChatColor.RED + "" + ChatColor.ITALIC + "delete shop"));
        shopEditMenuInv.setItem(7,removeShop);
        player.openInventory(shopEditMenuInv);
    }

    /*
     * For adding same item that already exists in shop, show ItemAddMenu
     */
    public void openItemAddMenu(Player player, String key, List<ItemStack> matchingItems, ItemStack itemToAdd) {
        //creates the inventory for this menu, adjustable to the amount of same items in the shop
        Inventory itemAddMenuInv = Bukkit.createInventory(new ShopInvHolder(key,itemToAdd.clone(),5),Math.min(54,9 + 9*(matchingItems.size()/9 + (matchingItems.size()%9 == 0 ? 0 : 1))),Component.text("Adding Item..."));
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