package me.PSK1103.GUIMarketplaceDirectory.utils;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.database.Database;
import net.coreprotect.language.Phrase;
import net.coreprotect.utility.Chat;
import net.coreprotect.utility.Color;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class CoreProtectLookup {

    private final GUIMarketplaceDirectory plugin;
    private final CoreProtectAPI coreProtectAPI;
    private final Logger logger;

    public CoreProtectLookup(GUIMarketplaceDirectory plugin) {
        this.plugin = plugin;
        coreProtectAPI = getCoreProtect();
        logger = plugin.getLogger();
    }

    public void lookup(Player player, List<String> owners, Location location, int searchTime, int radius) {
        getContainerLookup(player, owners, location, searchTime, radius);
        getSessionLookup(player, owners, searchTime);
    }

    public void getContainerLookup(Player player, List<String> owners, Location location, int searchTime, int radius) {
        if(coreProtectAPI != null && coreProtectAPI.isEnabled()) {
            int time = (int) (System.currentTimeMillis()/1000);
            List<String[]> results = processData(searchTime, radius, location, null, null, owners, null, List.of(4), 0, 1, 0, 1, true);
            if(results == null) {
                player.sendMessage(ChatColor.YELLOW + "No recent record found");
                return;
            }
            for(String[] result : results) {
                int logTime = Integer.parseInt(result[0]);
                String timeStr = getTimeSince(logTime, time, true);
                String user = result[1];
                Chat.sendComponent(player, ChatColor.AQUA + user + ChatColor.RESET + " interacted " + ChatColor.GREEN + timeStr);
            }
        }
    }

    public void getSessionLookup(Player player, List<String> owners, int searchTime) {
        if(coreProtectAPI != null && coreProtectAPI.isEnabled()) {
            int time = (int) (System.currentTimeMillis()/1000);
            List<String[]> results = processData(searchTime, -1, null, null, null, owners, null, List.of(8, 1), 0, 1, 0, 1, true);
            if(results == null) {
                player.sendMessage(ChatColor.YELLOW + "No recent record found");
                return;
            }
            for(String[] result : results) {
                int logTime = Integer.parseInt(result[0]);
                String timeStr = getTimeSince(logTime, time, true);
                String user = result[1];
                int action = Integer.parseInt(result[6]);
                String status = action !=0 ? "joined" : "left";
                Chat.sendComponent(player, ChatColor.AQUA + user + ChatColor.RESET + status + ChatColor.GREEN + timeStr);
            }

        }
    }

    private CoreProtectAPI getCoreProtect() {
        Plugin coreProtectPlugin = Bukkit.getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (coreProtectPlugin == null) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) coreProtectPlugin).getAPI();
        if (!CoreProtect.isEnabled()) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 7) {
            return null;
        }

        return CoreProtect;
    }

    private List<String[]> processData(int time, int radius, Location location, List<Object> restrictBlocks, List<Object> excludeBlocks, List<String> restrictUsers, List<String> excludeUsers, List<Integer> actionList, int action, int lookup, int offset, int rowCount, boolean useLimit) {
        // You need to either specify time/radius or time/user
        List<String[]> result = new ArrayList<>();
        List<String> uuids = new ArrayList<>();
        List<String> users = new ArrayList<>();

        if (restrictUsers == null) {
            restrictUsers = new ArrayList<>();
        }

        if(restrictBlocks == null)
            restrictBlocks = new ArrayList<>();

        if(excludeBlocks == null)
            excludeBlocks = new ArrayList<>();

        if (excludeUsers == null) {
            excludeUsers = new ArrayList<>();
        }

        if (actionList == null) {
            actionList = new ArrayList<>();
        }

        if (actionList.size() == 0 && restrictBlocks.size() > 0) {
            boolean addedMaterial = false;
            boolean addedEntity = false;

            for (Object argBlock : restrictBlocks) {
                if (argBlock instanceof Material && !addedMaterial) {
                    actionList.add(0);
                    actionList.add(1);
                    addedMaterial = true;
                }
                else if (argBlock instanceof EntityType && !addedEntity) {
                    actionList.add(3);
                    addedEntity = true;
                }
            }
        }

        if (actionList.size() == 0) {
            actionList.add(0);
            actionList.add(1);
        }

        if (restrictUsers.size() == 0) {
            restrictUsers.add("#global");
        }

        long timestamp = System.currentTimeMillis() / 1000L;
        long timePeriod = timestamp - time;

        if (radius < 1) {
            radius = -1;
        }

        if (restrictUsers.contains("#global") && radius == -1) {
            return null;
        }

        if (radius > -1 && location == null) {
            return null;
        }

        try {
            Connection connection = Database.getConnection(false, 1000);
            if (connection != null) {
                Statement statement = connection.createStatement();
                boolean restrictWorld = false;

                if (radius > 0) {
                    restrictWorld = true;
                }

                if (location == null) {
                    restrictWorld = false;
                }

                Integer[] argRadius = null;
                if (location != null && radius > 0) {
                    int xMin = location.getBlockX() - radius;
                    int xMax = location.getBlockX() + radius;
                    int zMin = location.getBlockZ() - radius;
                    int zMax = location.getBlockZ() + radius;
                    argRadius = new Integer[] { radius, xMin, xMax, -1, -1, zMin, zMax, 0 };
                }

                /*if (lookup == 1) {
                    if (location != null) {
                        restrictWorld = true;
                    }

                    if (useLimit) {
                        result = Lookup.performPartialLookup(statement, null, uuids, users, restrictBlocks, null, excludeUsers, actionList, location, argRadius, timePeriod, offset, rowCount, restrictWorld, true);
                    }
                    else {
                        result = Lookup.performLookup(statement, null, uuids, restrictUsers, restrictBlocks, excludeBlocks, excludeUsers, actionList, location, argRadius, timePeriod, restrictWorld, true);
                    }
                }
                else {
                    if (!Bukkit.isPrimaryThread()) {
                        boolean verbose = false;
                        result = Rollback.performRollbackRestore(statement, null, uuids, restrictUsers, null, restrictBlocks, excludeBlocks, excludeUsers, actionList, location, argRadius, timePeriod, restrictWorld, false, verbose, action, 0);
                    }
                }*/

                statement.close();
                connection.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String getTimeSince(int logTime, int currentTime, boolean component) {
        StringBuilder message = new StringBuilder();
        double timeSince = currentTime - (logTime + 0.00);

        // minutes
        timeSince = timeSince / 60;
        if (timeSince < 60.0) {
            message.append(Phrase.build(Phrase.LOOKUP_TIME, new DecimalFormat("0.00").format(timeSince) + "/m"));
        }

        // hours
        if (message.length() == 0) {
            timeSince = timeSince / 60;
            if (timeSince < 24.0) {
                message.append(Phrase.build(Phrase.LOOKUP_TIME, new DecimalFormat("0.00").format(timeSince) + "/h"));
            }
        }

        // days
        if (message.length() == 0) {
            timeSince = timeSince / 24;
            message.append(Phrase.build(Phrase.LOOKUP_TIME, new DecimalFormat("0.00").format(timeSince) + "/d"));
        }

        if (component) {
            Date logDate = new Date(logTime * 1000L);
            String formattedTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(logDate);

            return Chat.COMPONENT_TAG_OPEN + Chat.COMPONENT_POPUP + "|" + Color.GREY + formattedTimestamp + "|" + Color.GREY + message.toString() + Chat.COMPONENT_TAG_CLOSE;
        }

        return message.toString();
    }

}
