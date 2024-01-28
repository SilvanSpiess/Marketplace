package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;

public class ApproveChangeProcess extends ConfirmationProcess{
    private String name = "approving change"; //this can be changed to either delete or reject

    private boolean finished = false;
    private boolean succesful = false;

    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private Player player;
    private String uuid;
    private String shopKey;

    public ApproveChangeProcess(Player player, String shopKey, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.player = player;
        this.uuid = player.getUniqueId().toString();
        this.shopKey = shopKey;
        this.shopRepo = shopRepo;
        this.processHandler = processHandler;

        ConfirmationProcess.sendConfirmationMessage(player,"Do you wish to approve this change?");
    }

    @Override 
    public void executeTask(Player player) {
        if (shopRepo.approveChange(player, shopKey)) {
            player.sendMessage(ChatColor.GREEN + "Change approved!");
            finished = true;
            succesful = true;
            processHandler.discontinueProcessOfPlayer(this, uuid);
            processHandler.discontinueProcessOfShop(this, shopKey);
        } else {
            player.sendMessage(ChatColor.RED + "Something went wrong");
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
        player.sendMessage(ChatColor.GRAY + "Canceled " + getName());
    }
}
