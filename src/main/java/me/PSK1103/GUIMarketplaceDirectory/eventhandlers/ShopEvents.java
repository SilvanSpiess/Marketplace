package me.PSK1103.GUIMarketplaceDirectory.eventhandlers;

import me.PSK1103.GUIMarketplaceDirectory.database.SQLDatabase;
import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.invholders.MarketplaceBookHolder;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.mysql.MySQLShopRepo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        if(bookMeta.getTitle() != null && (bookMeta.getTitle().equalsIgnoreCase("[marketplace]") || bookMeta.getTitle().equalsIgnoreCase("[shop init]") || bookMeta.getTitle().equalsIgnoreCase("[init shop]"))) {
            if (bookMeta.getTitle().equalsIgnoreCase("[Marketplace]")) {
                Bukkit.getScheduler().runTask(plugin,()-> ShopEvents.this.plugin.gui.openShopDirectory(player));
            }
            else if (bookMeta.getTitle().equalsIgnoreCase("[shop init]") || bookMeta.getTitle().equalsIgnoreCase("[init shop]")) {
                if (!ShopEvents.this.plugin.getCustomConfig().multiOwnerEnabled()) {
                    return;                                }
                if (ShopEvents.this.plugin.getShopRepo().isShopOwner(player.getUniqueId().toString(), bookMeta.getPage(bookMeta.getPageCount()))) {
                    if (ShopEvents.this.plugin.getShopRepo().isAddingItem(player.getUniqueId().toString())) {
                        player.sendMessage(ChatColor.RED + "Finish adding item first");
                        return;                                    }
                    if (ShopEvents.this.plugin.getShopRepo().getIsUserAddingOwner(player.getUniqueId().toString()) && !ShopEvents.this.plugin.getShopRepo().getIsAddingOwner(bookMeta.getPage(bookMeta.getPageCount()))) {
                        player.sendMessage(ChatColor.RED + "Finish adding owner to other shop first");
                        return;                                    }
                    if (ShopEvents.this.plugin.getShopRepo().isShopUnderEditOrAdd(bookMeta.getPage(bookMeta.getPageCount()))) {
                        player.sendMessage(ChatColor.RED + "This shop is currently under some other operation, try again later");
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
        
        /* this function executes when you sign a book with [init shop], [shop init] or [marketplace]*/
        BookMeta meta = editBookEvent.getNewBookMeta();
        assert meta.getTitle()!=null;
        if(meta.getTitle().toLowerCase().equals("[init shop]") || meta.getTitle().toLowerCase().equals("[shop init]")) {
            //puts the contents of the book into a string
            StringBuilder desc = new StringBuilder();
            for(int i = 0;i<meta.getPageCount();i++) {
                desc.append(meta.getPage(i+1));
            }
            String data = desc.toString().trim();
            //checks whether that string has either format '[...][...]' or '[...][...][...]' with the string sequence of symbols (except '[')
            Pattern shopInitPattern = Pattern.compile("\\[([^\\[]*)\\]\\s*\\[([^\\[]*)\\](?:\\s*\\[([^\\[]*)\\])?");
            Matcher shopInitMatcher = shopInitPattern.matcher(data);
            if(!shopInitMatcher.matches()) { //if the patern doesn't match then event gets canceled
                editBookEvent.getPlayer().sendMessage(ChatColor.RED + "Incorrect shop initialisation, try again");
                editBookEvent.setCancelled(true);
                return;
            }

            //event is canceled if player is in process of adding item.
            if(plugin.getShopRepo().isAddingItem(editBookEvent.getPlayer().getUniqueId().toString())) { 
                editBookEvent.getPlayer().sendMessage(ChatColor.RED + "Finish adding item first");
                editBookEvent.setCancelled(true);
                return;
            }

            String name = "", d = "", displayItem;

            displayItem = "WRITTEN_BOOK";
            //gets the strings between the [ and ] into a string.
            name = shopInitMatcher.group(1);
            d = shopInitMatcher.group(2);
            // if display item is specified get that in string too, if not written book is default.
            if(shopInitMatcher.group(3) != null)
                displayItem = shopInitMatcher.group(3);
            // if there's a length limit AND the limit is exceeded, then the event gets canceled.
            if(plugin.getCustomConfig().getShopDetailsLengthLimit() > 0 && (name.length() + d.length()) > plugin.getCustomConfig().getShopDetailsLengthLimit()) {
                editBookEvent.getPlayer().sendMessage(ChatColor.YELLOW + "Shop name + description length is too long (limit " +
                        plugin.getCustomConfig().getShopDetailsLengthLimit() + " characters)");
                editBookEvent.setCancelled(true);
                return;
            }

            //gets location
            String loc = editBookEvent.getPlayer().getLocation().getBlockX() + "," + editBookEvent.getPlayer().getLocation().getBlockZ();

            Player player = editBookEvent.getPlayer();

            if(!plugin.getCustomConfig().multiOwnerEnabled()) {
                //initialize the shop and get a key based on that (key = System.currentTimeMillis() + uuid)
                String key = plugin.getShopRepo().addShopAsOwner(name, d, player.getName(), player.getUniqueId().toString(), loc, displayItem);
                if (key == null) {
                    player.sendMessage(Component.text(Color.RED + "An error occurred while adding shop"));
                    editBookEvent.setCancelled(true);
                    return;
                }
                // a page with the key for the shop gets added to the book
                meta.addPage(key);
                // a name gets set for the book keeping a potential player specified color in mind
                meta.setDisplayName(name.contains("&") ? ChatColor.translateAlternateColorCodes('&',name) : (ChatColor.GOLD + name));
                editBookEvent.setNewBookMeta(meta);
            }
            else {
                //initialize the shop and get a key based on that (key = System.currentTimeMillis() + uuid)
                String key = plugin.getShopRepo().addShop(name, d, player.getName(), player.getUniqueId().toString(), loc,displayItem);
                if (key == null) {
                    player.sendMessage(Component.text(Color.RED + "An error occurred while adding shop"));
                    editBookEvent.setCancelled(true);
                    return;
                }
                // a page with the key for the shop gets added to the book
                meta.addPage(key);
                // a name gets set for the book keeping a potential player specified color in mind
                meta.setDisplayName(name.contains("&") ? ChatColor.translateAlternateColorCodes('&',name) : (ChatColor.GOLD + name));
                editBookEvent.setNewBookMeta(meta);
                if(plugin.getCustomConfig().directoryModerationEnabled() && plugin.getCustomConfig().customApprovalMessageEnabled()) {
                    editBookEvent.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('§', plugin.getCustomConfig().getCustomApprovalMessage()));
                }
                //asks whether player is the owner of the just initiated shop
                plugin.gui.sendConfirmationMessage(player,"Are you the owner of " + name + " ?");
                // response gets catched in ownerAddEvent
                return;
            }

            if(plugin.getCustomConfig().directoryModerationEnabled() && plugin.getCustomConfig().customApprovalMessageEnabled()) {
                editBookEvent.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('§', plugin.getCustomConfig().getCustomApprovalMessage()));
            }
            editBookEvent.getPlayer().sendMessage(ChatColor.GOLD + "Shop initialised successfully!");
        }
        else if(meta.getTitle().equalsIgnoreCase("[Marketplace]")) {
            meta.setDisplayName(ChatColor.GOLD + "Marketplace Directory");
            editBookEvent.setNewBookMeta(meta);
            //easter eggs
            if(meta.getPage(1).contains("PSK is the best")) {
                editBookEvent.getPlayer().sendMessage(ChatColor.AQUA + "Gee thanks!");
            }
            if(meta.getPage(1).contains("hello there") || meta.getPage(1).contains("Hello there")) {
                editBookEvent.getPlayer().sendMessage(ChatColor.GRAY + "General Kenobi!");
            }
        }
    }

    @EventHandler
    public final void onJoin(PlayerJoinEvent e){
        try {
            if(this.plugin.getCustomConfig().usingDB()){
                SQLDatabase.addPlayer(e.getPlayer().getUniqueId().toString(), e.getPlayer().getName());
            }
        } catch (Exception exception) {
            exception.printStackTrace();
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
                if(shopSelectEvent.getCurrentItem().getType() == Material.LIME_STAINED_GLASS_PANE) {
                    currPage = Integer.parseInt(((TextComponent) shopSelectEvent.getInventory().getItem(45).getItemMeta().displayName()).content().substring(5));
                    plugin.gui.nextPage(((Player) shopSelectEvent.getWhoClicked()),currPage);
                }
                if(shopSelectEvent.getCurrentItem().getType() == Material.ORANGE_STAINED_GLASS_PANE) {
                    currPage = Integer.parseInt(((TextComponent) shopSelectEvent.getInventory().getItem(45).getItemMeta().displayName()).content().substring(5));
                    plugin.gui.prevPage(((Player) shopSelectEvent.getWhoClicked()),currPage);
                }
                return;
            }

            if(shopSelectEvent.getInventory().getSize() == 54) {
                if(shopSelectEvent.getInventory().getItem(45).getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE && (shopSelectEvent.getInventory().getItem(46) == null || shopSelectEvent.getInventory().getItem(46).getType() == Material.AIR)) {
                    currPage = Integer.parseInt(((TextComponent) shopSelectEvent.getInventory().getItem(45).getItemMeta().displayName()).content().substring(5));
                }
            }
            
            if(holder.getType() == 0) {
                if(shopSelectEvent.isLeftClick()) {
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                    return;
                }
                else if(shopSelectEvent.isRightClick()) {
                    //sends the link of the Dynmap with the location of the selected shop to the player
                    String input = holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("loc");
                    String[] parts = input.split(",");
                    int numbers[] = {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};       
                    String messageLink = "https://map.projectnebula.network/#world;flat;" + numbers[0] + ",64," + numbers[1] + ";7";
                    var mm = MiniMessage.miniMessage();
                    Component parsed = mm.deserialize("<#3ed3f1>You can <hover:show_text:'<gray><underlined>" + messageLink + "</underlined>'><click:OPEN_URL:'" + messageLink + "'><#3c9aaf><underlined><bold>[click here]</bold></underlined></click></hover> <#3ed3f1>to open the location in <#ee2bd6><bold>dynmap</bold><#3ed3f1>.");
                    shopSelectEvent.getWhoClicked().sendMessage(parsed);
                    return;
                }
            }            

            else if (holder.getType() == 1) {
                if(shopSelectEvent.isShiftClick()){
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                }
                else if(shopSelectEvent.isRightClick()) {
                    if(plugin.getShopRepo().isUserRejectingShop(shopSelectEvent.getWhoClicked().getUniqueId().toString()) || plugin.getShopRepo().isUserRemovingShop(shopSelectEvent.getWhoClicked().getUniqueId().toString())) {
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "Confirm rejection of previous shop first!");
                        return;
                    }
                    plugin.getShopRepo().approveShop(holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"));
                    shopSelectEvent.getWhoClicked().sendMessage(ChatColor.GREEN + "Shop approved");
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopDirectoryModerator(((Player) shopSelectEvent.getWhoClicked()),1);
                    return;
                }
                else if(shopSelectEvent.isLeftClick()) {
                    if(plugin.getShopRepo().isUserRejectingShop(shopSelectEvent.getWhoClicked().getUniqueId().toString()) || plugin.getShopRepo().isUserRemovingShop(shopSelectEvent.getWhoClicked().getUniqueId().toString())) {
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "Confirm rejection of previous shop first!");
                        return;
                    }
                    plugin.getShopRepo().addShopToRejectQueue(shopSelectEvent.getWhoClicked().getUniqueId().toString(),holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"));
                    plugin.gui.sendConfirmationMessage((Player)shopSelectEvent.getWhoClicked(),"Do you wish to reject this shop?");
                    shopSelectEvent.getWhoClicked().closeInventory();
                    return;
                }
            }
            else if(holder.getType() == 2) {
                if(shopSelectEvent.isRightClick()) {
                    if(plugin.getShopRepo().isUserRemovingShop(shopSelectEvent.getWhoClicked().getUniqueId().toString())) {
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "Confirm removal of previous shop first!");
                        return;
                    }
                    plugin.getShopRepo().addShopToRemoveQueue(shopSelectEvent.getWhoClicked().getUniqueId().toString(),holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage-1)).get("key"));
                    plugin.gui.sendConfirmationMessage((Player)shopSelectEvent.getWhoClicked(),"Do you wish to remove this shop?");
                    shopSelectEvent.getWhoClicked().closeInventory();
                    return;
                }
                else {
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                }
                return;
            }
            else if(holder.getType() == 3) {

                if(shopSelectEvent.getCursor().getType() == Material.AIR && shopSelectEvent.isRightClick()) {
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

                else {
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                }

            }
            else if (holder.getType() == 4) {
                if(shopSelectEvent.isRightClick()) {
                    int finalCurrPage = currPage;
                    class LookupThread implements Runnable {
                        @Override
                        public void run() {
                            plugin.getShopRepo().lookupShop((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(finalCurrPage - 1)).get("key"));
                        }
                    }
                    new Thread(new LookupThread()).start();
                    shopSelectEvent.getWhoClicked().closeInventory();
                }
                else {
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                }
            }
            else if (holder.getType() == 5) {
                if(shopSelectEvent.isRightClick()) {
                    String uuid = shopSelectEvent.getWhoClicked().getUniqueId().toString();
                    if(plugin.getShopRepo().isUserRejectingShop(uuid) || plugin.getShopRepo().isUserRemovingShop(uuid)) {
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "Confirm removal/rejection of previous shop first!");
                        shopSelectEvent.getWhoClicked().closeInventory();
                        return;
                    }
                    if(plugin.getShopRepo() instanceof MySQLShopRepo) {
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.GREEN + "Enter new lookup radius");
                        ((MySQLShopRepo) plugin.getShopRepo()).startSettingLookupRadius(uuid, holder.getShops().get(shopSelectEvent.getRawSlot() + 45 * (currPage - 1)).get("key"));
                    }
                    else {
                        shopSelectEvent.getWhoClicked().sendMessage(ChatColor.RED + "This feature only works when using a db and CoreProtect integration is on");
                    }

                    shopSelectEvent.getWhoClicked().closeInventory();
                }
                else {
                    shopSelectEvent.getWhoClicked().closeInventory();
                    plugin.gui.openShopInventory((Player) shopSelectEvent.getWhoClicked(), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("key"), holder.getShops().get(shopSelectEvent.getRawSlot() + 45*(currPage - 1)).get("name"),holder.getType());
                }
            }

        }
    }

    @EventHandler
    public final void ownerAddEvent(AsyncPlayerChatEvent chatEvent) {

        if(plugin.getShopRepo().getIsUserAddingOwner(chatEvent.getPlayer().getUniqueId().toString())) {
            chatEvent.setCancelled(true);

            String uuid = chatEvent.getPlayer().getUniqueId().toString();
            if (plugin.getShopRepo().getEditType(uuid) <= 0) {
                return;
            }

            int editType = plugin.getShopRepo().getEditType(uuid);

            if (editType == 2) {
                if (chatEvent.getMessage().equalsIgnoreCase("Y") || chatEvent.getMessage().equalsIgnoreCase("yes")) {
                    plugin.getShopRepo().addOwner(uuid, chatEvent.getPlayer());
                    chatEvent.getPlayer().sendMessage(ChatColor.GOLD + "Shop initialised successfully!");
                } else if (chatEvent.getMessage().equalsIgnoreCase("n") || (chatEvent.getMessage().equalsIgnoreCase("no"))) {
                    plugin.getShopRepo().initShopOwnerAddition(uuid);
                    chatEvent.getPlayer().sendMessage(ChatColor.YELLOW + "Enter owner's name (type nil to cancel)");
                } else {
                    chatEvent.getPlayer().sendMessage(ChatColor.GRAY + "Didn't get proper response");
                    plugin.gui.sendConfirmationMessage(chatEvent.getPlayer(),"Are you the owner of this shop?");
                }
            }
            else if (editType == 1 || editType == 5) {

                if (chatEvent.getMessage().equalsIgnoreCase("nil")) {
                    plugin.getShopRepo().stopInitOwner(uuid);
                    chatEvent.getPlayer().sendMessage(ChatColor.GRAY + "Cancelled adding another owner");
                    return;
                }
                String playerName = chatEvent.getMessage();

                List<OfflinePlayer> players;
                if(plugin.getCustomConfig().addingOfflinePlayerAllowed())
                    try {
                        players = Arrays.stream(plugin.getServer().getOfflinePlayers()).filter(offlinePlayer -> offlinePlayer.getName().toUpperCase(Locale.ROOT).startsWith(playerName)).collect(Collectors.toList());
                    } catch (NullPointerException e) {
                        chatEvent.getPlayer().sendMessage(ChatColor.RED + "Player data not found");
                        players = new ArrayList<>();
                    }
                else
                    players = plugin.getServer().matchPlayer(playerName).stream().map(player -> (OfflinePlayer) player).collect(Collectors.toList());


                if (players.size() == 0) {
                    chatEvent.getPlayer().sendMessage(ChatColor.RED + "No player found, try again");
                } else if (players.size() > 1) {
                    chatEvent.getPlayer().sendMessage(ChatColor.YELLOW + "Multiple players found, be more specific");
                } else {
                    plugin.getShopRepo().addOwner(uuid, players.get(0));
                    chatEvent.getPlayer().sendMessage(ChatColor.GOLD + players.get(0).getName() + " successfully added as owner");
                }
            }
            else if(editType == 3) {
                if (chatEvent.getMessage().equalsIgnoreCase("nil")) {
                    plugin.getShopRepo().stopInitOwner(uuid);
                    chatEvent.getPlayer().sendMessage(ChatColor.GRAY + "Cancelled setting display item");
                    return;
                }

                String materialName = chatEvent.getMessage().trim().replace(' ','_').toUpperCase(Locale.ROOT);
                Material trial = Material.getMaterial(materialName);
                if(trial != null) {
                    chatEvent.getPlayer().sendMessage(ChatColor.GREEN + "Setting shop display item to " + ChatColor.GOLD + trial.getKey().getKey());
                    plugin.getShopRepo().setDisplayItem(uuid,materialName);
                }
                else {
                    chatEvent.getPlayer().sendMessage(ChatColor.RED + "Item name doesn't match to a proper material name. Try again");
                    chatEvent.getPlayer().sendMessage(ChatColor.YELLOW + "Enter display item name (material name only, nil to cancel)");
                    return;
                }
            }
            else if (editType == 7) {
                String rad = chatEvent.getMessage();
                int radius;
                try {
                    radius = Integer.parseInt(rad);
                    if (radius <= 0)
                        throw new IllegalArgumentException("number less than 0");
                }
                catch (NumberFormatException e) {
                    chatEvent.getPlayer().sendMessage(ChatColor.RED + "Enter a number");
                    return;
                }
                catch (IllegalArgumentException e) {
                    chatEvent.getPlayer().sendMessage(ChatColor.RED + "Enter a positive number");
                    return;
                }
                if (!(plugin.getShopRepo() instanceof MySQLShopRepo)) {
                    chatEvent.getPlayer().sendMessage(ChatColor.RED + "Cannot do this operation with JSON shop repo");
                    return;
                }
                chatEvent.getPlayer().sendMessage("Setting lookup radius to " + ChatColor.AQUA + radius);
                ((MySQLShopRepo) plugin.getShopRepo()).setLookupRadius(uuid, radius);

            }
            return;
        }
        if(plugin.getShopRepo().isUserRejectingShop(chatEvent.getPlayer().getUniqueId().toString())) {
            chatEvent.setCancelled(true);
            String message = chatEvent.getMessage();
            if(message.equalsIgnoreCase("y") || message.equalsIgnoreCase("yes")) {
                plugin.getShopRepo().rejectShop(chatEvent.getPlayer().getUniqueId().toString());
                chatEvent.getPlayer().sendMessage(ChatColor.GREEN + "Rejected shop successfully");
            }
            else if(message.equalsIgnoreCase("n") || message.equalsIgnoreCase("no")) {
                plugin.getShopRepo().cancelRejectShop(chatEvent.getPlayer().getUniqueId().toString());
                chatEvent.getPlayer().sendMessage(ChatColor.YELLOW + "Cancelled shop rejection");
            }
            else {
                plugin.getShopRepo().cancelRejectShop(chatEvent.getPlayer().getUniqueId().toString());
                chatEvent.getPlayer().sendMessage(ChatColor.GRAY + "Didn't get proper response, cancelling rejection");
            }
            return;
        }

        if(plugin.getShopRepo().isUserRemovingShop(chatEvent.getPlayer().getUniqueId().toString())) {
            chatEvent.setCancelled(true);
            String message = chatEvent.getMessage();
            if(message.equalsIgnoreCase("y") || message.equalsIgnoreCase("yes")) {
                plugin.getShopRepo().removeShop(chatEvent.getPlayer().getUniqueId().toString());
                chatEvent.getPlayer().sendMessage(ChatColor.GREEN + "Removed shop successfully");
            }
            else if(message.equalsIgnoreCase("n") || message.equalsIgnoreCase("no")) {
                plugin.getShopRepo().cancelRemoveShop(chatEvent.getPlayer().getUniqueId().toString());
                chatEvent.getPlayer().sendMessage(ChatColor.YELLOW + "Cancelled shop removal");
            }
            else {
                plugin.getShopRepo().cancelRemoveShop(chatEvent.getPlayer().getUniqueId().toString());
                chatEvent.getPlayer().sendMessage(ChatColor.GRAY + "Didn't get proper response, cancelling removal");
            }
        }
    }

}
