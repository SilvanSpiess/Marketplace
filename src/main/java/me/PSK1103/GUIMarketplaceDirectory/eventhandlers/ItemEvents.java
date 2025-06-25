package me.PSK1103.GUIMarketplaceDirectory.eventhandlers;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.invholders.InvType;
import me.PSK1103.GUIMarketplaceDirectory.invholders.ShopInvHolder;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.ChatProcess;
import me.PSK1103.GUIMarketplaceDirectory.utils.GUI;
import me.PSK1103.GUIMarketplaceDirectory.utils.MyChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import java.util.List;
import java.util.Map;

public class ItemEvents implements Listener {
    final GUIMarketplaceDirectory plugin;

    public ItemEvents(GUIMarketplaceDirectory plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public final void addItem(InventoryClickEvent itemEvent) {
        /* 
         * is executed when player right clicks with book in hand on an item to add that item to their shop.
         */
        if(!itemEvent.isRightClick())
            return;
        if(itemEvent.getCursor() != null && itemEvent.getCursor().getType() == Material.WRITTEN_BOOK) {
            itemEvent.setCancelled(true);
            BookMeta bookMeta = ((BookMeta) itemEvent.getCursor().getItemMeta());

            if(!bookMeta.getTitle().toLowerCase().equals("[shop init]") && !bookMeta.getTitle().toLowerCase().equals("init shop")) {
                return;
            }

            ItemStack item = itemEvent.getCurrentItem(); //gets item you clicked on

            if(item == null || item.getType().isAir()) {
                return;
            }
            // searches for a place to put the book, because your inventory will be closed in order to type in chat
            Player player = ((Player) itemEvent.getWhoClicked());
            Inventory inventory = itemEvent.getInventory();
            if(player.getInventory().firstEmpty() !=-1) {
                player.getInventory().setItem(player.getInventory().firstEmpty(),itemEvent.getCursor());
                itemEvent.setCursor(new ItemStack(Material.AIR));
                player.updateInventory();
            }
            else if(inventory.firstEmpty() != -1) {
                inventory.setItem(inventory.firstEmpty(), itemEvent.getCursor());
                itemEvent.setCursor(new ItemStack(Material.AIR));
                player.updateInventory();
            }

            player.closeInventory(); //inventory gets closed

            //checks whether an item can be added right now
            if (plugin.getProcessHandler().isPlayerInProcess(player.getUniqueId().toString())) {
                ChatProcess process = this.plugin.getProcessHandler().getPlayerProcess(player.getUniqueId().toString());
                player.sendMessage(MyChatColor.RED + "Finish " + process.getName() + " first");
                return;  
            }

            if (plugin.getProcessHandler().isShopInProcess(bookMeta.getPage(bookMeta.getPageCount()))) {
                player.sendMessage(MyChatColor.RED + "Shop is under some operation, try again later.");
                return;  
            }

            List<ItemStack> matchingItems = plugin.getShopRepo().getMatchingItems(bookMeta.getPage(bookMeta.getPageCount()),item.getType().getKey().getKey().toUpperCase());

            if(matchingItems == null) {
                player.sendMessage(MyChatColor.RED + "Shop doesn't exist");
                return;
            }
            //asks for quantity and the function addItemData will pick up when the player types the amount or price in chat.

            if(matchingItems.size() == 0) { //if shop owner doesn't have the item in shop already. 
                plugin.getProcessHandler().initItemAddition(player, bookMeta.getPage(bookMeta.getPageCount()), item);
            }
            else { //if shop owner has the item in shop already it opens a window displaying those items and giving multiple options to perceed. 
                Inventory itemAddMenuInv = GUI.makeItemInventory("Adding Item...", bookMeta.getPage(bookMeta.getPageCount()), matchingItems, InvType.ADD_ITEM, plugin.getCustomConfig(), item.clone(), null);
                player.openInventory(itemAddMenuInv);
            }
        }
    }

    @EventHandler
    public final void addItemData(AsyncPlayerChatEvent addItemDetails) {
        /* this event gets executed every time people speak in chat, 
         * but will only do something if the corresponding player is in a chatProcess
         */
        if (plugin.getProcessHandler().isPlayerInProcess(addItemDetails.getPlayer().getUniqueId().toString())) {
            ChatProcess process = plugin.getProcessHandler().getPlayerProcess(addItemDetails.getPlayer().getUniqueId().toString());
            if (process.handleChat(addItemDetails.getPlayer(), addItemDetails.getMessage())) {
                addItemDetails.setCancelled(true);
            }
        }
    }

    @EventHandler
    public final void checkForAlternatives(InventoryClickEvent itemCheckEvent) {
        if(itemCheckEvent.getInventory().getHolder() instanceof ShopInvHolder) {
            itemCheckEvent.setCancelled(true);
            //shortcut variables
            ShopInvHolder holder = (ShopInvHolder)itemCheckEvent.getInventory().getHolder();
            int slotNum = itemCheckEvent.getRawSlot();
            //get action
            GUI.Action action = GUI.getButtonAction(itemCheckEvent.getInventory(), holder, slotNum, itemCheckEvent.getClick(), itemCheckEvent.getCurrentItem());
            int currPage = 0;
            Player player = (Player) itemCheckEvent.getWhoClicked();
            try {
                currPage = Integer.parseInt(((TextComponent) itemCheckEvent.getInventory().getItem(49).getItemMeta().displayName()).content().substring(5))-1;
            } catch (Exception e) {}
            String key = "";
            String name = "";
            try {
                key = holder.getShops().get(slotNum + 45*currPage);
                name = plugin.getShopRepo().getShopName(key);
            } catch (Exception e) {}
            
            switch(action) {
                case NOTHING: break;
                case NEXT_PAGE:
                    itemCheckEvent.getInventory().clear();
                    GUI.fillItemInventory(itemCheckEvent.getInventory(), holder, plugin.getCustomConfig(), currPage+1);
                    player.updateInventory();
                break;
                case PREVIOUS_PAGE:
                    itemCheckEvent.getInventory().clear();
                    GUI.fillItemInventory(itemCheckEvent.getInventory(), holder, plugin.getCustomConfig(), currPage-1);
                    player.updateInventory();
                break;
                case GO_BACK: {
                    Inventory backInventory = holder.makePreviousInventory();
                    if (backInventory == null) 
                        player.sendMessage("No back page found.");
                    else {
                        player.closeInventory();
                        player.openInventory(holder.makePreviousInventory());
                    }
                } break;
                case OPEN_SHOP: {
                    player.closeInventory();
                    List<ItemStack> inv = plugin.getShopRepo().getShopInv(key);
                    Inventory shopInventory = GUI.makeItemInventory(plugin.getShopRepo().isPendingShop(key) ? Component.text(name+ " §5§o(pending)") : Component.text(name), key, inv, InvType.NORMAL, plugin.getCustomConfig(), holder.getInventoryMaker());
                    player.openInventory(shopInventory);
                } break;
                case OPEN_EDIT_SHOP_INV: {
                    player.closeInventory();
                    List<ItemStack> inv = plugin.getShopRepo().getShopInv(key);
                    Inventory shopInventory = GUI.makeItemInventory(plugin.getShopRepo().isPendingShop(key) ? Component.text(name+ " §5§o(pending)") : Component.text(name), key, inv, InvType.INV_EDIT, plugin.getCustomConfig(), holder.getInventoryMaker());
                    player.openInventory(shopInventory);
                } break;
                case DYNMAP: {
                    String input = plugin.getShopRepo().getSpecificShopDetails(key).get("loc");
                    String[] parts = input.split(",");
                    String messageLink;
                    if(parts.length == 2) {       
                        messageLink = plugin.getCustomConfig().getDynmapServerAdress() + "#world;flat;" + Integer.parseInt(parts[0]) + ",64," + Integer.parseInt(parts[1]) + ";7";
                    }
                    else {      
                        messageLink = plugin.getCustomConfig().getDynmapServerAdress() + "#world;flat;" + Integer.parseInt(parts[0]) + ",64," + Integer.parseInt(parts[2]) + ";7";
                    }
                    var mm = MiniMessage.miniMessage();
                    Component parsed = mm.deserialize("<#3ed3f1>You can <hover:show_text:'<gray><underlined>" + messageLink + "</underlined>'><click:OPEN_URL:'" + messageLink + "'><#3c9aaf><underlined><bold>[click here]</bold></underlined></click></hover> <#3ed3f1>to open the location in <#ee2bd6><bold>dynmap</bold><#3ed3f1>.");
                    player.sendMessage(parsed);
                } break;
                case WAYPOINT: {
                    //xaero waypoint link
                    String input = plugin.getShopRepo().getSpecificShopDetails(key).get("loc");
                    String[] parts = input.split(",");
                    String shopname = plugin.getShopRepo().getSpecificShopDetails(key).get("name");
                    String messageWaypointLink;
                    if(parts.length == 2) {       
                        messageWaypointLink = "xaero-waypoint:%s:X:%s:%s:%s:11:false:0:Internal-overworld-waypoints".formatted(shopname, parts[0], "64", parts[1]);
                    }
                    else {      
                        messageWaypointLink = "xaero-waypoint:%s:X:%s:%s:%s:11:false:0:Internal-overworld-waypoints".formatted(shopname, parts[0], parts[1], parts[2]);
                    }
                    player.sendMessage(messageWaypointLink);
                } break;
                case FIND_BETTER_ALTERNATIVE:
                    plugin.getShopRepo().findBetterAlternative(player, holder.getKey(), currPage*45 + slotNum);
                break;
                case DELETE_ITEM:
                    plugin.getShopRepo().removeItem(key, itemCheckEvent.getCurrentItem());
                    List<ItemStack> inv = plugin.getShopRepo().getShopInv(key);
                    Inventory shopInventory = GUI.makeItemInventory(plugin.getShopRepo().isPendingShop(key) ? Component.text(name+ " §5§o(pending)") : Component.text(name), holder.getKey(), inv, holder.getType(), plugin.getCustomConfig(), holder.getPreviousInventoryMaker());
                    player.openInventory(shopInventory);
                break;
                case ADD_ITEM:
                    player.closeInventory();
                    ItemStack item = holder.getItem();
                    plugin.getProcessHandler().initItemAddition(player, holder.getKey(), item);
                break;
                case REMOVE_MATCHING_ITEMS:
                    player.closeInventory();
                    plugin.getShopRepo().removeMatchingItems(holder.getKey(),holder.getItem().getType().getKey().getKey().toUpperCase());
                    player.closeInventory();
                    player.sendMessage(MyChatColor.YELLOW + "All matching items removed");
                break;
                case DELETE_SHOP:
                    plugin.getProcessHandler().startDeletingShop(player, holder.getKey());
                    player.closeInventory();
                break;
                case SET_DESCRIPTION:
                    player.closeInventory();
                    plugin.getProcessHandler().startSettingDescription(player, holder.getKey());
                break;
                case CANCEL_DESCRIPTION: {
                    player.closeInventory();
                    plugin.getShopRepo().cancelNewDescription(player.getUniqueId().toString(), holder.getKey());
                    Map<String,String> thisShop = plugin.getShopRepo().getSpecificShopDetails(holder.getKey());
                    Map<String,String> pendingChangesShop = plugin.getShopRepo().getSpecificChangeDetails(holder.getKey());
                    Inventory shopEditMenuInv = GUI.makeShopEditMenu(plugin.getShopRepo().getShopTitle(holder.getKey()), holder.getKey(), thisShop, pendingChangesShop, plugin.getCustomConfig(), holder.getPreviousInventoryMaker());
                    player.openInventory(shopEditMenuInv);
                } break;
                case SET_LOCATION:
                    player.closeInventory();
                    plugin.getProcessHandler().startSettingLocation(player, holder.getKey());
                break;
                case CANCEL_LOCATION: {
                    player.closeInventory();
                    plugin.getShopRepo().cancelNewLocation(player.getUniqueId().toString(), holder.getKey());
                    Map<String,String> thisShop = plugin.getShopRepo().getSpecificShopDetails(holder.getKey());
                    Map<String,String> pendingChangesShop = plugin.getShopRepo().getSpecificChangeDetails(holder.getKey());
                    Inventory shopEditMenuInv = GUI.makeShopEditMenu(plugin.getShopRepo().getShopTitle(holder.getKey()), holder.getKey(), thisShop, pendingChangesShop, plugin.getCustomConfig(), holder.getPreviousInventoryMaker());
                    player.openInventory(shopEditMenuInv);
                } break;
                case SET_DISPLAYITEM:
                    player.closeInventory();
                    plugin.getProcessHandler().startSettingDisplayItem(player, holder.getKey());
                break;
                case CANCEL_DISPLAYITEM: {
                    player.closeInventory();
                    plugin.getShopRepo().cancelNewDisplayItem(player.getUniqueId().toString(), holder.getKey());
                    Map<String,String> thisShop = plugin.getShopRepo().getSpecificShopDetails(holder.getKey());
                    Map<String,String> pendingChangesShop = plugin.getShopRepo().getSpecificChangeDetails(holder.getKey());
                    Inventory shopEditMenuInv = GUI.makeShopEditMenu(plugin.getShopRepo().getShopTitle(holder.getKey()), holder.getKey(), thisShop, pendingChangesShop, plugin.getCustomConfig(), holder.getPreviousInventoryMaker());
                    player.openInventory(shopEditMenuInv);
                } break;
                case ADD_OWNER:
                    player.closeInventory();
                    plugin.getProcessHandler().startAddingOwner(player, holder.getKey());
                break;
                case CANCEL_OWNER: {
                    player.closeInventory();
                    plugin.getShopRepo().cancelNewOwner(player.getUniqueId().toString(), holder.getKey());
                    Map<String,String> thisShop = plugin.getShopRepo().getSpecificShopDetails(holder.getKey());
                    Map<String,String> pendingChangesShop = plugin.getShopRepo().getSpecificChangeDetails(holder.getKey());
                    Inventory shopEditMenuInv = GUI.makeShopEditMenu(plugin.getShopRepo().getShopTitle(holder.getKey()), holder.getKey(), thisShop, pendingChangesShop, plugin.getCustomConfig(), holder.getPreviousInventoryMaker());
                    player.openInventory(shopEditMenuInv);
                } break;
                default:
                    System.out.println("error: " + action.toString() + " was unsuccesful");
                break;
            }
        }
    }
}
