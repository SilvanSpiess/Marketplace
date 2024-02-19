package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;

public class AddOwnerProcess implements ChatProcess {
    private String name = "adding owner";
    private int step = 0;
    private int maxStep = 0;
    private boolean finished = false;
    private boolean succesful = false;

    private Player player;
    private String uuid;
    private String shopKey;
    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private boolean moderateEnabled;
    private boolean addingOfflinePlayerAllowed;
    private JavaPlugin plugin;

    public AddOwnerProcess(Player player, String shopKey, boolean moderateEnabled, boolean addingOfflinePlayerAllowed, JavaPlugin plugin, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.player = player;
        this.uuid = player.getUniqueId().toString();
        this.shopKey = shopKey;
        this.shopRepo = shopRepo;
        this.processHandler = processHandler;
        this.moderateEnabled = moderateEnabled;
        this.addingOfflinePlayerAllowed = addingOfflinePlayerAllowed;
        this.plugin = plugin;

        player.sendMessage(new String[]{ChatColor.GRAY + "Adding another owner...", ChatColor.YELLOW + "Enter player name (nil to cancel)"});
    }

    @Override
    public boolean handleChat(Player player, String chat) {
        if (chat.equalsIgnoreCase("nil")) {
            cancel();
            return true;
        } 

        String playerName = chat;

        List<OfflinePlayer> players;
        if(addingOfflinePlayerAllowed)
            try {
                players = Arrays.stream(plugin.getServer().getOfflinePlayers()).filter(offlinePlayer -> offlinePlayer.getName().toUpperCase(Locale.ROOT).startsWith(playerName)).collect(Collectors.toList());
            } catch (NullPointerException e) {
                player.sendMessage(ChatColor.RED + "Player data not found");
                players = new ArrayList<>();
            }
        else
            players = plugin.getServer().matchPlayer(playerName).stream().map(player2 -> (OfflinePlayer) player2).collect(Collectors.toList());

        if (players.size() == 0) {
            player.sendMessage(ChatColor.RED + "No player found, try again");
            return true;
        } else if (players.size() > 1) {
            player.sendMessage(ChatColor.YELLOW + "Multiple players found, be more specific");
            return true;
        } else {
            if (moderateEnabled) {
                shopRepo.submitNewOwner(shopKey, players.get(0).getUniqueId().toString(), players.get(0).getName());
                player.sendMessage(ChatColor.GREEN + "submitted " + ChatColor.GOLD + players.get(0).getName() + ChatColor.GREEN + " for approval as co-owner! please open a shop ticket to notify staff!");
            } else {
                shopRepo.addOwner(shopKey, players.get(0));
                player.sendMessage(ChatColor.GOLD + players.get(0).getName() + ChatColor.GREEN + " added as owner successfully");
            }
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
    
    @Override
    public ShopRepo getShopRepo() {
        return shopRepo;
    }
}
