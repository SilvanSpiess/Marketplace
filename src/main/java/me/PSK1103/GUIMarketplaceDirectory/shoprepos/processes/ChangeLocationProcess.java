package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;

public class ChangeLocationProcess extends ConfirmationProcess{
    private String name = "changing location";

    private boolean finished = false;
    private boolean succesful = false;

    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private Player player;
    private String uuid;
    private String shopKey;
    private String location;

    private boolean moderateEnabled;


    public ChangeLocationProcess(Player player, String shopKey, boolean moderateEnabled, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.shopKey = shopKey;
        this.player = player;
        this.shopRepo = shopRepo;
        this.processHandler = processHandler;
        this.uuid = player.getUniqueId().toString();

        this.moderateEnabled = moderateEnabled;

        this.location = player.getLocation().getBlockX() + "," + 
                        player.getLocation().getBlockY() + "," + 
                        player.getLocation().getBlockZ();
        player.sendMessage(ChatColor.YELLOW + "Do you want to move this shop to your current location? (" +
                           ChatColor.GOLD + ChatColor.BOLD + "Y" + ChatColor.YELLOW + "/" + ChatColor.GOLD + ChatColor.BOLD + "N" + ChatColor.YELLOW + ")");

    }
    @Override
    public void executeTask(Player player) {
        if (moderateEnabled) {
            shopRepo.submitNewLocation(uuid, shopKey, location);
            player.sendMessage(ChatColor.GOLD + "Shop relocation submitted successfully! Please open a shop ticket to notify staff!");
        } else {
            if (shopRepo.setLocation(player, shopKey, location)) 
                player.sendMessage(ChatColor.GOLD + "Shop relocated successfully!");
            else
                player.sendMessage(ChatColor.GOLD + "Something went wrong");
        }
        processHandler.discontinueProcessOfPlayer(this, uuid);
        processHandler.discontinueProcessOfShop(this, shopKey);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Player getPlayer() {
        return player;
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

    @Override
    public ShopRepo getShopRepo() {
        return shopRepo;
    }    
}
