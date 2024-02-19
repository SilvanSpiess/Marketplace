package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import org.bukkit.entity.Player;

import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;


public interface ChatProcess {
    public boolean handleChat(Player player, String chat);

    public String getName();
    public String getDescription();

    public int isAtStep();
    public int maxStep();

    public boolean isFinished();
    public boolean wasSuccesFul();

    public void cancel();

    public Player getPlayer();

    public ShopRepo getShopRepo();
}
