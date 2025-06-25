package me.PSK1103.GUIMarketplaceDirectory;

import me.PSK1103.GUIMarketplaceDirectory.eventhandlers.ItemEvents;
import me.PSK1103.GUIMarketplaceDirectory.eventhandlers.ShopEvents;
import me.PSK1103.GUIMarketplaceDirectory.guimd.GUIMarketplaceCommands;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ProcessHandler;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.utils.DynmapMarkerHandler;
import me.PSK1103.GUIMarketplaceDirectory.utils.Config;
import me.PSK1103.GUIMarketplaceDirectory.utils.Metrics;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.json.JSONShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.json.ItemList.BlockBuilder;

import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class GUIMarketplaceDirectory extends JavaPlugin implements BlockBuilder {

    File shops = null;
    private ShopRepo shopRepo;
    private ProcessHandler processHandler;
    private DynmapMarkerHandler dynmapMarkerHandler;
    private Config config;
    private Metrics metrics;
    private Logger logger;
    private Server server;

    private static final int pluginId = 9879;

    @Override
    public void onEnable() {
        server = getServer();
        logger = getLogger();
        saveDefaultConfig();
        config = new Config(this);
        if(config.bstatsEnabled())
            metrics = new Metrics(this, pluginId);
       
        shopRepo = new JSONShopRepo(this);
        processHandler = new ProcessHandler(this, shopRepo);
        if (config.getEnableDynmapMarkers()) {
            dynmapMarkerHandler = new DynmapMarkerHandler(this, shopRepo);
        }

        getServer().getPluginManager().registerEvents(new ShopEvents(this),this);
        getServer().getPluginManager().registerEvents(new ItemEvents(this),this);
        getCommand("GUIMD").setExecutor(new GUIMarketplaceCommands(this));
    }



    @Override
    public void onDisable() {
        //shopRepo.saveShops();
    }

    @Nullable
    public File getShops() {

        if(shops!=null)
            return shops;

        shops = new File(getDataFolder(),"shops.json");

        if(!shops.exists()) {
            try {
                getDataFolder().mkdir();
                shops.createNewFile();
                JSONObject init = new JSONObject();
                init.put("shops", new JSONArray());
                init.put("pendingShops", new JSONArray());
                init.put("pendingChanges", new JSONArray());
                FileWriter writer = new FileWriter(shops);
                writer.write(init.toJSONString());
                writer.close();

            }
            catch (IOException e) {
                logger.severe(String.format("Unable to initialise shops %s",e.getMessage()));
                e.printStackTrace();
            }
        }
        return shops;
    }

    public ShopRepo getShopRepo() {
        return shopRepo;
    }

    public Metrics getMetrics(){
        return metrics;
    }

    public Config getCustomConfig() {
        return config;
    }

    public DynmapMarkerHandler getDynmapMarkerHandler() {
        return dynmapMarkerHandler;
    }

    public ProcessHandler getProcessHandler() {
        return processHandler;
    }

    public Server getguimdServer() {
        return server;
    }

    @Override
    public BlockData getBlockData(String string) {
        return this.getServer().createBlockData(string);
    }

    @Override
    public PlayerProfile createPlayerProfile(UUID uniqueId, String name) {
        return this.getServer().createPlayerProfile(uniqueId, name);
    }
}
