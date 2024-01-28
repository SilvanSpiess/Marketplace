package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;
import net.kyori.adventure.text.Component;

public class NewShopProcess implements ChatProcess{
    private String name = "making new shop";
    private int step = 0;
    private int maxStep = 0;
    private boolean finished = false;
    private boolean succesful = false;

    private Player player;
    private String uuid;
    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private int shopDetailsLengthLimit;
    private boolean multiOwnerEnabled;
    private boolean directoryModerationEnabled;
    private boolean customApprovalMessageEnabled;
    private String customApprovalMessage;

    private OfflinePlayer shopOwner;
    private String shopName = "";
    private String shopDescription = "";
    private String shopDisplayItem = "WRITTEN_BOOK";
    private String shopLocation;
    private String shopKey;

    public NewShopProcess(Player player, int shopDetailsLengthLimit, boolean multiOwnerEnabled, boolean directoryModerationEnabled, boolean customApprovalMessageEnabled, String customApprovalMessage, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.player = player;
        this.uuid = player.getUniqueId().toString();
        this.shopRepo = shopRepo;
        this.processHandler = processHandler;
        this.shopDetailsLengthLimit = shopDetailsLengthLimit;
        this.multiOwnerEnabled = multiOwnerEnabled;
        this.directoryModerationEnabled = directoryModerationEnabled;
        this.customApprovalMessageEnabled = customApprovalMessageEnabled;
        this.customApprovalMessage = customApprovalMessage;
    }

    public boolean studyBook(PlayerEditBookEvent editBookEvent) {
        BookMeta bookmeta = editBookEvent.getNewBookMeta();
        //puts the contents of the book into a string
        StringBuilder desc = new StringBuilder();
        for(int i = 0;i<bookmeta.getPageCount();i++) {
            desc.append(bookmeta.getPage(i+1));
        }
        String data = desc.toString().trim();
        //checks whether that string has either format '[...][...]' or '[...][...][...]' with the string sequence of symbols (except '[')
        Pattern shopInitPattern = Pattern.compile("\\[([^\\[]*)\\]\\s*\\[([^\\[]*)\\](?:\\s*\\[([^\\[]*)\\])?");
        Matcher shopInitMatcher = shopInitPattern.matcher(data);
        if(!shopInitMatcher.matches()) { //if the patern doesn't match then event gets canceled
            player.sendMessage(ChatColor.RED + "Incorrect shop initialisation, try again");
            cancel();
            return false;
        }

        //gets the strings between the [ and ] into a string.
        shopName = shopInitMatcher.group(1);
        shopDescription = shopInitMatcher.group(2);
        // if display item is specified get that in string too, if not written book is default.
        if(shopInitMatcher.group(3) != null)
            //TODO give error if display item doesn't exist
            shopDisplayItem = shopInitMatcher.group(3);
        // if there's a length limit AND the limit is exceeded, then the event gets canceled.
        if(shopDetailsLengthLimit > 0 && (shopName.length() + shopDescription.length()) > shopDetailsLengthLimit) {
            player.sendMessage(ChatColor.YELLOW + "Shop name + description length is too long (limit " + shopDetailsLengthLimit + " characters)");
            cancel();
            return false;
        }

        //gets location
        shopLocation = player.getLocation().getBlockX() + "," + player.getLocation().getBlockY() + "," + player.getLocation().getBlockZ();

        if(!multiOwnerEnabled) {
            //TODO replace function addShopAsOwner 
            shopOwner = player;
            //initialize the shop and get a key based on that (key = System.currentTimeMillis() + uuid)
            if (makeShop(editBookEvent)) {
                if(directoryModerationEnabled && customApprovalMessageEnabled) {
                    editBookEvent.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('§', customApprovalMessage));
                }
                editBookEvent.getPlayer().sendMessage(ChatColor.GOLD + "Shop initialised successfully!");
                return true;
            } else {
                cancel();
                return false;
            }
        }
        else {
            shopOwner = player;
            if (makeShop(editBookEvent)) {
                if(directoryModerationEnabled && customApprovalMessageEnabled) {
                    editBookEvent.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('§', customApprovalMessage));
                }
                editBookEvent.getPlayer().sendMessage(ChatColor.GOLD + "Shop initialised successfully!");
                return true;
            } else {
                cancel();
                return false;
            }
        }
    }

    public boolean makeShop(PlayerEditBookEvent editBookEvent) {
        BookMeta bookmeta = editBookEvent.getNewBookMeta();
        //initialize the shop and get a key based on that (key = System.currentTimeMillis() + uuid)
        shopKey = shopRepo.addShopAsOwner(shopName, shopDescription, shopOwner.getName(), shopOwner.getUniqueId().toString(), shopLocation, shopDisplayItem);
        if (shopKey == null) {
            player.sendMessage(Component.text(Color.RED + "An error occurred while adding shop"));
            cancel();
            return false;
        }
        // a page with the key for the shop gets added to the book
        bookmeta.addPage(shopKey);
        // a name gets set for the book keeping a potential player specified color in mind
        bookmeta.setDisplayName(shopName.contains("&") ? ChatColor.translateAlternateColorCodes('&',shopName) : (ChatColor.GOLD + shopName));
        editBookEvent.setNewBookMeta(bookmeta);
        return true;
    }

    @Override
    public boolean handleChat(Player player, String chat) {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public int isAtStep() {
        return step;
    }

    @Override
    public int maxStep() {
        return step;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean wasSuccesFul() {
        return succesful;
    }

    @Override
    public void cancel() {
        //since this process currently doesn't take any chat input, it doesn't need to get added to the list of processes. 
        //instead it executes immediately.
        player.sendMessage(ChatColor.GRAY + "Canceled " + getName());
    }
    
}
