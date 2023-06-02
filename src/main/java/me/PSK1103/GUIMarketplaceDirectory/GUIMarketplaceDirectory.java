package me.PSK1103.GUIMarketplaceDirectory;

import me.PSK1103.GUIMarketplaceDirectory.database.SQLDatabase;
import me.PSK1103.GUIMarketplaceDirectory.eventhandlers.ItemEvents;
import me.PSK1103.GUIMarketplaceDirectory.eventhandlers.ShopEvents;
import me.PSK1103.GUIMarketplaceDirectory.guimd.GUIMarketplaceCommands;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.ShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.mysql.MySQLShopRepo;
import me.PSK1103.GUIMarketplaceDirectory.utils.Config;
import me.PSK1103.GUIMarketplaceDirectory.utils.GUI;
import me.PSK1103.GUIMarketplaceDirectory.utils.Metrics;
import me.PSK1103.GUIMarketplaceDirectory.shoprepos.json.JSONShopRepo;

import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

public class GUIMarketplaceDirectory extends JavaPlugin {

    File shops = null;
    private ShopRepo shopRepo;
    private Config config;
    public GUI gui;
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
        SQLDatabase.initiateConnection(this);
        if(config.usingDB()) {
            shopRepo = new MySQLShopRepo(this);
        }
        else
            shopRepo = new JSONShopRepo(this);
        this.gui = new GUI(this);

        getServer().getPluginManager().registerEvents(new ShopEvents(this),this);
        getServer().getPluginManager().registerEvents(new ItemEvents(this),this);
        getCommand("GUIMD").setExecutor(new GUIMarketplaceCommands(this));
    }

    @Override
    public void onDisable() {
        shopRepo.saveShops();
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
                init.put("shops",new JSONArray());
                init.put("pendingShops",new JSONArray());
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

    public Server getguimdServer() {
        return server;
    }

}
