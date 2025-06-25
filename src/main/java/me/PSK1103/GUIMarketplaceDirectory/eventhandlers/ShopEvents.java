package me.PSK1103.GUIMarketplaceDirectory.eventhandlers;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.invholders.InvType;
import me.PSK1103.GUIMarketplaceDirectory.invholders.MarketplaceBookHolder;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.ChatProcess;
import me.PSK1103.GUIMarketplaceDirectory.utils.GUI;
import me.PSK1103.GUIMarketplaceDirectory.utils.MyChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
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
                Bukkit.getScheduler().runTask(plugin,()-> {
                    List<Map<String,String>> shops = plugin.getShopRepo().getShopDetails();
                    //Creates the directory of all the shops (first page)
                    Inventory shopDirectory = GUI.makeShopInventory("Marketplace Directory", shops, InvType.NORMAL, plugin.getCustomConfig(), null);
                    player.openInventory(shopDirectory);
                });
            }
            else if (bookMeta.getTitle().equalsIgnoreCase("[shop init]") || bookMeta.getTitle().equalsIgnoreCase("[init shop]")) {
                if (!ShopEvents.this.plugin.getCustomConfig().multiOwnerEnabled()) {return;}
                if (ShopEvents.this.plugin.getShopRepo().isShopOwner(player.getUniqueId().toString(), bookMeta.getPage(bookMeta.getPageCount()))) {
                    if (ShopEvents.this.plugin.getProcessHandler().isPlayerInProcess(player.getUniqueId().toString())) {
                        ChatProcess process = this.plugin.getProcessHandler().getPlayerProcess(player.getUniqueId().toString());
                        player.sendMessage(MyChatColor.RED + "Finish " + process.getName() + " first");
                        return; 
                    }
                    Bukkit.getScheduler().runTask(plugin,()->{
                        String key = bookMeta.getPage(bookMeta.getPageCount());
                        Map<String,String> thisShop = plugin.getShopRepo().getSpecificShopDetails(key);
                        Map<String,String> pendingChangesShop = plugin.getShopRepo().getSpecificChangeDetails(key);
                        Inventory shopEditMenuInv = GUI.makeShopEditMenu(plugin.getShopRepo().getShopTitle(key), key, thisShop, pendingChangesShop, plugin.getCustomConfig(), null);
                        player.openInventory(shopEditMenuInv);
                    });
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
            bookmeta.setDisplayName(MyChatColor.GOLD + "Marketplace Directory");
            editBookEvent.setNewBookMeta(bookmeta);
            //easter eggs
            if(bookmeta.getPage(1).contains("PSK is the best")) {
                editBookEvent.getPlayer().sendMessage(MyChatColor.AQUA + "Gee thanks!");
            }
            if(bookmeta.getPage(1).contains("hello there") || bookmeta.getPage(1).contains("Hello there")) {
                editBookEvent.getPlayer().sendMessage(MyChatColor.GRAY + "General Kenobi!");
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
            shopSelectEvent.setCancelled(true);
            MarketplaceBookHolder holder = ((MarketplaceBookHolder) shopSelectEvent.getInventory().getHolder());
            int slotNum = shopSelectEvent.getRawSlot();

            GUI.Action action = GUI.getButtonAction(shopSelectEvent.getInventory(), holder, slotNum, shopSelectEvent.getClick(), shopSelectEvent.getCurrentItem());
            int currPage = 0;
            Player player = (Player) shopSelectEvent.getWhoClicked();
            try {
                currPage = Integer.parseInt(((TextComponent) shopSelectEvent.getInventory().getItem(49).getItemMeta().displayName()).content().substring(5))-1;
            } catch (Exception e) {}
            switch (action) {
                case NOTHING: break;
                case NEXT_PAGE:
                    shopSelectEvent.getInventory().clear();
                    GUI.fillShopInventory(shopSelectEvent.getInventory(), holder, currPage+1, plugin.getCustomConfig());
                    player.updateInventory();
                break;
                case PREVIOUS_PAGE:
                    shopSelectEvent.getInventory().clear();
                    GUI.fillShopInventory(shopSelectEvent.getInventory(), holder, currPage-1, plugin.getCustomConfig());
                    player.updateInventory();
                break;
                case GO_BACK:
                    Inventory backInventory = holder.makePreviousInventory();
                    if (backInventory == null) 
                        player.sendMessage("No back page found.");
                    else {
                        player.closeInventory();
                        player.openInventory(holder.makePreviousInventory());
                    }
                break;
                case OPEN_SHOP:
                    player.closeInventory();
                    String key = holder.getShops().get(slotNum + 45*currPage).get("key");
                    String name = holder.getShops().get(slotNum + 45*currPage).get("name");
                    List<ItemStack> inv = plugin.getShopRepo().getShopInv(key);
                    Inventory shopInventory = GUI.makeItemInventory(plugin.getShopRepo().isPendingShop(key) ? Component.text(name+ " §5§o(pending)") : Component.text(name), key, inv, InvType.NORMAL, plugin.getCustomConfig(), holder.getInventoryMaker());
                    player.openInventory(shopInventory);
                break;
                case DYNMAP:
                    String input = holder.getShops().get(slotNum + 45*currPage).get("loc");
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
                break;
                case APPROVE_SHOP: {
                    if (plugin.getProcessHandler().isPlayerInProcess(player.getUniqueId().toString())) {
                        ChatProcess process = this.plugin.getProcessHandler().getPlayerProcess(player.getUniqueId().toString());
                        player.sendMessage(MyChatColor.RED + "Finish " + process.getName() + " first");
                        return;  
                    }

                    if (plugin.getProcessHandler().isShopInProcess(holder.getShops().get(slotNum + 45 * currPage).get("key"))) {
                        player.sendMessage(MyChatColor.RED + "Shop is under some operation, try again later.");
                        return;  
                    }
                    plugin.getShopRepo().approveShop(player, holder.getShops().get(slotNum + 45 * currPage).get("key"));                    
                    player.closeInventory();
                    Inventory shopDirectory = GUI.makeShopInventory("GMD pending approvals", plugin.getShopRepo().getPendingShopDetails(), InvType.PENDING_APPROVALS, plugin.getCustomConfig(), holder.getPreviousInventoryMaker());
                    player.openInventory(shopDirectory);
                } break;
                case REJECT_SHOP:
                    plugin.getProcessHandler().startRejectingShop(player, holder.getShops().get(slotNum + 45 * currPage).get("key"));
                    player.closeInventory();
                break;
                case APPROVE_CHANGES:
                    plugin.getProcessHandler().startApprovingChange(player, holder.getShops().get(slotNum + 45 * currPage).get("key"));
                    player.closeInventory();
                break;
                case REJECT_CHANGES:
                    plugin.getProcessHandler().startRejectingChange(player, holder.getShops().get(slotNum + 45 * currPage).get("key"));
                    player.closeInventory();
                break;
                case SHOW_CHANGES: {
                    String shopKey = holder.getShops().get(slotNum + 45 * currPage).get("key");
                    GUI.switchShopVersion(shopSelectEvent.getInventory(), slotNum, plugin.getShopRepo().getSpecificChangeDetails(shopKey), plugin.getCustomConfig());
                } break;
                case UNSHOW_CHANGES:{
                    String shopKey = holder.getShops().get(slotNum + 45 * currPage).get("key");
                    GUI.switchShopVersion(shopSelectEvent.getInventory(), slotNum, plugin.getShopRepo().getSpecificShopDetails(shopKey), plugin.getCustomConfig());
                } break;
                case DELETE_SHOP:
                    plugin.getProcessHandler().startDeletingShop(player, holder.getShops().get(slotNum + 45*currPage).get("key"));
                    player.closeInventory();
                break;
                case GET_SHOP_BOOK:
                    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                    BookMeta meta = (BookMeta) book.getItemMeta();
                    Map<String,String> shopDetails = holder.getShops().get(slotNum + 45*currPage);
                    meta.setDisplayName(MyChatColor.GOLD + shopDetails.get("name"));
                    meta.setTitle("[shop init]");
                    meta.setPages("[" + shopDetails.get("name") + "]\n[" + shopDetails.get("desc") + "]",shopDetails.get("key"));
                    meta.setAuthor(plugin.getShopRepo().getOwner(shopDetails.get("key")));
                    meta.setGeneration(BookMeta.Generation.COPY_OF_ORIGINAL);
                    book.setItemMeta(meta);
                    if(player.getInventory().firstEmpty() != -1)
                        player.getInventory().setItem(player.getInventory().firstEmpty(),book);
                    else
                        shopSelectEvent.setCursor(book);
                break;
                default:
                    System.out.println("error: " + action.toString() + " was unsuccesful");
                break;
            }
        }
    }
}
