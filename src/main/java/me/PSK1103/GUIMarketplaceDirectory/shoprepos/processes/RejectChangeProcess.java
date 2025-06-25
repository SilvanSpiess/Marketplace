package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import me.PSK1103.GUIMarketplaceDirectory.utils.MyChatColor;
import org.bukkit.entity.Player;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;

public class RejectChangeProcess extends ConfirmationProcess {
    private String name = "rejecting change"; //this can be changed to either delete or reject

    private boolean finished = false;
    private boolean succesful = false;

    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private Player player;
    private String uuid;
    private String shopKey;

    public RejectChangeProcess(Player player, String shopKey, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.player = player;
        this.uuid = player.getUniqueId().toString();
        this.shopKey = shopKey;
        this.shopRepo = shopRepo;
        this.processHandler = processHandler;

        ConfirmationProcess.sendConfirmationMessage(player,"Do you wish to reject this change?");
    }
    
    @Override 
    public void executeTask(Player player) {
        if (shopRepo.rejectChange(shopKey)) {
            player.sendMessage(MyChatColor.GREEN + "Change rejected!");
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
