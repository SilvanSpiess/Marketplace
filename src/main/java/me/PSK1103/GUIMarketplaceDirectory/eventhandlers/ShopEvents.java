package me.PSK1103.GUIMarketplaceDirectory.eventhandlers;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.invholders.InvType;
import me.PSK1103.GUIMarketplaceDirectory.invholders.MarketplaceBookHolder;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.ChatProcess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.*;

public class ShopEvents implements Listener { 

    private final GUIMarketplaceDirectory plugin;   

    public ShopEvents(GUIMarketplaceDirectory plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public final void openShopBook(PlayerInteractEvent playerInteractEvent) {
        Player player = playerInteractEvent.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if(itemInHand.getType() != Material.WRITTEN_BOOK) 
            return;
        
        BookMeta bookMeta = (BookMeta) itemInHand.getItemMeta();
        if(bookMeta.getTitle() != null && (playerInteractEvent.getAction() == Action.RIGHT_CLICK_AIR || playerInteractEvent.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (bookMeta.getTitle().equalsIgnoreCase("[Marketplace]")) {
                Bukkit.getScheduler().runTask(plugin,()-> ShopEvents.this.plugin.gui.openShopDirectory(player));
            }
            else if (bookMeta.getTitle().equalsIgnoreCase("[shop init]") || bookMeta.getTitle().equalsIgnoreCase("[init shop]")) {
                if (!ShopEvents.this.plugin.getCustomConfig().multiOwnerEnabled()) {return;}
                if (ShopEvents.this.plugin.getShopRepo().isShopOwner(player.getUniqueId().toString(), bookMeta.getPage(bookMeta.getPageCount()))) {
                    if (ShopEvents.this.plugin.getProcessHandler().isPlayerInProcess(player.getUniqueId().toString())) {
                        ChatProcess process = this.plugin.getProcessHandler().getPlayerProcess(player.getUniqueId().toString());
                        player.sendMessage(ChatColor.RED + "Finish " + process.getName() + " first");
                        return; 
                    }
                    Bukkit.getScheduler().runTask(plugin,()->plugin.gui.openShopEditMenu(player,bookMeta.getPage(bookMeta.getPageCount())));
                }
            }
            return;
        }
    }

    @EventHandler
    public final void onShopAdd(PlayerEditBookEvent editBookEvent) {
        if(!editBookEvent.isSigning())
            return;
        BookMeta bookmeta = editBookEvent.getNewBookMeta();
        /* this function executes when you sign a book with [init shop], [shop init] or [marketplace]*/
        assert bookmeta.getTitle()!=null;
        if(bookmeta.getTitle().toLowerCase().equals("[init shop]") || bookmeta.getTitle().toLowerCase().equals("[shop init]")) {
            if (!plugin.getProcessHandler().addShop(editBookEvent.getPlayer(), editBookEvent))
                editBookEvent.setCancelled(true);
        }
        else if(bookmeta.getTitle().equalsIgnoreCase("[Marketplace]")) {
            bookmeta.setDisplayName(ChatColor.GOLD + "Marketplace Directory");
            editBookEvent.setNewBookMeta(bookmeta);
            //easter eggs
            if(bookmeta.getPage(1).contains("PSK is the best")) {
                editBookEvent.getPlayer().sendMessage(ChatColor.AQUA + "Gee thanks!");
            }
            if(bookmeta.getPage(1).contains("hello there") || bookmeta.getPage(1).contains("Hello there")) {
                editBookEvent.getPlayer().sendMessage(ChatColor.GRAY + "General Kenobi!");
            }
        }
    }

    @EventHandler
    public final void onJoin(PlayerJoinEvent e) {

    }

    @EventHandler
    public final void onLeave(PlayerQuitEvent e) {
        if (plugin.getProcessHandler().isPlayerInProcess(e.getPlayer().getUniqueId().toString())) {
            ChatProcess process = plugin.getProcessHandler().getPlayerProcess(e.getPlayer().getUniqueId().toString());
            process.cancel();
        }
    }
    

    @EventHandler
    public final void selectShop(InventoryClickEvent shopSelectEvent) {
        if(shopSelectEvent.getInventory().getHolder() instanceof MarketplaceBookHolder) {
            MarketplaceBookHolder holder = ((MarketplaceBookHolder) shopSelectEvent.getInventory().getHolder());
            shopSelectEvent.setCancelled(true);

            if(shopSelectEvent.getRawSlot() > shopSelectEvent.getInventory().getSize() || shopSelectEvent.getRawSlot() < 0) {
                shopSelectEvent.setCancelled(true);
                return;
            }

            int currPage = 1;

            if(shopSelectEvent.getRawSlot() > 44 && holder.isPaged()) {
                if(shopSelectEvent.getCurrentItem() != null && shopSelectEvent.getCurrentItem().getType() == Material.LIME_STAINED_GLASS_PANE) {
                    currPage = Integer.parseInt(((TextComponent) shopSelectEvent.getInventory().getItem(49).getItemMeta().displayName()).content().substring(5));
                    plugin.gui.nextPage(((Player) shopSelectEvent.getWhoClicked()),currPage);
                }
                if(shopSelectEvent.getCurrentItem() != null && shopSelectEvent.getCurrentItem().getType() == Material.ORANGE_STAINED_GLASS_PANE) {
                    currPage = Integer.parseInt(((TextComponent) shopSelectEvent.getInventory().getItem(49).getItemMeta().displayName()).content().substring(5));
                    plugin.gui.prevPage(((Player) shopSelectEvent.getWhoClicked()),currPage);
                }
                return;
            }

            if(shopSelectEvent.getInventory().getSize() == 54) {
                if(shopSelectEvent.getInventory().getItem(49) != null && shopSelectEvent.getInventory().getItem(49).getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
                    currPage = Integer.parseInt(((TextComponent) shopSelectEvent.getInventory().getItem(49).getItemMeta().displayName()).content().substring(5));
                }
            }
            
            if(holder.getType() == InvType.NORMAL) {
                //View the shop
                if(shopSelectEvent.isLeftClick() && shopSelectEvent.getRawSlot() + 45*(currPage - 1) < holder.getShops().size()) {
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                    return;
                }
                //sends the link of the Dynmap with the location of the selected shop to the player
                else if(shopSelectEvent.isRightClick() && shopSelectEvent.getRawSlot() + 45*(currPage - 1) < holder.getShops().size()) {                    
                    String input = holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("loc");
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
                    shopSelectEvent.getWhoClicked().sendMessage(parsed);
                    return;
                }
            }           

            else if (holder.getType() == InvType.PENDING_APPROVALS) {
                //View the shop
                if(shopSelectEvent.isShiftClick() && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()){
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                }
                //Approval of shop
                else if(shopSelectEvent.getClick() == ClickType.SWAP_OFFHAND && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()) {
                    if (plugin.getProcessHandler().isPlayerInProcess(shopSelectEvent.getWhoClicked().getUniqueId().toString())) {
                        ChatProcess process = this.plugin.getProcessHandler().getPlayerProcess(shopSelectEvent.getWhoClicked().getUniqueId().toString());
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "Finish " + process.getName() + " first");
                        return;  
                    }

                    if (plugin.getProcessHandler().isShopInProcess(holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"))) {
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "Shop is under some operation, try again later.");
                        return;  
                    }
                    plugin.getShopRepo().approveShop((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"));                    
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopDirectoryModerator(((Player) shopSelectEvent.getWhoClicked()),InvType.PENDING_APPROVALS);
                    return;
                }
                //Rejection of shop
                else if(shopSelectEvent.getClick() == ClickType.DROP && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()) {
                    plugin.getProcessHandler().startRejectingShop((Player)shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"));
                    shopSelectEvent.getWhoClicked().closeInventory();
                    return;
                }
            }
            else if (holder.getType() == InvType.PENDING_CHANGES) {
                //leftclick to switch to old shop
                if(shopSelectEvent.getClick() == ClickType.LEFT && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()){
                    plugin.gui.switchShopVersion(shopSelectEvent.getInventory(), shopSelectEvent.getRawSlot(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"), false);
                    return;
                }
                //rightclick to switch to new shop
                else if (shopSelectEvent.getClick() == ClickType.RIGHT && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()) {
                    plugin.gui.switchShopVersion(shopSelectEvent.getInventory(), shopSelectEvent.getRawSlot(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"), true);
                    return;
                }
                //drop key to reject changes
                else if (shopSelectEvent.getClick() == ClickType.DROP && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()) {
                    plugin.getProcessHandler().startRejectingChange((Player)shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"));
                    shopSelectEvent.getWhoClicked().closeInventory();
                    return;  
                }
                //swap_offhand key to approve changes
                else if (shopSelectEvent.getClick() == ClickType.SWAP_OFFHAND && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()) {
                    plugin.getProcessHandler().startApprovingChange((Player)shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"));
                    shopSelectEvent.getWhoClicked().closeInventory();
                    return;
                }
            }
            else if(holder.getType() == InvType.REVIEW) {
                //delete the shop
                if(shopSelectEvent.getClick() == ClickType.DROP && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()) {
                    plugin.getProcessHandler().startDeletingShop((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"));
                    shopSelectEvent.getWhoClicked().closeInventory();
                    return;
                }
                //open the shop inventory
                else if(shopSelectEvent.getClick() != ClickType.DROP && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()) {
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                }
                return;
            }
            else if(holder.getType() == InvType.RECOVER) {
                if(shopSelectEvent.isRightClick() && shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()) {
                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta meta = (BookMeta) book.getItemMeta();
                    Map<String,String> shopDetails = holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage-1));
                    meta.setDisplayName(ChatColor.GOLD + shopDetails.get("name"));
                    meta.setTitle("[shop init]");
                    meta.setPages("[" + shopDetails.get("name") + "]\n[" + shopDetails.get("desc") + "]",shopDetails.get("key"));
                    meta.setAuthor(plugin.getShopRepo().getOwner(shopDetails.get("key")));
                    meta.setGeneration(BookMeta.Generation.COPY_OF_ORIGINAL);
                    book.setItemMeta(meta);
                    Player player = ((Player) shopSelectEvent.getWhoClicked());
                    if(player.getInventory().firstEmpty() != -1)
                        player.getInventory().setItem(player.getInventory().firstEmpty(),book);
                    else
                        shopSelectEvent.setCursor(book);
                }

                else if(shopSelectEvent.getRawSlot() + 45 * (currPage-1) < holder.getShops().size()) {
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                }
            }
            else if (holder.getType() == InvType.ADD_ITEM) {
                if(shopSelectEvent.isRightClick()) {
                    String uuid = shopSelectEvent.getWhoClicked().getUniqueId().toString();
                    if (plugin.getProcessHandler().isPlayerInProcess(uuid)) {
                        ChatProcess process = this.plugin.getProcessHandler().getPlayerProcess(uuid);
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "Finish " + process.getName() + " first");
                        return;  
                    }

                    if (plugin.getProcessHandler().isShopInProcess(holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"))) {
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "Shop is under some operation, try again later.");
                        return;  
                    }

                    shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "This feature only works when using a db and CoreProtect integration is on");
                    shopSelectEvent.getWhoClicked().closeInventory();
                }
                else {
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                }
            }
        }
    }
}
