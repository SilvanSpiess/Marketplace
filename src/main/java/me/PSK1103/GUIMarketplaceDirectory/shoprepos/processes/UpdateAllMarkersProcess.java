package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.utils.DynmapMarkerHandler;

public class UpdateAllMarkersProcess extends ConfirmationProcess {
    private String name = "Updating all markers";
    private boolean finished = false;
    private boolean succesful = false;
    private Player player;
    private String uuid;
    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private DynmapMarkerHandler dynmapMarkerHandler;

    public UpdateAllMarkersProcess(Player player, DynmapMarkerHandler dynmapMarkerHandler, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.player = player;
        this.shopRepo = shopRepo;
        this.processHandler = processHandler;
        this.dynmapMarkerHandler = dynmapMarkerHandler;
        this.uuid = player.getUniqueId().toString();


        ConfirmationProcess.sendConfirmationMessage(player, "Do you want to (re-)create all Dynmap Shop markers?");
        player.sendMessage(ChatColor.GRAY + "This is a time and memory costly operation");
    }

    @Override
    public void executeTask(Player player) {
        dynmapMarkerHandler.addAllShopMarkers(player);
        player.sendMessage(ChatColor.GREEN + "Markers of all shops have been created successfully!");  
        processHandler.discontinueProcessOfPlayer(this, uuid);      
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
        processHandler.discontinueProcessOfPlayer(this, uuid);
        player.sendMessage(ChatColor.GRAY + "Procedure of adding all Markers has been cancelled");
    }
    
}
