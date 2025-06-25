package me.PSK1103.GUIMarketplaceDirectory.shoprepos.processes;

import me.PSK1103.GUIMarketplaceDirectory.utils.MyChatColor;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public abstract class ConfirmationProcess implements ChatProcess{
    // 0 process initiated no data collected, waiting for amount response.
    // 1 amount determined, waiting for price
    // 2 all data collected, rounding up inititated.
    private int step = 0;
    private int maxStep = 1;

    public ConfirmationProcess() {
    }

    @Override
    public boolean handleChat(Player player, String chat) {
        if (chat.toLowerCase().equals("yes") || chat.toLowerCase().equals("y")) {
            executeTask(player);
            step += 1;
            return true;
        } if (chat.toLowerCase().equals("no") || chat.toLowerCase().equals("n")) {
            cancel();
            return true;
        } else {
            cancel();
            return false;
        }
    }

    public static void sendConfirmationMessage(Player player, String msg) {
        Component yes = Component.text(MyChatColor.GOLD + "" + MyChatColor.BOLD + "Y").clickEvent(net.kyori.adventure.text.event.ClickEvent.clickEvent(net.kyori.adventure.text.event.ClickEvent.Action.RUN_COMMAND,"Y"));
        Component no = Component.text(MyChatColor.GOLD + "" + MyChatColor.BOLD + "N").clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND,"N"));
        player.sendMessage(Component.text(msg + " (").color(NamedTextColor.YELLOW).append(yes).append(Component.text("/")).append(no).append(Component.text(")")).color(NamedTextColor.YELLOW));
    }

    public abstract void executeTask(Player player);

    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    public int isAtStep() {
        return step;
    }

    @Override
    public int maxStep() {
        return maxStep;
    }
}
