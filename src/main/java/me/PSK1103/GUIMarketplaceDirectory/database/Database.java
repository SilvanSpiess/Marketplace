package me.PSK1103.GUIMarketplaceDirectory.database;

import me.PSK1103.GUIMarketplaceDirectory.database.entities.Item;
import me.PSK1103.GUIMarketplaceDirectory.database.entities.Owner;
import me.PSK1103.GUIMarketplaceDirectory.database.entities.Shop;
import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import java.util.Properties;

public class Database {

    private GUIMarketplaceDirectory plugin;
    private SessionFactory sessionFactory;

    public Database(GUIMarketplaceDirectory plugin) {
        this.plugin = plugin;

    }

    public SessionFactory getSessionFactory() {
        if (sessionFactory == null || sessionFactory.isClosed()) {
            Properties properties = new Properties();
            properties.put(Environment.DRIVER, DBConfig.DB.DRIVER);
            properties.put(Environment.URL, DBConfig.DB.URL);
            properties.put(Environment.USER, DBConfig.DB.USER);
            properties.put(Environment.PASS, DBConfig.DB.PASS);
            properties.put(Environment.DIALECT, DBConfig.DB.DIALECT);
            properties.put(Environment.HBM2DDL_AUTO, "update");
            properties.put(Environment.SHOW_SQL, "true");
            properties.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
            properties.put(Environment.POOL_SIZE, "1");
            properties.put(Environment.PHYSICAL_NAMING_STRATEGY, "me.PSK1103.GUIMarketplaceDirectory.database.PrefixPhysicalNamingStrategy");
            properties.put(Environment.SCANNER_DISCOVERY, "class");
            Configuration configuration =  new Configuration();
            configuration.setProperties(properties);
            configuration.addAnnotatedClass(Shop.class);
            configuration.addAnnotatedClass(Owner.class);
            configuration.addAnnotatedClass(Item.class);
            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties()).build();

            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        }
        return sessionFactory;
    }



}
