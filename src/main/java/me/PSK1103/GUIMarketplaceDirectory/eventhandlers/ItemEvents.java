package me.PSK1103.GUIMarketplaceDirectory.eventhandlers;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.invholders.InvType;
import me.PSK1103.GUIMarketplaceDirectory.invholders.ShopInvHolder;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.ChatProcess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import java.util.List;

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
                player.sendMessage(ChatColor.RED + "Finish " + process.getName() + " first");
                return;  
            }

            if (plugin.getProcessHandler().isShopInProcess(bookMeta.getPage(bookMeta.getPageCount()))) {
                player.sendMessage(ChatColor.RED + "Shop is under some operation, try again later.");
                return;  
            }

            List<ItemStack> matchingItems = plugin.getShopRepo().getMatchingItems(bookMeta.getPage(bookMeta.getPageCount()),item.getType().getKey().getKey().toUpperCase());

            if(matchingItems == null) {
                player.sendMessage(ChatColor.RED + "Shop doesn't exist");
                return;
            }
            //asks for quantity and the function addItemData will pick up when the player types the amount or price in chat.

            if(matchingItems.size() == 0) { //if shop owner doesn't have the item in shop already. 
                plugin.getProcessHandler().initItemAddition(player, bookMeta.getPage(bookMeta.getPageCount()), item);
            }
            else { //if shop owner has the item in shop already it opens a window displaying those items and giving multiple options to perceed. 
                plugin.gui.openItemAddMenu(player,bookMeta.getPage(bookMeta.getPageCount()),matchingItems,item);
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
            if(itemCheckEvent.getCurrentItem() == null || itemCheckEvent.getCurrentItem().getType() == Material.AIR)
                return;

            ShopInvHolder holder = (ShopInvHolder)itemCheckEvent.getInventory().getHolder();

            if(holder.getKey().length() == 0 && holder.getType() != InvType.SEARCH) {return;}

            if(itemCheckEvent.getRawSlot() > itemCheckEvent.getInventory().getSize()) {return;}
            InvType type = holder.getType();
            Player player = ((Player) itemCheckEvent.getWhoClicked());

            if(type == InvType.SEARCH) { 
                int currPage = 1;
                //if clicks in bottom 9 slots of the inventory AND the inventory size is 54 (double chest).
                if(itemCheckEvent.getRawSlot() >= (itemCheckEvent.getInventory().getSize() - 9) && itemCheckEvent.getInventory().getSize() == 54) {
                    //has multiple pages
                    if(holder.isPaged()) {
                        //clicks on next page
                        if (itemCheckEvent.getCurrentItem().getType() == Material.LIME_STAINED_GLASS_PANE) {
                            currPage = Integer.parseInt(itemCheckEvent.getInventory().getItem(49).getItemMeta().getDisplayName().substring(5));
                            plugin.gui.nextInvPage(((Player) itemCheckEvent.getWhoClicked()), currPage);
                        }
                        //clicks on previous page
                        if (itemCheckEvent.getCurrentItem().getType() == Material.ORANGE_STAINED_GLASS_PANE) {
                            currPage = Integer.parseInt(itemCheckEvent.getInventory().getItem(49).getItemMeta().getDisplayName().substring(5));
                            plugin.gui.prevInvPage(((Player) itemCheckEvent.getWhoClicked()), currPage);
                        }
                    }
                    /* clicks on bottom right slot, which is going to open the normal shop directory if you're in normal view(0)? 
                    if you were a moderator in mode (pending/review/recover) looking at a shops content, 
                    it will put you back to that shop menu with that mode you were in */
                    if (itemCheckEvent.getCurrentItem() != null && itemCheckEvent.getRawSlot() == itemCheckEvent.getInventory().getSize() - 1) {
                        player.closeInventory();
                    }
                    return;
                }
            }
            if(type == InvType.NORMAL || type == InvType.PENDING_APPROVALS || type == InvType.REVIEW || type == InvType.RECOVER) { 
                int currPage = 1;
                //if clicks in bottom 9 slots of the inventory AND the inventory size is 54 (double chest).
                if(itemCheckEvent.getRawSlot() >= (itemCheckEvent.getInventory().getSize() - 9) && itemCheckEvent.getInventory().getSize() == 54) {
                    //has multiple pages
                    if(holder.isPaged()) {
                        //clicks on next page
                        if (itemCheckEvent.getCurrentItem().getType() == Material.LIME_STAINED_GLASS_PANE) {
                            currPage = Integer.parseInt(itemCheckEvent.getInventory().getItem(49).getItemMeta().getDisplayName().substring(5));
                            plugin.gui.nextInvPage(((Player) itemCheckEvent.getWhoClicked()), currPage);
                        }
                        //clicks on previous page
                        if (itemCheckEvent.getCurrentItem().getType() == Material.ORANGE_STAINED_GLASS_PANE) {
                            currPage = Integer.parseInt(itemCheckEvent.getInventory().getItem(49).getItemMeta().getDisplayName().substring(5));
                            plugin.gui.prevInvPage(((Player) itemCheckEvent.getWhoClicked()), currPage);
                        }
                    }
                    /* clicks on bottom right slot, which is going to open the normal shop directory if you're in normal view(0)? 
                    if you were a moderator in mode (pending/review/recover) looking at a shops content, 
                    it will put you back to that shop menu with that mode you were in */
                    if (itemCheckEvent.getCurrentItem() != null && itemCheckEvent.getRawSlot() == itemCheckEvent.getInventory().getSize() - 1) {
                        player.closeInventory();
                        if (type == InvType.NORMAL) plugin.gui.openShopDirectory(player);
                        else plugin.gui.openShopDirectoryModerator(player, type);
                    }
                    return;
                }
                //if right click AND valid item AND not barrier AND top 5 rows 
                if (itemCheckEvent.getCurrentItem() != null && itemCheckEvent.getCurrentItem().getType() != Material.AIR && itemCheckEvent.getCurrentItem().getType() != Material.BARRIER && itemCheckEvent.getRawSlot()<45) {
                    if(itemCheckEvent.isRightClick() && itemCheckEvent.getRawSlot() != itemCheckEvent.getInventory().getSize() - 1) {
                        //if fails, then currPage=1
                        try {
                            currPage = Integer.parseInt(itemCheckEvent.getInventory().getItem(49).getItemMeta().getDisplayName().substring(5));
                        }
                        catch (Exception ignored) {}
                        plugin.getShopRepo().findBetterAlternative(player, holder.getKey(), holder.getItemId((currPage-1)*45 + itemCheckEvent.getRawSlot()));
                    }
                }
                //returns to previous menu
                if (itemCheckEvent.getRawSlot() == itemCheckEvent.getInventory().getSize() - 1) {
                    player.closeInventory();
                    if (type == InvType.NORMAL) plugin.gui.openShopDirectory(player);
                    else plugin.gui.openShopDirectoryModerator(player, type);
                } 
                return;               
            }
            else if(type == InvType.INV_EDIT) {
                int currPage = 1;
                //if clicks in bottom 9 slots of the inventory AND the inventory size is 54 (double chest).
                if(itemCheckEvent.getRawSlot() >= (itemCheckEvent.getInventory().getSize() - 9) && itemCheckEvent.getInventory().getSize() == 54) {
                    //has multiple pages
                    if(holder.isPaged()) {
                        //clicks on next page
                        if (itemCheckEvent.getCurrentItem().getType() == Material.LIME_STAINED_GLASS_PANE) {
                            currPage = Integer.parseInt(itemCheckEvent.getInventory().getItem(49).getItemMeta().getDisplayName().substring(5));
                            plugin.gui.nextInvPage(((Player) itemCheckEvent.getWhoClicked()), currPage);
                        }
                        //clicks on previous page
                        if (itemCheckEvent.getCurrentItem().getType() == Material.ORANGE_STAINED_GLASS_PANE) {
                            currPage = Integer.parseInt(itemCheckEvent.getInventory().getItem(49).getItemMeta().getDisplayName().substring(5));
                            plugin.gui.prevInvPage(((Player) itemCheckEvent.getWhoClicked()), currPage);
                        }
                    }
                    //returns to previous menu
                    if (itemCheckEvent.getClick() != ClickType.DROP && itemCheckEvent.getCurrentItem() != null && itemCheckEvent.getRawSlot() == itemCheckEvent.getInventory().getSize() - 1) {
                        player.closeInventory();                      
                        plugin.gui.openShopEditMenu(player, holder.getKey());
                    }                    
                    return;
                }
                else if(itemCheckEvent.getCurrentItem() != null && itemCheckEvent.getCurrentItem().getType() != Material.AIR && itemCheckEvent.getCurrentItem().getType() != Material.BARRIER && itemCheckEvent.getRawSlot()<45) {
                    if(itemCheckEvent.getClick()==ClickType.DROP) {
                        plugin.getShopRepo().removeItem(holder.getKey(), itemCheckEvent.getCurrentItem());
                        plugin.gui.openShopInventory(player, holder.getKey(), plugin.getShopRepo().getShopName(holder.getKey()), InvType.INV_EDIT);
                    }
                    else if(itemCheckEvent.isRightClick() && itemCheckEvent.getRawSlot() != itemCheckEvent.getInventory().getSize() - 1){
                        //if fails, then currPage=1
                        try {
                            currPage = Integer.parseInt(itemCheckEvent.getInventory().getItem(49).getItemMeta().getDisplayName().substring(5));
                        }
                        catch (Exception ignored) {}
                        plugin.getShopRepo().findBetterAlternative(player, holder.getKey(), holder.getItemId((currPage-1)*45 + itemCheckEvent.getRawSlot()));   
                    }
                }
                //returns to previous menu
                if(itemCheckEvent.getClick() != ClickType.DROP && itemCheckEvent.getCurrentItem() != null && itemCheckEvent.getRawSlot() == itemCheckEvent.getInventory().getSize() - 1) {
                    player.closeInventory();                        
                    plugin.gui.openShopEditMenu(player, holder.getKey());
                }
                return;
            }
            else if(type == InvType.SHOP_MENU) {
                    /* this is your shop edit menu, where you can:
                     * (1) set description
                     * (4) see shop
                     * (7) set location
                     * (10) set display item
                     * (13) add owner
                     * (16) delete shop
                     */
                    if(itemCheckEvent.getRawSlot() == 1) {
                        if(itemCheckEvent.getClick() == ClickType.DROP) {
                            player.closeInventory();
                            plugin.getShopRepo().cancelNewDescription(player.getUniqueId().toString(), holder.getKey());
                            plugin.gui.openShopEditMenu(player, holder.getKey());
                        } else {
                            //TODO
                            player.closeInventory();
                            plugin.getProcessHandler().startSettingDescription(player, holder.getKey());
                        }
                    }
                    if(itemCheckEvent.getRawSlot() == 4) {
                        if(itemCheckEvent.isRightClick()) {
                            String input = holder.getShops().get(0).get("loc");
                            String[] parts = input.split(",");
                            String messageLink;
                            if(parts.length == 2) {       
                                messageLink = "https://map.projectnebula.network/#world;flat;" + Integer.parseInt(parts[0]) + ",64," + Integer.parseInt(parts[1]) + ";7";
                            }
                            else {      
                                messageLink = "https://map.projectnebula.network/#world;flat;" + Integer.parseInt(parts[0]) + ",64," + Integer.parseInt(parts[2]) + ";7";
                            }
                            var mm = MiniMessage.miniMessage();
                            Component parsed = mm.deserialize("<#3ed3f1>You can <hover:show_text:'<gray><underlined>" + messageLink + "</underlined>'><click:OPEN_URL:'" + messageLink + "'><#3c9aaf><underlined><bold>[click here]</bold></underlined></click></hover> <#3ed3f1>to open the location in <#ee2bd6><bold>dynmap</bold><#3ed3f1>.");
                            itemCheckEvent.getWhoClicked().sendMessage(parsed);
                        }
                        else {
                            player.closeInventory();
                            plugin.gui.openShopInventory(player, holder.getKey(), plugin.getShopRepo().getShopName(holder.getKey()), InvType.INV_EDIT);
                        }                        
                    }                    
                    if(itemCheckEvent.getRawSlot() == 7) {
                        if(itemCheckEvent.getClick() == ClickType.DROP) {
                            player.closeInventory();
                            plugin.getShopRepo().cancelNewLocation(player.getUniqueId().toString(), holder.getKey());
                            plugin.gui.openShopEditMenu(player, holder.getKey());
                        } else {
                            player.closeInventory();
                            plugin.getProcessHandler().startSettingLocation(player, holder.getKey());
                        }
                    }
                    else if (itemCheckEvent.getRawSlot() == 10) {
                        if(itemCheckEvent.getClick() == ClickType.DROP) {
                            player.closeInventory();
                            plugin.getShopRepo().cancelNewDisplayItem(player.getUniqueId().toString(), holder.getKey());
                            plugin.gui.openShopEditMenu(player, holder.getKey());
                        } else {
                            player.closeInventory();
                            plugin.getProcessHandler().startSettingDisplayItem(player, holder.getKey());
                        }
                    }
                    if(itemCheckEvent.getRawSlot() == 13) {
                        if(itemCheckEvent.getClick() == ClickType.DROP) {
                            player.closeInventory();
                            plugin.getShopRepo().cancelNewOwner(player.getUniqueId().toString(), holder.getKey());
                            plugin.gui.openShopEditMenu(player, holder.getKey());
                        } else {
                            player.closeInventory();
                            plugin.getProcessHandler().startAddingOwner(player, holder.getKey());
                        }
                    }
                    else if(itemCheckEvent.getRawSlot() == 16) {
                        plugin.getProcessHandler().startDeletingShop(player, holder.getKey());
                        player.closeInventory();
                    }
                }
            else if(type == InvType.ADD_ITEM) {
                /* this is the menu that opens when you wanna add an item that you already sell in your shop.
                 * it gives you the options to:
                 * continue adding the item
                 * remove all matching items
                 * remove 1 of the matching items
                 */
                if(itemCheckEvent.getRawSlot() == itemCheckEvent.getInventory().getSize()-7) {
                    player.closeInventory();
                    ItemStack item = holder.getItem();
                    plugin.getProcessHandler().initItemAddition(player, holder.getKey(), item);
                }

                else if(itemCheckEvent.getRawSlot() == itemCheckEvent.getInventory().getSize()-3) {
                    player.closeInventory();
                    plugin.getShopRepo().removeMatchingItems(holder.getKey(),holder.getItem().getType().getKey().getKey().toUpperCase());
                    player.closeInventory();
                    player.sendMessage(ChatColor.YELLOW + "All matching items removed");
                }

                else if(itemCheckEvent.getRawSlot()<itemCheckEvent.getInventory().getSize()-9 && itemCheckEvent.getCurrentItem()!=null && itemCheckEvent.isRightClick() && itemCheckEvent.getCurrentItem().getType()!= Material.AIR) {
                    plugin.getShopRepo().removeItem(holder.getKey(), itemCheckEvent.getCurrentItem());
                    List<ItemStack> matchingItems = plugin.getShopRepo().getMatchingItems(holder.getKey(), itemCheckEvent.getCurrentItem().getType().getKey().getKey().toUpperCase());
                    player.closeInventory();
                    plugin.gui.openItemAddMenu(player, holder.getKey(), matchingItems, itemCheckEvent.getCurrentItem());
                }
            } 
            else if(type == InvType.SEARCH && itemCheckEvent.isRightClick() && itemCheckEvent.getRawSlot() < Math.min(itemCheckEvent.getInventory().getSize(),holder.getShops().size())) {
                /* this is the menu that opens when you search for items */
                player.closeInventory();
                plugin.gui.openShopInventory(player,holder.getShops().get(itemCheckEvent.getRawSlot()).get("id"),holder.getShops().get(itemCheckEvent.getRawSlot()).get("name"),InvType.NORMAL);
            }            
        }
    }
}
