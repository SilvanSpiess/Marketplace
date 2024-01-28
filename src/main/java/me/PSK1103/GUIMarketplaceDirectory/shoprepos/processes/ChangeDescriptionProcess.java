package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.json.ItemList;

public class ChangeDescriptionProcess implements ChatProcess {
    private int step = 0;
    private int maxStep = 1;
    private String name = "changing description";

    private boolean finished = false;
    private boolean succesful = false;

    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private Player player;
    private String uuid;
    private String shopKey;

    private boolean moderateEnabled;

    private String description;

    public ChangeDescriptionProcess(Player player, String shopKey, boolean moderateEnabled, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.shopKey = shopKey;
        this.player = player;
        this.uuid = player.getUniqueId().toString();
        this.shopRepo = shopRepo;
        this.processHandler = processHandler;

        this.moderateEnabled = moderateEnabled;

        player.sendMessage(ChatColor.YELLOW + "Enter new description (nil to cancel)");
        player.sendMessage(ChatColor.GRAY + "Do not use the '&' symbol");
    }

    @Override
    public boolean handleChat(Player player, String chat) {
        if (chat.equals("nil")) {
            finished = true;
            cancel();
            return true;
        } else {
            description = chat;
            if (moderateEnabled) {
                shopRepo.submitNewDescription(uuid, shopKey, description);
                player.sendMessage(ChatColor.GREEN + "Shop description submitted for approval! Please open a shop ticket to notify staff!");
            } else {
                shopRepo.setDescription(player, shopKey, description);
                player.sendMessage(ChatColor.GREEN + "Shop description has been changed!");  
            }
            finished = true;
            succesful = true;
            processHandler.discontinueProcessOfPlayer(this, uuid);
            processHandler.discontinueProcessOfShop(this, shopKey);
            return true;
        }
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
    public void cancel() {
        finished = true;
        processHandler.discontinueProcessOfPlayer(this, uuid);
        processHandler.discontinueProcessOfShop(this, shopKey);
        player.sendMessage(ChatColor.GRAY + "Canceled " + getName());
    }    
}
