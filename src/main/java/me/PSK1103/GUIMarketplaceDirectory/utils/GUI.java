package me.PSK1103.GUIMarketplaceDirectory.utils;

import me.PSK1103.GUIMarketplaceDirectory.invholders.InvType;
import me.PSK1103.GUIMarketplaceDirectory.invholders.MarketplaceBookHolder;
import me.PSK1103.GUIMarketplaceDirectory.invholders.ShopInvHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.ChatPaginator;

import java.util.*;
import java.util.stream.Collectors;

public class GUI {
    public enum Action {
        NOTHING, 
        PREVIOUS_PAGE, NEXT_PAGE, GO_BACK, 
        OPEN_SHOP, OPEN_EDIT_SHOP_INV, DELETE_ITEM, REMOVE_MATCHING_ITEMS, ADD_ITEM,
        DYNMAP, FIND_BETTER_ALTERNATIVE,
        CANCEL_DESCRIPTION, SET_DESCRIPTION,
        CANCEL_LOCATION, SET_LOCATION,
        CANCEL_DISPLAYITEM, SET_DISPLAYITEM,
        CANCEL_OWNER, ADD_OWNER,
        //moderation
        DELETE_SHOP, GET_SHOP_BOOK,
        APPROVE_SHOP, REJECT_SHOP,
        REJECT_CHANGES, APPROVE_CHANGES,
        SHOW_CHANGES, UNSHOW_CHANGES;
    }

    public interface InventoryMaker {
        public void setPage(int page);
        public Inventory makeInventory();
    }

    private static ItemStack makeShopDisplayItem(Map<String, String> shop, Config config, boolean hasPendingTag, Component... clicks) {
        ItemStack shopItem;
        try {
            shopItem = new ItemStack(Material.getMaterial(shop.get("displayItem")));
        }
        catch (Exception e) {
            shopItem = new ItemStack(Material.WRITTEN_BOOK);
        }
        //Adds lore (info) to the shop display items
        ItemMeta shopMeta = shopItem.getItemMeta();
        //shopname
        shopMeta.displayName(Component.text(config.getDefaultShopNameColor() + shop.get("name")));
        List<String> l = new ArrayList<>(Arrays.asList(ChatPaginator.wordWrap(config.getDefaultShopDescColor() + shop.get("desc"),30)));
        List<Component> lore = new ArrayList<>();
        //pending tag
        if (hasPendingTag) lore.add(Component.text(MyChatColor.RED + "[Pending changes]"));
        //owner
        lore.add(Component.text(config.getDefaultShopOwnerColor() + shop.get("owners")));
        //description
        l.forEach(s -> lore.add(Component.text(s)));
        //location
        String[] parts = shop.get("loc").split(",");
        if(Integer.parseInt(parts[1]) < config.getMaxUndergroundMarketLevel() && parts.length >= 3) {
            lore.add(Component.text(config.getDefaultShopLocColor() + parts[0] + ", " + config.getDefaultShopULocColor() + parts[1] + config.getDefaultShopLocColor() + ", " + parts[2]));    
        }
        else {
            lore.add(Component.text(config.getDefaultShopLocColor() + shop.get("loc")));
        }
        for (Component click: clicks) {
            lore.add(click);
        }
        shopMeta.lore(lore);
        shopMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        shopMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        shopItem.setItemMeta(shopMeta);
        return shopItem;
    }

    private static ItemStack makeShopDisplayItem(Map<String, String> shop, Config config, Component... clicks) {
        return makeShopDisplayItem(shop, config, false, clicks);
    }
    
    private static ItemStack addItemLore(ItemStack item, Component... lore) {        
        ItemMeta itemMeta = item.getItemMeta();
        List<Component> allLore = itemMeta.lore();
        if (allLore==null) allLore = new LinkedList<>();
        allLore.addAll(Arrays.asList(lore));
        itemMeta.lore(allLore);
        item.setItemMeta(itemMeta);
        return item;
    }

    private static ItemStack makeDisplayItem(Material material, Component displayName) {
        ItemStack displayItem = new ItemStack(material);
        ItemMeta meta = displayItem.getItemMeta();
        meta.displayName(displayName);     
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    private static Component[] getItemShopTextFor(InvType type, Config config) {
        ArrayList<Component> texts = new ArrayList<>();
        switch(type) {
            case NORMAL:
                texts.add(Component.text(config.getDefaultShopDynmapColor() + "§oRight click to see this shop on Dynmap"));
            break;
            case PENDING_APPROVALS: 
                texts.add(Component.text(MyChatColor.AQUA + "Shift click to view")); 
                texts.add(Component.text(MyChatColor.GREEN + "Press 'swap-offhand' key to approve"));
                texts.add(Component.text(MyChatColor.RED + "Press 'drop' key to reject"));
            break;
            case PENDING_CHANGES:
                texts.add(Component.text(MyChatColor.WHITE + "Left click to see old shop"));
                texts.add(Component.text(MyChatColor.AQUA + "Right click to see new shop"));
                texts.add(Component.text(MyChatColor.GREEN + "Press 'swap-offhand' key to approve"));
                texts.add(Component.text(MyChatColor.RED + "Press 'drop' key to reject"));
            break;
            case REVIEW: 
                texts.add(Component.text(MyChatColor.RED + "Press 'drop' key to delete"));
            break;
            case RECOVER: 
                texts.add(Component.text(MyChatColor.AQUA + "Right click to recover"));
            break;
            case SEARCH:
                texts.add(Component.text(config.getDefaultShopDynmapColor() + "§oRight click to see this shop on Dynmap"));
            break;
            default:
        }
        return texts.toArray(new Component[0]);
    }

    private static Component[] getItemTextFor(InvType type, Config config) {
        ArrayList<Component> texts = new ArrayList<>();
        switch(type) {
            case INV_EDIT: 
                texts.add(Component.text(MyChatColor.GOLD + "§oShift click to find a better deal"));
                texts.add(Component.text(MyChatColor.RED + "Press 'drop' key to delete this item"));
            break;
            case SEARCH:
                texts.add(Component.text(MyChatColor.YELLOW + "Left-click to view this shop"));
                texts.add(Component.text(config.getDefaultShopDynmapColor() + "§oRight click to see this shop on Dynmap"));
            break;
            case ADD_ITEM:
                texts.add(Component.text(MyChatColor.RED + "Press 'drop' key to remove"));
            break;
            default:
                texts.add(Component.text(MyChatColor.GOLD + "§oShift click to find a better deal"));
        }
        return texts.toArray(new Component[0]);
    }

    private static int getInvSize(int contents, boolean hasBackButton) {
        if (contents == 0) {
            return 9;
        }
        if (hasBackButton) {
            if (contents<54) {
                return (contents/9)*9+9;
            } else {
                return 54;
            }
        } else {
            if (contents<=54) {
                return 9*((int) Math.ceil(((double) contents)/9));
            } else {
                return 54;
            }
        }
    }

    public static void switchShopVersion(Inventory inventory, int invSlot, Map<String, String> shop, Config config) {
        inventory.setItem(invSlot, makeShopDisplayItem(shop, config, getItemShopTextFor(InvType.PENDING_CHANGES, config)));
    }

    public static void fillShopInventory(Inventory shopDirectory, MarketplaceBookHolder holder, int page, Config config) {
        fillShopInventory(shopDirectory, holder, holder.getShops(), holder.getType(), page, config);
    }

    public static void fillShopInventory(Inventory shopDirectory, MarketplaceBookHolder holder, List<Map<String,String>> shops, InvType type, int page, Config config) {
        holder.getInventoryMaker().setPage(page);
        for(int i=0;i<(shops.size() > 54 ? Math.min(45, shops.size()-45*page) : shops.size());i++) {
            ItemStack shopItem = makeShopDisplayItem(shops.get(page*45+i), config, getItemShopTextFor(type, config));
            shopDirectory.setItem(i,shopItem);
        }

        if(shops.size() > 54) {
            ((MarketplaceBookHolder) shopDirectory.getHolder()).setPaged();
            //nextPage button
            ItemStack nextPage = makeDisplayItem(shops.size() > (page+1)*45 ? Material.LIME_STAINED_GLASS_PANE : Material.BARRIER, Component.text("Next Page"));
            shopDirectory.setItem(50,nextPage);
            //prevPage button
            ItemStack prevPage = makeDisplayItem(page>0 ?  Material.ORANGE_STAINED_GLASS_PANE : Material.BARRIER, Component.text("Previous Page"));
            shopDirectory.setItem(48,prevPage);
            //numPage display
            ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page " + (page+1)));
            shopDirectory.setItem(49,pageNum);
        }
    }

    public static Inventory makeShopInventory(String title, List<Map<String,String>> shops, InvType type, Config config, InventoryMaker previousWindow) {
        return makeShopInventory(Component.text(title), shops, type, config, 0, previousWindow);
    }

    public static Inventory makeShopInventory(Component title, List<Map<String,String>> shops, InvType type, Config config, InventoryMaker previousWindow) {
        return makeShopInventory(title, shops, type, config, 0, previousWindow);
    }

    public static Inventory makeShopInventory(Component title, List<Map<String,String>> shops, InvType type, Config config, int page, InventoryMaker previousWindow) {
        InventoryMaker instructions = new shopInventoryMaker(title, shops, type, config, page, previousWindow);
        MarketplaceBookHolder holder = new MarketplaceBookHolder(shops, type, instructions);
        holder.setPreviousInventoryMaker(previousWindow);
        Inventory shopDirectory = Bukkit.createInventory(holder, getInvSize(shops.size(), previousWindow!=null), title);
        //Creates the directory of all the selected shops 
        fillShopInventory(shopDirectory, holder, shops, type, page, config);

        return shopDirectory;
    }

    public static void fillItemInventory(Inventory shopInventory, ShopInvHolder holder, Config config, int page) {
        fillItemInventory(shopInventory, holder, holder.getInv(), holder.getType(), config, page, holder.getPreviousInventoryMaker() != null);
    }

    public static void fillItemInventory(Inventory shopInventory, ShopInvHolder holder, List<ItemStack> items, InvType type, Config config, int page, boolean backButton) {
        holder.getInventoryMaker().setPage(page);
        List<ItemStack> inv = items.stream().map(item -> addItemLore(item.clone(), getItemTextFor(type, config))).collect(Collectors.toList());
        //int i=0;i<(shops.size() > 54 ? Math.min(45, shops.size()-45*page) : shops.size());i++
        for(int i=0;i< Math.min(45, inv.size()-page*45);i++) {            
            shopInventory.setItem(i,inv.get(i + page*45));
        }
        if(inv.size() == 0) {
            //empty shop display
            ItemStack empty = makeDisplayItem(Material.BARRIER, Component.text(MyChatColor.RED + "This shop is empty!"));
            shopInventory.setItem(4,empty);
        }
        //goBack button
        if (backButton) {
            ItemStack back = makeDisplayItem(Material.ARROW, Component.text(MyChatColor.YELLOW + "Go Back"));
            shopInventory.setItem(shopInventory.getSize()-1,back);
        }
        if(inv.size()>45) {
            ((ShopInvHolder) shopInventory.getHolder()).setPaged();
            //nextPage button
            ItemStack nextPage = makeDisplayItem(inv.size() > (page+1)*45 ? Material.LIME_STAINED_GLASS_PANE : Material.BARRIER, Component.text("Next Page"));
            shopInventory.setItem(50,nextPage);
            //prevPage button
            ItemStack prevPage = makeDisplayItem(page>0 ?  Material.ORANGE_STAINED_GLASS_PANE : Material.BARRIER, Component.text("Previous Page"));
            shopInventory.setItem(48,prevPage);
            //numPage display
            ItemStack pageNum = makeDisplayItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, Component.text("Page " + (page+1)));
            shopInventory.setItem(49,pageNum);
        }
        if(type==InvType.ADD_ITEM) {
            //addItem button
            ItemStack addItem = makeDisplayItem(Material.ENDER_PEARL, Component.text(MyChatColor.GREEN + "" + MyChatColor.ITALIC + "Add item"));
            shopInventory.setItem(shopInventory.getSize()-7,addItem);
            //removeAllItems button
            ItemStack removeAllItems = makeDisplayItem(Material.FLINT_AND_STEEL, Component.text(MyChatColor.RED + "" + MyChatColor.ITALIC + "Remove all items"));
            shopInventory.setItem(shopInventory.getSize()-3,removeAllItems);
        }
    }

    public static Inventory makeItemInventory(String title, String key, List<ItemStack> items, InvType type, Config config, ItemStack item, InventoryMaker previousWindow) {
        return makeItemInventory(Component.text(title), key, items, Collections.nCopies(items.size(), key), type, config, item, 0, previousWindow);
    }

    public static Inventory makeItemInventory(Component title, String key, List<ItemStack> items, InvType type, Config config, InventoryMaker previousWindow) {
        return makeItemInventory(title, key, items, Collections.nCopies(items.size(), key), type, config, null, 0, previousWindow);
    }
    
    public static Inventory makeItemInventory(String title, String key, List<ItemStack> items, List<String> shops, InvType type, Config config, InventoryMaker previousWindow) {
        return makeItemInventory(Component.text(title), key, items, shops, type, config, null, 0, previousWindow);
    }

    public static Inventory makeItemInventory(Component title, String key, List<ItemStack> items, List<String> shops, InvType type, Config config, ItemStack itemToAdd, int page, InventoryMaker previousWindow) {
        InventoryMaker instructions = new itemInventoryMaker(title, key, items, shops, type, config, itemToAdd, page, previousWindow);
        ShopInvHolder holder = new ShopInvHolder(key,type,items, instructions);
        holder.setPreviousInventoryMaker(previousWindow);
        holder.setShops(shops);
        holder.setItem(itemToAdd); //itemToAdd is null unless type=ADD_ITEM
        Inventory shopInventory = Bukkit.createInventory(holder,getInvSize(items.size(), previousWindow != null), title);
        fillItemInventory(shopInventory, holder, items, type, config, page, previousWindow != null);
        return shopInventory;
    }

    public static Inventory makeShopEditMenu(String title, String key, Map<String, String> shop, Map<String, String> pendingChanges, Config config, InventoryMaker previousWindow) {
        InventoryMaker instructions = new shopEditMenuMaker(title, key, shop, pendingChanges, config, previousWindow);
        //creates the inventory for this menu
        ShopInvHolder currentShopView = new ShopInvHolder(key, InvType.SHOP_MENU, instructions);
        //Adds this shop to a list with one entry, to open its inventory if rawSloth 4 is clicked
        currentShopView.setShops(Collections.nCopies(18, key));
        boolean hasPendingChanges = pendingChanges!=null;
        Inventory shopEditMenuInv = Bukkit.createInventory(currentShopView,18, Component.text(title));
        //setDescription button
        ItemStack setDescription = makeDisplayItem(Material.PAPER, Component.text(MyChatColor.GOLD + "" + MyChatColor.ITALIC + "Set description"));
        if (hasPendingChanges && !shop.get("desc").equals(pendingChanges.get("desc"))) {
            List<Component> descriptionLore = new LinkedList<>();
            descriptionLore.add(Component.text(MyChatColor.DARK_PURPLE + "[Old]"));
            descriptionLore.addAll(Arrays.asList(ChatPaginator.wordWrap(MyChatColor.GREEN + shop.get("desc"),30)).stream().map(x -> Component.text(MyChatColor.GREEN + x)).toList());
            descriptionLore.add(Component.text(MyChatColor.LIGHT_PURPLE + "[New]"));
            descriptionLore.addAll(Arrays.asList(ChatPaginator.wordWrap(MyChatColor.GREEN + pendingChanges.get("desc"),30)).stream().map(x -> Component.text(MyChatColor.GREEN + x)).toList());
            descriptionLore.add(Component.text(MyChatColor.DARK_PURPLE + " "));
            descriptionLore.add(Component.text(MyChatColor.RED + "Press 'drop' key to revert the change"));
            setDescription = addItemLore(setDescription, descriptionLore.toArray(new Component[descriptionLore.size()]));
        }
        setDescription = addItemLore(setDescription, Component.text(MyChatColor.DARK_AQUA + "Left click to change your description"));
        shopEditMenuInv.setItem(1,setDescription);
        //see shop button
        ItemStack seeShop;
        if (hasPendingChanges) seeShop = makeShopDisplayItem(pendingChanges, config, true,
                                                             Component.text(config.getDefaultShopDynmapColor() + "§oRight click to see this shop on Dynmap"));
        else seeShop = makeShopDisplayItem(shop, config, false, 
                                           Component.text(config.getDefaultShopDynmapColor() + "§oRight click to see this shop on Dynmap"));
        shopEditMenuInv.setItem(4,seeShop);
        //setLocation button
        ItemStack setLocation = makeDisplayItem(Material.COMPASS, Component.text(MyChatColor.GOLD + "" + MyChatColor.ITALIC + "Set location"));
        if (hasPendingChanges && !shop.get("loc").equals(pendingChanges.get("loc"))) {
            String[] partsOld = shop.get("loc").split(",");
            String[] partsNew = pendingChanges.get("loc").split(",");
            List<Component> locationLore = new LinkedList<>();
            locationLore.add(Component.text(MyChatColor.DARK_PURPLE + "[Old]"));
            if(Integer.parseInt(partsOld[1]) < config.getMaxUndergroundMarketLevel() && partsOld.length >= 3) {
                locationLore.add(Component.text(config.getDefaultShopLocColor() + partsOld[0] + "," + config.getDefaultShopULocColor() + partsOld[1] + config.getDefaultShopLocColor() + "," + partsOld[2]));
            } else {
                locationLore.add(Component.text(config.getDefaultShopLocColor() + shop.get("loc")));
            }
            locationLore.add(Component.text(MyChatColor.LIGHT_PURPLE + "[New]"));
            if(Integer.parseInt(partsNew[1]) < config.getMaxUndergroundMarketLevel() && partsNew.length >= 3) {
                locationLore.add(Component.text(config.getDefaultShopLocColor() + partsNew[0] + "," + config.getDefaultShopULocColor() + partsNew[1] + config.getDefaultShopLocColor() + "," + partsNew[2]));
            } else {
                locationLore.add(Component.text(config.getDefaultShopLocColor() + pendingChanges.get("loc")));
            }
            locationLore.add(Component.text(MyChatColor.DARK_PURPLE + " "));
            locationLore.add(Component.text(MyChatColor.RED + "Press 'drop' key to revert the change"));
            setLocation = addItemLore(setLocation, locationLore.toArray(new Component[locationLore.size()]));
        }
        setLocation = addItemLore(setLocation, Component.text(MyChatColor.DARK_AQUA + "Left click to move your shop"));
        shopEditMenuInv.setItem(7,setLocation);
        //setDisplayItem button
        ItemStack setDisplayItem = makeDisplayItem(Material.WRITABLE_BOOK, Component.text(MyChatColor.GOLD + "" + MyChatColor.ITALIC + "Set display item"));
        if (hasPendingChanges && !shop.get("displayItem").equals(pendingChanges.get("displayItem"))) {
            setDisplayItem = addItemLore(setDisplayItem, Component.text(MyChatColor.DARK_PURPLE + "[Old]"),
                                                         Component.text(MyChatColor.GREEN + shop.get("displayItem")),
                                                         Component.text(MyChatColor.LIGHT_PURPLE + "[New]"),
                                                         Component.text(MyChatColor.GREEN + pendingChanges.get("displayItem")),
                                                         Component.text(MyChatColor.DARK_PURPLE + " "),
                                                         Component.text(MyChatColor.RED + "Press 'drop' key to revert the change"));
        }
        setDisplayItem = addItemLore(setDisplayItem, Component.text(MyChatColor.DARK_AQUA + "Left click to change display item"));
        shopEditMenuInv.setItem(10,setDisplayItem);
        //addOwner button
        ItemStack addOwner = makeDisplayItem(Material.BEACON, Component.text(MyChatColor.GOLD + "" + MyChatColor.ITALIC + "Add owner"));
        if (hasPendingChanges && !shop.get("owners").equals(pendingChanges.get("owners"))) {
            addOwner = addItemLore(addOwner, Component.text(MyChatColor.DARK_PURPLE + "[Old]"),
                                             Component.text(MyChatColor.GREEN + shop.get("owners")),
                                             Component.text(MyChatColor.LIGHT_PURPLE + "[New]"),
                                             Component.text(MyChatColor.GREEN + pendingChanges.get("owners")),
                                             Component.text(MyChatColor.DARK_PURPLE + " "),
                                             Component.text(MyChatColor.RED + "Press 'drop' key to revert the change"));
        }
        addOwner = addItemLore(addOwner, Component.text(MyChatColor.DARK_AQUA + "Left click to add a new owner"));
        shopEditMenuInv.setItem(13,addOwner);
        //removeShop button
        ItemStack removeShop = makeDisplayItem(Material.FLINT_AND_STEEL, Component.text(MyChatColor.RED + "" + MyChatColor.ITALIC + "Delete shop"));
        shopEditMenuInv.setItem(16,removeShop);
        return shopEditMenuInv;
    }

    public static Action getButtonAction(Inventory shopInventory, ShopInvHolder holder, int slotNum, ClickType click, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR || slotNum<0 || slotNum>=shopInventory.getSize()) 
            return Action.NOTHING;

        int currPage = 0;
        if (holder.isPaged() && slotNum>=shopInventory.getSize()-9) {
            if (clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE) return Action.NEXT_PAGE;
            if (clickedItem.getType() == Material.ORANGE_STAINED_GLASS_PANE) return Action.PREVIOUS_PAGE;
            currPage = Integer.parseInt(((TextComponent) shopInventory.getItem(49).getItemMeta().displayName()).content().substring(5))-1;
        }
        if (slotNum == shopInventory.getSize()-1 && clickedItem.displayName().toString().contains("Go Back")) {
            return Action.GO_BACK;
        }
        if (holder.getType() == InvType.SHOP_MENU) {
            if (slotNum == 1 && click != ClickType.DROP) return Action.SET_DESCRIPTION;
            if (slotNum == 1 && click == ClickType.DROP) return Action.CANCEL_DESCRIPTION;
            if (slotNum == 7 && click != ClickType.DROP) return Action.SET_LOCATION;
            if (slotNum == 7 && click == ClickType.DROP) return Action.CANCEL_LOCATION;
            if (slotNum == 10 && click != ClickType.DROP) return Action.SET_DISPLAYITEM;
            if (slotNum == 10 && click == ClickType.DROP) return Action.CANCEL_DISPLAYITEM;
            if (slotNum == 13 && click != ClickType.DROP) return Action.ADD_OWNER;
            if (slotNum == 13 && click == ClickType.DROP) return Action.CANCEL_OWNER;
            if (slotNum == 16) return Action.DELETE_SHOP;
            if (slotNum == 4 && click.isRightClick()) return Action.DYNMAP;
            if (slotNum == 4 && !click.isRightClick()) return Action.OPEN_EDIT_SHOP_INV;
            return Action.NOTHING;
        } else if (holder.getType() == InvType.ADD_ITEM) {
            if (slotNum == shopInventory.getSize()-3) return Action.REMOVE_MATCHING_ITEMS;
            if (slotNum == shopInventory.getSize()-7) return Action.ADD_ITEM;
            if (click == ClickType.DROP && slotNum + 45*currPage < holder.getInv().size()) return Action.DELETE_ITEM;
            return Action.NOTHING;
        } else if (slotNum + 45*currPage < holder.getInv().size()) {
            if (click.isShiftClick() && holder.getType() != InvType.ADD_ITEM && holder.getType() != InvType.SEARCH) return Action.FIND_BETTER_ALTERNATIVE;
            if (click.isLeftClick() && holder.getType() == InvType.SEARCH) return Action.OPEN_SHOP;
            if (click.isRightClick() && holder.getType() == InvType.SEARCH) return Action.DYNMAP;
            if (click == ClickType.DROP && holder.getType() == InvType.INV_EDIT) return Action.DELETE_ITEM;
        }
        return Action.NOTHING;
    }

    public static Action getButtonAction(Inventory itemInventory, MarketplaceBookHolder holder, int slotNum, ClickType click, ItemStack clickedItem) {
        //was something clicked
        if (clickedItem == null || clickedItem.getType() == Material.AIR || slotNum<0 || slotNum>=itemInventory.getSize()) 
            return Action.NOTHING;
        InvType type = holder.getType();
        //if holder is paged
        int currPage = 0;
        if (holder.isPaged() && slotNum>=itemInventory.getSize()-9) {
            if (clickedItem.getType() == Material.LIME_STAINED_GLASS_PANE) return Action.NEXT_PAGE;
            if (clickedItem.getType() == Material.ORANGE_STAINED_GLASS_PANE) return Action.PREVIOUS_PAGE;
            currPage = Integer.parseInt(((TextComponent) itemInventory.getItem(49).getItemMeta().displayName()).content().substring(5))-1;
        }
        if (slotNum == itemInventory.getSize()-1 && clickedItem.displayName().toString().contains("Go Back")) {
            return Action.GO_BACK;
        }
        //if a shop was clicked
        if (slotNum + 45*currPage < holder.getShops().size()) {
            if (click.isShiftClick()) return Action.OPEN_SHOP;
            //pending approvals
            if (click == ClickType.SWAP_OFFHAND && type==InvType.PENDING_APPROVALS) return Action.APPROVE_SHOP;
            if (click == ClickType.DROP && type==InvType.PENDING_APPROVALS) return Action.REJECT_SHOP;
            //pending changes
            if (click == ClickType.SWAP_OFFHAND && type==InvType.PENDING_CHANGES) return Action.APPROVE_CHANGES;
            if (click == ClickType.DROP && type==InvType.PENDING_CHANGES) return Action.REJECT_CHANGES;
            if (click.isLeftClick() && type==InvType.PENDING_CHANGES) return Action.UNSHOW_CHANGES;
            if (click.isRightClick() && type==InvType.PENDING_CHANGES) return Action.SHOW_CHANGES;
            //review
            if (click == ClickType.DROP && type==InvType.REVIEW) return Action.DELETE_SHOP;
            //recover
            if (click.isRightClick() && type==InvType.RECOVER) return Action.GET_SHOP_BOOK;
            //normal and search window
            if (click.isRightClick() && (type == InvType.NORMAL || type == InvType.SEARCH)) return Action.DYNMAP;
            if (click.isLeftClick() && (type == InvType.NORMAL || type == InvType.SEARCH)) return Action.OPEN_SHOP;
        }
        return Action.NOTHING;
    }

    public static class shopInventoryMaker implements InventoryMaker {
        Component title;
        List<Map<String,String>> shops;
        InvType type;
        Config config;
        int page;
        InventoryMaker instructions;
        public shopInventoryMaker(Component title, List<Map<String,String>> shops, InvType type, Config config, int page, InventoryMaker instructions) {
            this.title = title;
            this.shops = shops;
            this.type = type;
            this.config = config;
            this.page = page;
            this.instructions = instructions;
        }
        @Override
        public void setPage(int page) {
            this.page = page;
        }

        @Override
        public Inventory makeInventory() {
            return makeShopInventory(title, shops, type, config, page, instructions);
        }
    
    }

    public static class itemInventoryMaker implements InventoryMaker {
        Component title;
        String key;
        List<ItemStack> items;
        List<String> shops;
        InvType type;
        Config config;
        ItemStack itemToAdd;
        int page;
        InventoryMaker instructions;
        public itemInventoryMaker(Component title, String key, List<ItemStack> items, List<String> shops, InvType type, Config config, ItemStack itemToAdd, int page, InventoryMaker instructions) {
            this.title = title;
            this.key = key;
            this.items = items;
            this.shops = shops;
            this.type = type;
            this.config = config;
            this.itemToAdd = itemToAdd;
            this.page = page;
            this.instructions = instructions;
        }

        @Override
        public void setPage(int page) {
            this.page = page;
        }

        @Override
        public Inventory makeInventory() {
            return makeItemInventory(title, key, items, shops, type, config, itemToAdd, page, instructions);
        }
        
    }

    public static class shopEditMenuMaker implements InventoryMaker {
        String title;
        String key;
        Map<String, String> shop;
        Map<String, String> pendingChanges;
        Config config;
        InventoryMaker instructions;
        public shopEditMenuMaker(String title, String key, Map<String, String> shop, Map<String, String> pendingChanges, Config config, InventoryMaker instructions) {
            this.title = title;
            this.key = key;
            this.shop = shop;
            this.pendingChanges = pendingChanges;
            this.config = config;
            this.instructions = instructions;
        }

        @Override
        public void setPage(int page) {
            //the shop menu (so far) isn't paged thus this is ignored.
        }

        @Override
        public Inventory makeInventory() {
            return makeShopEditMenu(title, key, shop, pendingChanges, config, instructions);
        }
        
    }
}
