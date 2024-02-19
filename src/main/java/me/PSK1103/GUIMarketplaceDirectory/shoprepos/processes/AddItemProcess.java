package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.json.ItemList;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.json.ItemList.BlockBuilder;

public class AddItemProcess implements ChatProcess {
    // 0 process initiated no data collected, waiting for amount response.
    // 1 amount determined, waiting for price
    // 2 all data collected, rounding up inititated.
    private int step = 0;
    private int maxStep = 2;
    private String name = "adding item";

    private boolean finished = false;
    private boolean succesful = false;

    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private Player player;
    private String uuid;
    private String shopKey;

    private String quantity;
    private int price;
    private ItemStack itemStack;
    private ItemList item;

    public AddItemProcess(Player player, String key, ItemStack itemStack, BlockBuilder blockBuilder, ShopRepo shoprepo, ProcessHandler processHandler) {
        this.player = player;
        this.shopKey = key;
        this.itemStack = itemStack;
        this.shopRepo = shoprepo;
        this.processHandler = processHandler;

        String name = itemStack.getType().getKey().getKey().toUpperCase();
        uuid = player.getUniqueId().toString();
        item = new ItemList(name, itemStack.getItemMeta(), blockBuilder);

        List<Integer> errorTracker = item.storeExtraInfo(itemStack);
        if (errorTracker.contains(2)) { 
            player.sendMessage(new String[]{ChatColor.YELLOW + " The enchanted item you're trying to add has illegal enchants on it. You may continue adding, however these enchants will not be seen within your shop window."});                    
        }
        player.sendMessage(ChatColor.GREEN + "Set quantity (in format shulker:stack:num)");
        player.sendMessage(ChatColor.GRAY + "Or type \"nil\" to cancel.");

    }

    @Override
    public boolean handleChat(Player player, String chat) {
        if(step == 0) {
            //if message is in format shulker:stack:num and process is waiting for quantity then 
            if (chat.matches("\\d+:\\d+:\\d+")) {
                quantity = chat;
                player.sendMessage(ChatColor.GREEN + "Enter price (in diamonds)");
                step += 1;
                return true;
            } else if (chat.equals("nil")) {
                cancel();
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "The format of your quantity string was incorrect.");
                player.sendMessage(ChatColor.GREEN + "Set quantity (in format shulker:stack:num)");
                player.sendMessage(ChatColor.GRAY + "Or type \"nil\" to cancel.");
                return true;
            }
        } else if(step == 1) {
            if (chat.matches("-?\\d+")) {
                //if message is a number
                try {
                    price = Integer.parseInt(chat);
                    step += 1;
                    item.setQty(quantity);
                    item.setPrice(price);
                    finished = true;
                    succesful = true;
                    if (shopRepo.addItemToShop(item, shopKey)) {
                        player.sendMessage(ChatColor.GOLD + "Item added successfully!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Something went wrong while adding item.");
                    }
                    processHandler.discontinueProcessOfPlayer(this, uuid);
                    processHandler.discontinueProcessOfShop(this, shopKey);
                    return true;
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "The format of your price was incorrect.");
                    player.sendMessage(ChatColor.GREEN + "Enter price (in diamonds)");
                    player.sendMessage(ChatColor.GRAY + "Or type \"nil\" to cancel.");
                    return true;
                }
            } else if (chat.equals("nil")) {
                cancel();
                player.sendMessage(ChatColor.GRAY + "Cancelled item addition");
                return true;
            }  else {
                player.sendMessage(ChatColor.RED + "The format of your price was incorrect.");
                player.sendMessage(ChatColor.GREEN + "Enter price (in diamonds)");
                player.sendMessage(ChatColor.GRAY + "Or type \"nil\" to cancel.");
                return true;
            }
        } else {
            //if message doesn't fullfill either of the criteria the item addition gets cancelled
            cancel();
            player.sendMessage(ChatColor.GRAY + "Cancelled item addition");
            return false;
        }
    }

    @Override
    public void cancel() {
        finished = true;
        processHandler.discontinueProcessOfPlayer(this, uuid);
        processHandler.discontinueProcessOfShop(this, shopKey);
        player.sendMessage(ChatColor.GRAY + "Canceled " + getName());
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
        return maxStep;
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
    public ShopRepo getShopRepo() {
        return shopRepo;
    }
}