package me.PSK1103.GUIMarketplaceDirectory.shoprepos;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.AddItemProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.AddOwnerProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.ApproveChangeProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.ChangeDescriptionProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.ChangeDisplayitemProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.ChangeLocationProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.ChatProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.NewShopProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.RejectChangeProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.RemoveShopProcess;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes.UpdateAllMarkersProcess;

public class ProcessHandler {
    private GUIMarketplaceDirectory plugin;
    private ShopRepo shopRepo;

    private final Map<String, ChatProcess> playerProcesses;
    private final Map<String, ChatProcess> shopProcesses;
    
    public ProcessHandler(GUIMarketplaceDirectory plugin, ShopRepo shopRepo) {
        this.plugin = plugin;
        this.shopRepo = shopRepo;

        playerProcesses = new HashMap<>();
        shopProcesses = new HashMap<>();
    }

    public void startSettingDescription(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if (!checkProcessAvailabilityLowPrio(player, shopKey)) 
            return;

        ChangeDescriptionProcess changeDescriptionProcess = new ChangeDescriptionProcess(player, shopKey, plugin.getCustomConfig().descriptionModerationEnabled(), shopRepo, this);
        playerProcesses.put(uuid, changeDescriptionProcess);
        shopProcesses.put(shopKey, changeDescriptionProcess);
    }

    public void startSettingLocation(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if (!checkProcessAvailabilityLowPrio(player, shopKey)) 
            return;
        ChangeLocationProcess changeLocationProcess = new ChangeLocationProcess(player, shopKey, plugin.getCustomConfig().descriptionModerationEnabled(), shopRepo, this);
        playerProcesses.put(uuid, changeLocationProcess);
        shopProcesses.put(shopKey, changeLocationProcess);
    }

    public void startAddingOwner(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if (!checkProcessAvailabilityLowPrio(player, shopKey)) 
            return;

        AddOwnerProcess addOwnerProcess = new AddOwnerProcess(player, shopKey, plugin.getCustomConfig().addOwnerModerationEnabled(), plugin.getCustomConfig().addingOfflinePlayerAllowed(), plugin, shopRepo, this);
        playerProcesses.put(uuid, addOwnerProcess);
        shopProcesses.put(shopKey, addOwnerProcess);
    }

    public void startSettingDisplayItem(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if (!checkProcessAvailabilityLowPrio(player, shopKey)) 
            return;
        ChangeDisplayitemProcess changeDisplayitemProcess = new ChangeDisplayitemProcess(player, shopKey, plugin.getCustomConfig().displayItemModerationEnabled(), shopRepo, this);
        playerProcesses.put(uuid, changeDisplayitemProcess);
        shopProcesses.put(shopKey, changeDisplayitemProcess);
    }

    //false means something went wrong
    public boolean addShop(Player player, PlayerEditBookEvent editBookEvent) {
        String uuid = player.getUniqueId().toString();
        //event is canceled if player is in a process.
        if(isPlayerInProcess(player.getUniqueId().toString())) { 
            ChatProcess process = getPlayerProcess(uuid);
            player.sendMessage(ChatColor.RED + "Finish " + process.getName() + " first");
            return false;
        }

        NewShopProcess newShopProcess = new NewShopProcess(player, plugin.getCustomConfig().getShopDetailsLengthLimit(), plugin.getCustomConfig().multiOwnerEnabled(), plugin.getCustomConfig().directoryModerationEnabled(), plugin.getCustomConfig().customApprovalMessageEnabled(), plugin.getCustomConfig().getCustomApprovalMessage(), shopRepo, this);
        if (newShopProcess.studyBook(editBookEvent)) {
            return true; //book fit the format and was processed correctly. The sign event can happen
        } else {
            return false; //Something went wrong with the format reading, cancel the signing so player can try again
        }
    }

    public void startAddingAllShopMarkers(Player player) {
        String uuid = player.getUniqueId().toString();
        if (playerProcesses.containsKey(player.getUniqueId().toString())) {
            ChatProcess process = playerProcesses.get(player.getUniqueId().toString());
            player.sendMessage(ChatColor.GRAY + "Cancelling " + process.getName());
            process.cancel();
        }
        UpdateAllMarkersProcess updateAllMarkersProcess = new UpdateAllMarkersProcess(player, plugin.getDynmapMarkerHandler(), shopRepo, this);
        playerProcesses.put(uuid, updateAllMarkersProcess);
    }

    public void startApprovingChange(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if(!checkProcessAvailabilityHighPrio(player, shopKey)) {
            return;
        }

        ApproveChangeProcess approveChangeProcess = new ApproveChangeProcess(player, shopKey, shopRepo, this);
        playerProcesses.put(uuid, approveChangeProcess);
        shopProcesses.put(shopKey, approveChangeProcess);
    }

    public void startRejectingChange(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if(!checkProcessAvailabilityHighPrio(player, shopKey)) {
            return;
        }

        RejectChangeProcess rejectChangeProcess = new RejectChangeProcess(player, shopKey, shopRepo, this);
        playerProcesses.put(uuid, rejectChangeProcess);
        shopProcesses.put(shopKey, rejectChangeProcess);
    }

    public void startRejectingShop(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if(!checkProcessAvailabilityHighPrio(player, shopKey)) {
            return;
        }

        RemoveShopProcess removeShopProcess = new RemoveShopProcess(player, shopKey, "reject", plugin.getCustomConfig().getEnableDynmapMarkers(), shopRepo, this);
        playerProcesses.put(uuid, removeShopProcess);
        shopProcesses.put(shopKey, removeShopProcess);
    }

    public void startDeletingShop(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if(!checkProcessAvailabilityHighPrio(player, shopKey)) {
            return;
        }

        RemoveShopProcess removeShopProcess = new RemoveShopProcess(player, shopKey, "delete", plugin.getCustomConfig().getEnableDynmapMarkers(), shopRepo, this);
        playerProcesses.put(uuid, removeShopProcess);
        shopProcesses.put(shopKey, removeShopProcess);
    }




    private boolean checkProcessAvailabilityHighPrio(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if (!shopRepo.shopExist(shopKey)) {
            player.sendMessage(ChatColor.RED + "shop not found!");
            return false;
        }
        if (playerProcesses.containsKey(uuid)) {
            ChatProcess process = playerProcesses.get(uuid);
            player.sendMessage(ChatColor.GRAY + "Cancelling " + process.getName());
            process.cancel();
        }

        if (shopProcesses.containsKey(shopKey)) {
            ChatProcess process = shopProcesses.get(shopKey);
            player.sendMessage(ChatColor.GRAY + "canceled " + process.getName() + " by " + process.getPlayer().getName());
            process.cancel();
        }
        return true;
    }

    private boolean checkProcessAvailabilityLowPrio(Player player, String shopKey) {
        String uuid = player.getUniqueId().toString();
        if (!shopRepo.shopExist(shopKey)) {
            player.sendMessage(ChatColor.RED + "shop not found!");
            return false;
        }
        if (playerProcesses.containsKey(uuid)) {
            ChatProcess process = playerProcesses.get(uuid);
            player.sendMessage(ChatColor.GRAY + "Cancelling " + process.getName());
            process.cancel();
        }
        if (shopProcesses.containsKey(shopKey)) {
            player.sendMessage(ChatColor.RED + "shop is already under some operation. Please try again later");
            return false;
        }
        return true;
    }

    public void initItemAddition(Player player, String shopKey, ItemStack itemStack) {
        String uuid = player.getUniqueId().toString();
        if (!checkProcessAvailabilityLowPrio(player, shopKey)) 
            return;

        AddItemProcess addItemProcess = new AddItemProcess(player, shopKey, itemStack, this.plugin, shopRepo, this);
        playerProcesses.put(uuid, addItemProcess);
        shopProcesses.put(shopKey, addItemProcess);
    }

    public void discontinueProcessOfPlayer(ChatProcess process, String uuid) {
        if(playerProcesses.containsKey(uuid))
            playerProcesses.remove(uuid);
    }

    public void discontinueProcessOfShop(ChatProcess process, String shopkey) {
        if(shopProcesses.containsKey(shopkey))
            shopProcesses.remove(shopkey);
    }

    public boolean isPlayerInProcess(String uuid) {
        return playerProcesses.containsKey(uuid);
    }

    public ChatProcess getPlayerProcess(String uuid) {
        if (playerProcesses.containsKey(uuid)) {
            return playerProcesses.get(uuid);
        } else return null;
    }

    public boolean isShopInProcess(String shopKey) {
        return shopProcesses.containsKey(shopKey);
    }

    public ChatProcess getShopProcess(String shopKey) {
        if (shopProcesses.containsKey(shopKey)) {
            return shopProcesses.get(shopKey);
        } else return null;
    }
}
