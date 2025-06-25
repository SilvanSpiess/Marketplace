package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import me.PSK1103.GUIMarketplaceDirectory.utils.MyChatColor;
import org.bukkit.entity.Player;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;

public class RemoveShopProcess extends ConfirmationProcess{
    private String name = "removing shop"; //this can be changed to either delete or reject

    private boolean finished = false;
    private boolean succesful = false;

    private String removeKind = "remove";
    private boolean enabledDynmapMarkers = false;

    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private Player player;
    private String uuid;
    private String shopKey;

    public RemoveShopProcess(Player player, String shopKey, String removeKind, boolean enabledDynmapMarkers, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.player = player;
        this.uuid = player.getUniqueId().toString();
        this.shopKey = shopKey;
        this.shopRepo = shopRepo;
        this.processHandler = processHandler;

        this.removeKind = removeKind;
        if (removeKind.charAt(removeKind.length()-1) == 'e') {
            name = removeKind.substring(0, removeKind.length()-1) + "ing shop";
        } else {
            name = removeKind + "ing shop";
        }

        ConfirmationProcess.sendConfirmationMessage(player, "Do you wish to " + removeKind + " this shop?");
    }

    @Override 
    public void executeTask(Player player) {
        if (shopRepo.removeShop(player, shopKey)) {
            if (removeKind.charAt(removeKind.length()-1) == 'e') {
                player.sendMessage(MyChatColor.GREEN + "Shop " + removeKind + "d succesfully!");
            } else {
                player.sendMessage(MyChatColor.GREEN + "Shop " + removeKind + "ed succesfully!");
            }
            finished = true;
            succesful = true;
            processHandler.discontinueProcessOfPlayer(this, uuid);
            processHandler.discontinueProcessOfShop(this, shopKey);
        } else {
            player.sendMessage(MyChatColor.RED + "Something went wrong");
            cancel();
        }
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
        player.sendMessage(MyChatColor.GRAY + "Canceled " + getName());
    }
    
    @Override
    public ShopRepo getShopRepo() {
        return shopRepo;
    }
}
