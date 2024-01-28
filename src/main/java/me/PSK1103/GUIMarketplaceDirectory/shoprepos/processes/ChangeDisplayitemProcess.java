package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;

public class ChangeDisplayitemProcess implements ChatProcess{
    private int step = 0;
    private int maxStep = 1;
    private String name = "changing display item";

    private boolean finished = false;
    private boolean succesful = false;

    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private Player player;
    private String uuid;
    private String shopKey;

    private boolean moderateEnabled;

    private String displayitem;
    private static final EnumSet<Material> materialsWithoutTextures = EnumSet.noneOf(Material.class);

    static {
        materialsWithoutTextures.addAll(Arrays.asList(Material.LAVA, 
                                                      Material.WATER, 
                                                      Material.BUBBLE_COLUMN,
                                                      Material.PISTON_HEAD,
                                                      Material.MOVING_PISTON,
                                                      Material.AIR,
                                                      Material.ATTACHED_MELON_STEM, 
                                                      Material.ATTACHED_PUMPKIN_STEM));
    }

    public ChangeDisplayitemProcess(Player player, String shopKey, boolean moderateEnabled, ShopRepo shopRepo, ProcessHandler processHandler) {
        this.shopKey = shopKey;
        this.player = player;
        this.shopRepo = shopRepo;
        this.processHandler = processHandler;
        this.uuid = player.getUniqueId().toString();

        this.moderateEnabled = moderateEnabled;

        player.sendMessage(ChatColor.YELLOW + "Enter display item name (material name only, nil to cancel)");
    }

    @Override
    public boolean handleChat(Player player, String chat) {
        if (chat.equals("nil")) {
            finished = true;
            cancel();
            return true;
        } else {
            String materialName = chat.trim().replace(' ','_').toUpperCase(Locale.ROOT);
            Material material = Material.getMaterial(materialName);
            if(material != null && !materialsWithoutTextures.contains(material)) {
                displayitem = materialName;
                if (moderateEnabled) {
                    shopRepo.submitNewDisplayItem(uuid, shopKey, displayitem);
                    player.sendMessage(ChatColor.GREEN + "Submitted new display item " + ChatColor.GOLD + "\"" + material.getKey().getKey() + "\"" + ChatColor.GREEN + " for approval! Please open a shop ticket to notify staff!");
                } else {
                    shopRepo.setDescription(player, shopKey, displayitem);
                    player.sendMessage(ChatColor.GREEN + "Set shop display item to " + ChatColor.GOLD + material.getKey().getKey());
                }
                finished = true;
                succesful = true;
                processHandler.discontinueProcessOfPlayer(this, uuid);
                processHandler.discontinueProcessOfShop(this, shopKey);
                return true;
            }
            else {
                player.sendMessage(ChatColor.RED + "Item name doesn't match to a proper material name. Try again");
                player.sendMessage(ChatColor.YELLOW + "Enter display item name (material name only, nil to cancel)");
                return true;
            }
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return name;
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
}
