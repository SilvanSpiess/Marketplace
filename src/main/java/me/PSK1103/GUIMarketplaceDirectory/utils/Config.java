package me.PSK1103.GUIMarketplaceDirectory.utils;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class Config {

    private final GUIMarketplaceDirectory plugin;
    private final Logger logger;

    private String defaultShopNameColor;
    private String defaultShopDescColor;
    private String defaultShopOwnerColor;
    private String defaultShopLocColor;
    private String defaultShopULocColor;
    private String defaultShopDynmapColor;

    private int shopDetailsLengthLimit;

    private int maxUndergroundMarketLevel;

    private boolean moderateDirectory;

    private boolean moderateDisplayItem;
    private boolean moderateLocation;
    private boolean moderateDescription;
    private boolean moderateAddOwner;
    
    private String tutorialLink;
    private String modTutorialLink;

    private String dynmapServerAdress;
    private boolean enableDynmapMarkers;
    private String shopSetName;
    private String shopIconName;
    private int minZoomDynmap;
    private int maxZoomDynmap;

    private boolean multiOwner;
    private boolean allowAddingOfflinePLayer;

    private boolean filterAlternatives;

    private boolean useCoreProtect;
    private int defaultLookupRadius;
    private String lookupTime;

    private boolean enableCustomApprovalMessage;
    private String customApprovalMessage;

    private boolean enableBstats;

    public Config(@NotNull GUIMarketplaceDirectory plugin) {
        //config constructer
        this.plugin = plugin;
        logger = plugin.getLogger();
        try {
            matchConfigParams();
        }
        catch (IOException | InvalidConfigurationException e) {
            logger.severe(e.getMessage());
        }
        if (new File(plugin.getDataFolder(), "config.yml").exists()) loadCustomConfig();
        else loadDefaultConfig();
    }

    private void loadCustomConfig() {
        logger.info("Loading custom config");
        final FileConfiguration configFile = new YamlConfiguration();
        final FileConfiguration defaultConfig = new YamlConfiguration();
        // the plugin tries to load the custom config file
        try {
            defaultConfig.load(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("config.yml"))));
            configFile.load(new File(plugin.getDataFolder(), "config.yml"));

            //these are the settings for how to display the shops. It is used inside ShopEvents
            defaultShopNameColor = configFile.getString("default-shop-name-color",defaultConfig.getString("default-shop-name-color"));
            defaultShopDescColor = configFile.getString("default-shop-desc-color",defaultConfig.getString("default-shop-desc-color"));
            defaultShopOwnerColor = configFile.getString("default-shop-owner-color",defaultConfig.getString("default-shop-owner-color"));
            defaultShopLocColor = configFile.getString("default-shop-loc-color",defaultConfig.getString("default-shop-loc-color"));
            defaultShopULocColor = configFile.getString("default-shop-u-loc-color",defaultConfig.getString("default-shop-u-loc-color"));
            defaultShopDynmapColor = configFile.getString("default-shop-dynmap-color",defaultConfig.getString("default-shop-dynmap-color"));

            //sets a character limit for shop name + description
            //set to -1 for no limit
            shopDetailsLengthLimit = configFile.getInt("shop-details-length-limit",defaultConfig.getInt("shop-details-length-limit",-1));

            //sets the maximum height of the underground market
            maxUndergroundMarketLevel = configFile.getInt("max-underground-market-level",defaultConfig.getInt("max-underground-market-level",0));

            //determines whether the shops will have to be approved by staff or not. 
            //It is used in SQL database and JSONShopRepo to decide whether to put the newly added shop in PendingShops or shops.
            //It is used to decide whether to show a message to the player when making a shop.
            moderateDirectory = configFile.getBoolean("moderate-directory",defaultConfig.getBoolean("moderate-directory"));
            
            //determines whether certain changes will require staff approval
            moderateDisplayItem = configFile.getBoolean("moderate-ShopDisplayItem",defaultConfig.getBoolean("moderate-ShopDisplayItem"));
            moderateLocation = configFile.getBoolean("moderate-ShopLocation",defaultConfig.getBoolean("moderate-ShopLocation"));
            moderateDescription = configFile.getBoolean("moderate-ShopDescription",defaultConfig.getBoolean("moderate-ShopDescription"));
            moderateAddOwner = configFile.getBoolean("moderate-ShopAddOwner",defaultConfig.getBoolean("moderate-ShopAddOwner"));
           
            //these are the links that get shown with the "/gmd tutorial" command. It is used inside GUIMarketplaceCommands.
            tutorialLink = configFile.getString("tutorial-link",defaultConfig.getString("tutorial-link"));
            modTutorialLink = configFile.getString("tutorial-moderator-link",defaultConfig.getString("tutorial-moderator-link"));

            //gets called when a player makes a shop in ShopEvents
            enableCustomApprovalMessage = configFile.getBoolean("enable-custom-approval-message",defaultConfig.getBoolean("enable-custom-approval-message"));
            customApprovalMessage = configFile.getString("custom-approval-message",defaultConfig.getString("custom-approval-message"));

            //gets the Dynmap Protperties
            dynmapServerAdress = configFile.getString("dynmap-server-adress", defaultConfig.getString("dynmap-server-adress"));
            enableDynmapMarkers = configFile.getBoolean("enable-dynmap-markers", defaultConfig.getBoolean("enable-dynmap-markers"));
            shopSetName = configFile.getString("shop-set-name", defaultConfig.getString("shop-set-name"));
            shopIconName = configFile.getString("shop-icon-name", defaultConfig.getString("shop-icon-name"));
            minZoomDynmap = configFile.getInt("min-zoom-dynmap", defaultConfig.getInt("min-zoom-dynmap"));
            maxZoomDynmap = configFile.getInt("max-zoom-dynmap", defaultConfig.getInt("max-zoom-dynmap"));

            //co owner can be added to a shop. Used in ShopEvents.
            multiOwner = configFile.getBoolean("multi-owner",defaultConfig.getBoolean("multi-owner"));

            //allows adding of offline players
            allowAddingOfflinePLayer = configFile.getBoolean("allow-add-offline-players",defaultConfig.getBoolean("allow-add-offline-players"));

            //gets used in shop repo and allows you to filter. gets used in ShopRepo
            //while searching for alternatives to potions the results will only show potions with similar effects
            //while searching for alternatives to enchanted books the results will only show books with at least one type of enchant in common
            filterAlternatives = configFile.getBoolean("filter-alternatives-list",defaultConfig.getBoolean("filter-alternatives-list"));

            //core protect info
            useCoreProtect = configFile.getBoolean("use-coreprotect",defaultConfig.getBoolean("use-coreprotect", false));
            defaultLookupRadius = configFile.getInt("default-lookup-radius",defaultConfig.getInt("default-lookup-radius", 20));
            lookupTime = configFile.getString("lookup-time", defaultConfig.getString("lookup-time","7d"));

            //bstats
            enableBstats = configFile.getBoolean("enable-bstats", defaultConfig.getBoolean("enable-bstats"));

        } catch (IOException | InvalidConfigurationException e) {
            //if it fails it reverst to the default config
            logger.severe("Failed to parse custom config");
            logger.warning("Reverting to default config");
            loadDefaultConfig();
        }
    }

    private void loadDefaultConfig() {
        logger.info("Loading default config");
        final FileConfiguration defaultConfig = new YamlConfiguration();

        try {
            defaultConfig.load(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("config.yml"))));

            //these are the settings for how to display the shops. It is used inside ShopEvents
            defaultShopNameColor = defaultConfig.getString("default-shop-name-color");
            defaultShopDescColor = defaultConfig.getString("default-shop-desc-color");
            defaultShopOwnerColor = defaultConfig.getString("default-shop-owner-color");
            defaultShopLocColor = defaultConfig.getString("default-shop-loc-color");
            defaultShopULocColor = defaultConfig.getString("default-shop-u-loc-color");
            defaultShopDynmapColor = defaultConfig.getString("default-shop-dynmap-color");

            //sets a character limit for shop name + description
            //set to -1 for no limit
            shopDetailsLengthLimit = defaultConfig.getInt("shop-details-length-limit",-1);

            //sets the maximum height of the underground market
            maxUndergroundMarketLevel = defaultConfig.getInt("max-underground-market-level",0);

            //determines whether the shops will have to be approved by staff or not. 
            //It is used in SQL database and JSONShopRepo to decide whether to put the newly added shop in PendingShops or shops.
            //It is used to decide whether to show a message to the player when making a shop.
            moderateDirectory = defaultConfig.getBoolean("moderate-directory");

            //determines whether certain changes will require staff approval
            moderateDisplayItem = defaultConfig.getBoolean("moderate-ShopDisplayItem");
            moderateLocation = defaultConfig.getBoolean("moderate-ShopLocation");
            moderateDescription = defaultConfig.getBoolean("moderate-ShopDescription");
            moderateAddOwner = defaultConfig.getBoolean("moderate-ShopAddOwner");

            //these are the links that get shown with the "/gmd tutorial" command. It is used inside GUIMarketplaceCommands.
            tutorialLink = defaultConfig.getString("tutorial-link");
            modTutorialLink = defaultConfig.getString("tutorial-moderator-link");

            //gets called when a player makes a shop in ShopEvents
            enableCustomApprovalMessage = defaultConfig.getBoolean("enable-custom-approval-message");
            customApprovalMessage = defaultConfig.getString("custom-approval-message");

            //gets the Dynmap Protperties
            dynmapServerAdress = defaultConfig.getString("dynmap-server-adress");
            enableDynmapMarkers = defaultConfig.getBoolean("enable-dynmap-markers");
            shopSetName = defaultConfig.getString("shop-set-name");
            shopIconName = defaultConfig.getString("shop-icon-name");
            minZoomDynmap = defaultConfig.getInt("min-zoom-dynmap");
            maxZoomDynmap = defaultConfig.getInt("max-zoom-dynmap");

            //co owner can be added to a shop. Used in ShopEvents.
            multiOwner = defaultConfig.getBoolean("multi-owner");

            //allows adding of offline players
            allowAddingOfflinePLayer = defaultConfig.getBoolean("allow-add-offline-players");

            //gets used in shop repo and allows you to filter. gets used in ShopRepo
            //while searching for alternatives to potions the results will only show potions with similar effects
            //while searching for alternatives to enchanted books the results will only show books with at least one type of enchant in common
            filterAlternatives = defaultConfig.getBoolean("filter-alternatives-list");

            //core protect info
            useCoreProtect = defaultConfig.getBoolean("use-coreprotect", false);
            defaultLookupRadius = defaultConfig.getInt("default-lookup-radius", 20);
            lookupTime = defaultConfig.getString("lookup-time","7d");

            //bstats
            enableBstats = defaultConfig.getBoolean("enable-bstats");

        } catch (IOException | InvalidConfigurationException e) {
            logger.severe("Failed to parse custom config");
            logger.warning("Reverting to default config");
            loadDefaultConfig();
        }

    }

    private void matchConfigParams() throws IOException, InvalidConfigurationException {
        /* function that checks whether the config.yml has all the correct parameters. 
        If it is missing any, it will try to update the config.yml file to contain them. */
        //opens file
        File configFile = new File(plugin.getDataFolder(),"config.yml");
        if(!configFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }

        //gets the custom and default config as a YamlConfiguration.
        final FileConfiguration customConfig = new YamlConfiguration();
        customConfig.load(configFile);
        final FileConfiguration defaultConfig = new YamlConfiguration();
        defaultConfig.load(new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("config.yml"))));

        // gets the values inside the custom config
        Map<String,Object> customValues = customConfig.getValues(true);

        //gets a list of all the parameters
        List<String> customParams = new ArrayList<>(customValues.keySet());
        List<String> defaultParams = new ArrayList<>(defaultConfig.getValues(true).keySet());


        Collections.sort(customParams);
        Collections.sort(defaultParams);

        //and compares them
        if(customParams.equals(defaultParams)) {
            logger.info("Custom config is up-to-date"); 
        }
        else { //if they don't match, it throws a warning and tries to update the custom config.yml to contain the missing parameters.
            logger.warning("Custom config is missing some parameters\nTrying to reconstruct config.yml keeping current config values");
            InputStreamReader isr = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("config.yml"));
            BufferedReader br = new BufferedReader(isr);
            List<String> lines = new ArrayList<>();
            br.lines().forEach(line -> { //checks for each parameter in the default config, to find the corresponding one in the custom config
                boolean found = false;
                for(String k : customParams) {
                    if(line.matches("\\s*" + k + ".*")) {
                        found = true;
                        lines.add(line.substring(0,line.indexOf(k) + k.length() + 1) + " " + customValues.get(k).toString());
                        break;
                    }
                } //if it can't find a config that should be there, it adds it to the list 'lines', which will later add it to the real file.
                if(!found)
                    lines.add(line);

            });
            isr.close();
            br.close();

            //it adds the missing parameters to the custom config
            FileWriter fw = new FileWriter(configFile);
            BufferedWriter bw = new BufferedWriter(fw);
            for(String line : lines) {
                bw.write(line);
                bw.write("\n");
            }
            bw.flush();
            bw.close();
        }
    }

    public void reloadConfig() {
        try {
            matchConfigParams();
        }
        catch (IOException | InvalidConfigurationException e) {
            logger.severe(e.getMessage());
        }

        loadCustomConfig();
    }

    public String getDefaultShopNameColor() {
        return defaultShopNameColor;
    }

    public String getDefaultShopDescColor() {
        return defaultShopDescColor;
    }

    public String getDefaultShopOwnerColor() {
        return defaultShopOwnerColor;
    }

    public String getDefaultShopLocColor() {
        return defaultShopLocColor;
    }

    public String getDefaultShopULocColor() {
        return defaultShopULocColor;
    }

    public String getDefaultShopDynmapColor() {
        return defaultShopDynmapColor;
    }

    public String getTutorialLink() {
        return tutorialLink;
    }

    public String getModTutorialLink() {
        return modTutorialLink;
    }

    public boolean directoryModerationEnabled() {
        return moderateDirectory;
    }

    public boolean displayItemModerationEnabled() {
        return moderateDisplayItem;
    }

    public boolean locationModerationEnabled() {
        return moderateLocation;
    }

    public boolean descriptionModerationEnabled() {
        return moderateDescription;
    }

    public boolean addOwnerModerationEnabled() {
        return moderateAddOwner;
    }

    public int getShopDetailsLengthLimit() {
        return shopDetailsLengthLimit;
    }

    public int getMaxUndergroundMarketLevel() {
        return maxUndergroundMarketLevel;
    }

    public String getDynmapServerAdress() {
        return dynmapServerAdress;
    }            

    public boolean getEnableDynmapMarkers() {
        return enableDynmapMarkers;
    }

    public String getShopSetName() {
        return shopSetName;
    }

    public String getShopIconName() {
        return shopIconName;
    }

    public int getMinZoomDynmap() {
        return minZoomDynmap;
    }

    public int getMaxZoomDynmap() {
        return maxZoomDynmap;
    }

    public boolean multiOwnerEnabled() {
        return multiOwner;
    }

    public boolean addingOfflinePlayerAllowed() {
        return allowAddingOfflinePLayer;
    }

    public boolean customApprovalMessageEnabled() {
        return enableCustomApprovalMessage;
    }

    public String getCustomApprovalMessage() {
        return customApprovalMessage;
    }

    public boolean filterAlternatives() {
        return filterAlternatives;
    }

    public boolean useCoreProtect() {
        return useCoreProtect;
    }

    public int getDefaultLookupRadius() {
        return defaultLookupRadius;
    }

    public String getLookupTime() {
        return lookupTime;
    }

    public boolean bstatsEnabled() {
        return enableBstats;
    }

    @Override
    public String toString() {
        return "Config{" +
                "default-shop-name-color='" + defaultShopNameColor + "'\n" +
                ", default-shop-desc-color='" + defaultShopDescColor + "'\n" +
                ", default-shop-owner-color='" + defaultShopOwnerColor + "'\n" +
                ", default-shop-loc-color='" + defaultShopLocColor + "'\n" +
                ", default-shop-u-loc-color='" + defaultShopULocColor + "'\n" +
                ", default-shop-dynmap-color='" + defaultShopDynmapColor + "'\n" +
                ", shop-details-length-limit=" + shopDetailsLengthLimit + "\n" +
                ", max-underground-market-level=" + maxUndergroundMarketLevel + "\n" + 
                ", tutorial-link='" + tutorialLink + "'\n" + 
                ", tutorial-moderator-link='" + modTutorialLink + "'\n" + 
                ", moderate-directory=" + moderateDirectory + "\n" +
                ", moderate-ShopDisplayItem=" + moderateDisplayItem + "\n" +
                ", moderate-ShopLocation=" + moderateLocation + "\n" +
                ", moderate-ShopDescription=" + moderateDescription + "\n" +
                ", moderate-ShopAddOwner=" + moderateAddOwner + "\n" +
                ", dynmap-server-adress=" + dynmapServerAdress + "'\n" +
                ", enable-dynmap-markers=" + enableDynmapMarkers + "\n" +
                ", shop-set-name=" + shopSetName + "'\n" +
                ", shop-icon-name=" + shopIconName + "'\n" +
                ", min-zoom-dynmap=" + minZoomDynmap + "\n" +
                ", max-zoom-dynmap=" + maxZoomDynmap + "\n" +
                ", multi-owner=" + multiOwner + "\n" +
                ", enable-custom-approval-message=" + enableCustomApprovalMessage + "\n" +
                ", custom-approval-message='" + customApprovalMessage + "'\n" +
                ", enable-bstats=" + enableBstats + "\n" +
                '}';
    }
}