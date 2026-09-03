package GUIMarketplaceDirectory.shoprepos.processes;

import java.time.LocalDateTime;

import org.bukkit.entity.Player;

import GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import GUIMarketplaceDirectory.shoprepos.ShopRepo;
import GUIMarketplaceDirectory.shoprepos.json.items.Sellable;
import GUIMarketplaceDirectory.utils.MyChatColor;

public class MarkOutOfStockProcess extends ConfirmationProcess {
    private final String name = "marking item out of stock";

    private boolean finished = false;
    private boolean succesful = false;

    private final ShopRepo shopRepo;
    private final Sellable itemList;
    private final ProcessHandler processHandler;
    private final Player player;
    private final String uuid;

    public MarkOutOfStockProcess(Player player, Sellable itemList, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.player = player;
        this.shopRepo = shopRepo;
        this.itemList = itemList;
        this.processHandler = processHandler;
        this.uuid = player.getUniqueId().toString();
        player.sendMessage(MyChatColor.YELLOW + "Do you want to mark this item out of stock? (" +
                           MyChatColor.GOLD + MyChatColor.BOLD + "Y" + MyChatColor.YELLOW + "/" + MyChatColor.GOLD + MyChatColor.BOLD + "N" + MyChatColor.YELLOW + ")");
    }
    
    @Override
    public void executeTask(Player player) {
        shopRepo.markItemOutOfStock(itemList, player.getName(), player.getUniqueId().toString(), LocalDateTime.now());
        finished = true;
        succesful = true;
        processHandler.discontinueProcessOfPlayer(this, uuid);
        player.sendMessage(MyChatColor.GREEN + "Item was marked out of stock");
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
        succesful = false;
        processHandler.discontinueProcessOfPlayer(this, uuid);
        player.sendMessage(MyChatColor.GRAY + "Canceled " + getName());
    }

    @Override
    public ShopRepo getShopRepo() {
        return shopRepo;
    } 
    
}
