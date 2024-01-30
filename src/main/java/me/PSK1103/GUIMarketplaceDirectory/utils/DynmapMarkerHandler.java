package me.PSK1103.GUIMarketplaceDirectory.utils;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;

import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;

public class DynmapMarkerHandler {

    private GUIMarketplaceDirectory plugin; 
    private ShopRepo shopRepo;   

    public DynmapMarkerHandler(GUIMarketplaceDirectory plugin, ShopRepo shopRepo) {
        this.plugin = plugin;
        this.shopRepo = shopRepo;
    }

    //creates an empty set of markers
    //Example: /dmarker addset shops maxzoom:6 minzoom:4
    public String addShopSet() {
        return "dmarker addset label:" + plugin.getCustomConfig().getShopSetName().replaceAll(" ", "_") + 
               " maxzoom:" + plugin.getCustomConfig().getMaxZoomDynmap() + 
               " minzoom:" + plugin.getCustomConfig().getMinZoomDynmap();
    }

    //deletes the existing shops set
    //Example: /dmarker deleteset shops 
    public String deleteShopSet() {
        return "dmarker deleteset label:" + plugin.getCustomConfig().getShopSetName().replaceAll(" ", "_");
    }

    //creates a new marker for the specified shop
    //Example: /dmarker add shop_name icon:shop_icon set:shops x:-23 y:78 z:-345 world:world
    public String addShopMarker(Map<String, String> shop) {
        String[] splitLoc = shop.get("loc").split(","); 
        if(splitLoc.length == 2) {
            return "dmarker add label:" + shop.get("name").replaceAll(" ", "_").
                                                           replaceAll("'", "").
                                                           replaceAll("&", "and") +
                   " icon:" + plugin.getCustomConfig().getShopIconName() +
                   " set:" + plugin.getCustomConfig().getShopSetName() +
                   " x:" + splitLoc[0] + " y:64 z:" + splitLoc[1] + " world:world";
        }
        else {
            return "dmarker add label:" + shop.get("name").replaceAll(" ", "_").
                                                           replaceAll("'", "").
                                                           replaceAll("&", "and") +
                   " icon:" + plugin.getCustomConfig().getShopIconName() +
                   " set:" + plugin.getCustomConfig().getShopSetName() +
                   " x:" + splitLoc[0] + " y:" + splitLoc[1] + " z:" + splitLoc[2] + " world:world";
        }        
    }

    public String addShopMarker(String shopKey) {
        return addShopMarker(shopRepo.getSpecificShopDetails(shopKey));
    }
    
    //creates an empty set of markers
    //Example: /dmarker delete shop_name set:shops
    public String deleteShopMarker(Map<String, String> shop) {
        return "dmarker delete label:" + shop.get("name").replaceAll(" ", "_").
                                                          replaceAll("'", "").
                                                          replaceAll("&", "and") + 
               " set:" + plugin.getCustomConfig().getShopSetName();
    }

    public String deleteShopMarker(String shopKey) {
        return deleteShopMarker(shopRepo.getSpecificShopDetails(shopKey));
    }

    //appends a description to the specified marker (shop)
    //Example: /dmarker appenddesc shop_name set:shops desc:"Shop by shop_owner, shop_desc"
    public String appendShopMarkerDescription(Map<String, String> shop) {
        return "dmarker appenddesc label:" + shop.get("name").replaceAll(" ", "_").
                                                              replaceAll("'", "").
                                                              replaceAll("&", "and") +
               " set:" + plugin.getCustomConfig().getShopSetName() +
               " desc:\"Shop by " + shop.get("owners") +
               ", " + shop.get("desc") + "\"";
    }

    public String appendShopMarkerDescription(String shopKey) {
        return appendShopMarkerDescription(shopRepo.getSpecificShopDetails(shopKey));
    }
    

    //resets the description of the specified marker (shop)
    public String resetShopMarkerDescription(Map<String, String> shop) {
        return "dmarker resetdesc " + shop.get("name").replaceAll(" ", "_").
                                                       replaceAll("'", "").
                                                       replaceAll("&", "and") +
               " set:" + plugin.getCustomConfig().getShopSetName();
    }

    public String resetShopMarkerDescription(Player player, String shopKey) {
        return resetShopMarkerDescription(shopRepo.getSpecificShopDetails(shopKey));
    }

    public void addShopMarkerCommand(Player player, String shopKey) {
        CommandExecutor commandExecutor = new CommandExecutor(player, addShopMarker(shopKey), appendShopMarkerDescription(shopKey));
        Bukkit.getScheduler().runTask(plugin, commandExecutor);
    }
    public void updateShopMarkerCommand(Player player, String shopKey) {
        CommandExecutor commandExecutor = new CommandExecutor(player, deleteShopMarker(shopKey), addShopMarker(shopKey), appendShopMarkerDescription(shopKey));
        Bukkit.getScheduler().runTask(plugin, commandExecutor);
    }
    public void deleteShopMarkerCommand(Player player, String shopKey) {
        CommandExecutor commandExecutor = new CommandExecutor(player, deleteShopMarker(shopKey));
        Bukkit.getScheduler().runTask(plugin, commandExecutor);
    }

    public void addAllShopMarkers(Player player) {
        player.sendMessage(ChatColor.GREEN + "Starting to create all dynmap shop markers");
        CommandExecutor commandExecutor0 = new CommandExecutor(player, deleteShopSet(), addShopSet());
        Bukkit.getScheduler().runTask(plugin, commandExecutor0);
        
        for(Map<String, String> shop : shopRepo.getShopDetails()) {
            CommandExecutor commandExecutor = new CommandExecutor(player, deleteShopMarker(shop), addShopMarker(shop), appendShopMarkerDescription(shop));
            Bukkit.getScheduler().runTask(plugin, commandExecutor);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } 
        }
    }
}

class CommandExecutor implements Runnable {
    private String[] commands;
    private Player player;
    public CommandExecutor(Player player, String... commands) {
        this.commands = commands;
        this.player = player;
    }
    @Override
    public void run() {
        for(String command : commands) {
            Bukkit.getServer().dispatchCommand(player, command);
        }
    }
}