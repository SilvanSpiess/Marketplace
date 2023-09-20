package me.PSK1103.GUIMarketplaceDirectory.guimd;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import me.PSK1103.GUIMarketplaceDirectory.invholders.InvType;
//import me.PSK1103.GUIMarketplaceDirectory.shoprepos.mysql.MySQLShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.utils.GUI;
import me.PSK1103.GUIMarketplaceDirectory.utils.Config;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class GUIMarketplaceCommands implements TabExecutor {

    final GUIMarketplaceDirectory plugin;
    final Logger logger;
    final Config config;

    public GUIMarketplaceCommands(GUIMarketplaceDirectory plugin) {
        this.plugin = plugin;
        this.logger = plugin.getSLF4JLogger();
        this.config = plugin.getCustomConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (label.equals("guimarketplacedirectory") || label.equals("gmd") || label.equals("guimd")) {
            if (args.length >= 3) {
                if (args[0].equals("search") || args[0].equals("s")) {
                    if(commandSender instanceof ConsoleCommandSender) {
                        commandSender.sendMessage(ChatColor.RED + "You do not have permissions to use this command");
                        return true;
                    }
                    switch (args[1]) {
                        case "shop":
                        case "s":
                            StringBuilder key = new StringBuilder();
                            for (int i = 2; i < args.length - 1; i++) {
                                key.append(args[i]);
                                key.append(' ');
                            }
                            key.append(args[args.length - 1]);
                            plugin.gui.openRefinedShopPageByName(((Player) commandSender), key.toString());
                            return true;

                        case "item":
                        case "i":
                            StringBuilder key1 = new StringBuilder();
                            for (int i = 2; i < args.length - 1; i++) {
                                key1.append(args[i]);
                                key1.append(' ');
                            }
                            key1.append(args[args.length - 1]);
                            plugin.gui.openRefinedItemInventory(((Player) commandSender), key1.toString());
                            return true;

                        case "player":
                        case "p":
                            plugin.gui.openRefinedShopPageByPlayer(((Player) commandSender), args[2]);
                            return true;
                    }
                }
                else if((args[0].equals("moderate") || args[0].equals("m")) && (args[1].equals("lookup") || args[1].equals("l"))) {
                    if((args[2].equals("set") || args[2].equals("s"))) {
                        if (plugin.getCustomConfig().useCoreProtect())
                            plugin.gui.openShopDirectoryModerator(((Player) commandSender), InvType.ADD_ITEM);
                    }
                    else if((args[2].equals("all") || args[2].equals("a")))
                        plugin.getShopRepo().lookupAllShops(((Player) commandSender));
                    return true;
                }
            }
            if (args.length == 2) {
                switch (args[0]) {
                    case "moderate":
                    case "m":
                        if (!commandSender.hasPermission("GUIMD.moderate") || commandSender instanceof ConsoleCommandSender) {
                            commandSender.sendMessage(ChatColor.RED + "You do not have permissions to use this command");
                            return true;
                        }
                        if (!plugin.getCustomConfig().directoryModerationEnabled()) {
                            commandSender.sendMessage(ChatColor.RED + "This feature is not enabled! Enable it from the config");
                            return true;
                        }
                        switch (args[1]) {
                            case "approvals":
                            case "a":
                                plugin.gui.openShopDirectoryModerator(((Player) commandSender), InvType.PENDING_APPROVALS);
                                return true;
                            case "changes":
                            case "c":
                                plugin.gui.openShopDirectoryModerator(((Player) commandSender), InvType.PENDING_CHANGES);
                                return true;

                            case "review":
                                plugin.gui.openShopDirectoryModerator(((Player) commandSender), InvType.REVIEW);
                                return true;

                            case "recover":
                                plugin.gui.openShopDirectoryModerator((Player) commandSender, InvType.RECOVER);
                                return true;

                            case "lookup": //very interesting can't find explanation about this on github and it doesn't really make sense to be the 'edit your shop' menu
                                if(plugin.getCustomConfig().useCoreProtect())
                                    plugin.gui.openShopDirectoryModerator((Player) commandSender, InvType.LOOKUP);
                                return true;

                            case "migrate":
                            case "m":
                                /* 
                                if(plugin.getShopRepo() instanceof MySQLShopRepo) {
                                    ((MySQLShopRepo) plugin.getShopRepo()).migrateJSONShops();
                                }
                                else commandSender.sendMessage(Component.text(ChatColor.RED + "Currenty using JSON shop repo, cannot migrate"));
                                */
                                commandSender.sendMessage(ChatColor.RED + "Migration is currently disabled");
                                return true;
                        }
                        break;
                }
            }
            if (args.length == 1) {
                switch (args[0]) {
                    case "help":
                        commandSender.sendMessage(ChatColor.LIGHT_PURPLE + "=============GUIMarketplaceDirectory v" + plugin.getDescription().getVersion() + "=============");
                        commandSender.sendMessage(ChatColor.GOLD + "/guimd search [item/player/shop] (key): " + ChatColor.GREEN + "Search for items via item name or shops via shop name/player name");
                        if(commandSender.hasPermission("GUIMD.dir")) {
                            commandSender.sendMessage(ChatColor.GOLD + "/guimd dir: " + ChatColor.GREEN + "Gives you a copy of the Marketplace Directory book");
                            commandSender.sendMessage(ChatColor.GOLD + "/guimd tutorial: " + ChatColor.GREEN + "Shows link to the tutorial video");
                        }
                        if(commandSender.hasPermission("GUIMD.moderate")) {
                            commandSender.sendMessage(ChatColor.GOLD + "/guimd moderate approvals: " + ChatColor.GREEN + "Shows shops requiring approval");
                            commandSender.sendMessage(ChatColor.GOLD + "/guimd moderate changes: " + ChatColor.GREEN + "Shows shop changes requiring acceptance");
                            commandSender.sendMessage(ChatColor.GOLD + "/guimd moderate review: " + ChatColor.GREEN + "Shows active shops for removal if deemed objectionable");
                            commandSender.sendMessage(ChatColor.GOLD + "/guimd moderate recover: " + ChatColor.GREEN + "Shows active shops for recovering a copy of the [shop init] book if the owner loses their copy");
                            commandSender.sendMessage(ChatColor.GOLD + "/guimd reload: " + ChatColor.GREEN + "Refreshes the plugin");  
                        }
                        return true;

                    case "r":
                    case "reload":
                        if (!(commandSender instanceof ConsoleCommandSender) && !commandSender.hasPermission("GUIMD.moderate")) {
                            commandSender.sendMessage(ChatColor.RED + "You do not have permissions to reload the marketplace directory config");
                            return true;
                        }
                        plugin.getCustomConfig().reloadConfig();
                        plugin.gui = new GUI(plugin);
                        return true;
                    case "dir":
                    case "d":
                        if(commandSender instanceof Player) {
                            Player player = (Player) commandSender;

                            if(!commandSender.hasPermission("GUIMD.dir")) {
                                player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
                                return true;
                            }
                            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "give " + player.getDisplayName() + " written_book{display:{Name:'{\"text\":\"Marketplace Directory\",\"color\":\"gold\",\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}'},title:\"[Marketplace]\",author:\"Sil and Lonne\"} 1");
                            return true;
                        }
                        commandSender.sendMessage("Use this command as player");
                        return true;
                    case "config":
                    case "c":
                        if(commandSender instanceof ConsoleCommandSender)
                            logger.info(plugin.getCustomConfig().toString());
                        return true;
                    case "tutorial":
                    case "t":
                        String tutorialLink = this.config.getTutorialLink();
                        String modTutorialLink = this.config.getModTutorialLink();
                        if(tutorialLink.length() <= 5 && modTutorialLink.length() <= 5){
                            commandSender.sendMessage(ChatColor.RED + "No tutorial video links were provided ");
                            return true;
                        }
                        else {
                            if(tutorialLink.length() > 5){
                                var mm = MiniMessage.miniMessage();
                                Component parsed = mm.deserialize("<#3ed3f1>You can <hover:show_text:'<gray><underlined>" + tutorialLink + "</underlined>'><click:OPEN_URL:'" + tutorialLink + "'><#3c9aaf><underlined><bold>[click here]</bold></underlined></click></hover> <#3ed3f1>to watch <#ee2bd6><bold>the guimd tutorial</bold><#3ed3f1>.");
                                commandSender.sendMessage(parsed);
                            }
                            if((modTutorialLink.length() > 5) && commandSender.hasPermission("GUIMD.moderate")){
                                var mm = MiniMessage.miniMessage();
                                Component parsed = mm.deserialize("<#3ed3f1>You can <hover:show_text:'<gray><underlined>" + modTutorialLink + "</underlined>'><click:OPEN_URL:'" + modTutorialLink + "'><#3c9aaf><underlined><bold>[click here]</bold></underlined></click></hover> <#3ed3f1>to watch <#ee2bd6><bold>the guimd moderator tutorial</bold><#3ed3f1>.");
                                commandSender.sendMessage(parsed);
                            }
                            return true;
                        }
                }
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (alias.equals("gmd") || alias.equals("guimd") || alias.equals("guimarketplacedirectory")) {
            List<String> hints = new ArrayList<>();

            if (args.length == 1) {
                if (args[0].length() == 0) {
                    hints.add("dir");
                    hints.add("help");
                    hints.add("tutorial");
                    if (commandSender.hasPermission("GUIMD.moderate")) {
                        hints.add("moderate");
                        hints.add("reload");
                    }
                    hints.add("search");
                } else {
                    if ("moderate".startsWith(args[0]) && commandSender.hasPermission("GUIMD.moderate")) {
                        if (args[0].equals("moderate")) {
                            //hints.add("migrate");
                            //hints.add("lookup");
                            hints.add("approvals");
                            hints.add("changes");
                            hints.add("recover");
                            hints.add("review");
                        } else
                            hints.add("moderate");
                    } else if ("reload".startsWith(args[0])) {
                        hints.add("reload");
                    } else if ("tutorial".startsWith(args[0])) {
                        hints.add("tutorial");
                    } else if ("search".startsWith(args[0])) {
                        if (args[0].equals("search")) {
                            hints.add("item");
                            hints.add("player");
                            hints.add("shop");
                        } else
                            hints.add("search");
                    } else if ("help".startsWith(args[0]) && !args[0].equals("help"))
                        hints.add("help");
                    else if ("dir".startsWith(args[0]) && !args[0].equals("dir"))
                        hints.add("dir");
                }
            }

            if (args.length == 2) {
                if (args[0].equals("moderate") && commandSender.hasPermission("GUIMD.moderate")) {
                    if (args[1].length() == 0) {
                        //hints.add("lookup");
                        //hints.add("migrate");
                        hints.add("approvals");
                        hints.add("changes");
                        hints.add("recover");
                        hints.add("review");
                    } else {
                        /*
                        if ("lookup".startsWith(args[1])) {
                            if (!args[1].equals("lookup")) {
                                hints.add("lookup");
                            }
                        }
                        if ("migrate".startsWith(args[1])) {
                            if (!args[1].equals("migrate")) {
                                hints.add("migrate");
                            }
                        } */
                        if ("approvals".startsWith(args[1])) {
                            if (!args[1].equals("approvals")) {
                                hints.add("approvals");
                            }
                        }
                        else if ("changes".startsWith(args[1])) {
                            if (!args[1].equals("changes")) {
                                hints.add("changes");
                            }
                        }
                        if ("recover".startsWith(args[1])) {
                            if (!args[1].equals("recover")) {
                                hints.add("recover");
                            }
                        }
                        if ("review".startsWith(args[1])) {
                            if (!args[1].equals("review")) {
                                hints.add("review");
                            }
                        }
                    }
                }

                if (args[0].equals("search")) {
                    if (args[1].length() == 0) {
                        hints.add("item");
                        hints.add("player");
                        hints.add("shop");
                    } else {
                        if ("item".startsWith(args[1])) {
                            if (!args[1].equals("item")) {
                                hints.add("item");
                            }
                        }
                        if ("player".startsWith(args[1])) {
                            if (!args[1].equals("player")) {
                                hints.add("player");
                            }
                        }
                        if ("shop".startsWith(args[1])) {
                            if (!args[1].equals("shop")) {
                                hints.add("shop");
                            }
                        }
                    }
                }
            }

            if (args.length == 0) {
                hints.add("dir");
                hints.add("help");
                if (commandSender.hasPermission("GUIMD.moderate")) {
                    hints.add("moderate");
                    hints.add("reload");
                }
                hints.add("search");
            }

            if(args.length == 3) {
                if(args[0].equals("moderate") && args[1].equals("lookup")) {
                    if(args[2].length() == 0) {
                        hints.add("all");
                        hints.add("set");
                    }
                    else {
                        if ("all".startsWith(args[2])) {
                            if (!args[2].equals("all")) {
                                hints.add("all");
                            }
                        }
                        if ("set".startsWith(args[2])) {
                            if (!args[2].equals("set")) {
                                hints.add("set");
                            }
                        }
                    }
                }
            }
            return hints;
        }
        return null;
    }
}