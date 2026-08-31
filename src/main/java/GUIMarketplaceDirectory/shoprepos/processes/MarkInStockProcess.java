package GUIMarketplaceDirectory.shoprepos.processes;

import org.bukkit.entity.Player;

import GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import GUIMarketplaceDirectory.shoprepos.ShopRepo;
import GUIMarketplaceDirectory.shoprepos.json.items.SellableItemList;
import GUIMarketplaceDirectory.utils.MyChatColor;

public class MarkInStockProcess extends ConfirmationProcess {
    private final String name = "marking item back in stock";

    private boolean finished = false;
    private boolean succesful = false;

    private final ShopRepo shopRepo;
    private final SellableItemList itemList;
    private final ProcessHandler processHandler;
    private final Player player;
    private final String uuid;

    public MarkInStockProcess(Player player, SellableItemList itemList, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.player = player;
        this.shopRepo = shopRepo;
        this.itemList = itemList;
        this.processHandler = processHandler;
        this.uuid = player.getUniqueId().toString();
    }
    
    @Override
    public void executeTask(Player player) {
        itemList.setInStock(true);
        itemList.setOutOfStockByName(null);
        itemList.setOutOfStockByUuid(null);
        itemList.setOutOfStockSince(null);
        finished = true;
        succesful = true;
        processHandler.discontinueProcessOfPlayer(this, uuid);
        player.sendMessage(MyChatColor.GREEN + "Item is back in stock");
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
