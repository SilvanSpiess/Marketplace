package me.PSK1103.GUIMarketplaceDirectory.database;

import me.PSK1103.GUIMarketplaceDirectory.GUIMarketplaceDirectory;
import org.json.simple.JSONObject;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class SQLDatabase {

    public static final int SHOP_ADD = 0;
    public static final int OWNER_ADD = 1;
    public static final int BRIDGE_ADD = 2;
    public static final int ITEM_ADD = 3;
    public static final int SHOP_OPERATION = 4;
    public static final int GET_SHOP_OWNER = 5;
    public static final int GET_SHOP_NAME = 6;
    public static final int GET_SHOP_OPERATION_BY_OWNER = 7;
    public static final int GET_SHOP_OPERATION_BY_SHOP = 8;
    public static final int GET_MATCHING_ITEMS = 9;
    public static final int FIND_ITEM = 10;
    public static final int FIND_PLAYER = 11;
    public static final int FIND_SHOP = 12;
    public static final int GET_DIRECTORY = 13;
    public static final int APPROVE_SHOP = 14;
    public static final int REMOVE_SHOP = 15;
    public static final int START_REMOVE_SHOP = 16;
    public static final int GET_ID_FROM_UUID = 17;
    public static final int GET_INV = 18;
    public static final int START_ADD_ITEM = 19;
    public static final int CHANGE_ADD_ITEM_STATUS = 20;
    public static final int SET_QTY = 21;
    public static final int SET_PRICE = 22;
    public static final int FINISH_ITEM_TRANSACTION = 23;
    public static final int CANCEL_ITEM_ADDITION = 24;
    public static final int INIT_OWNER = 25;
    public static final int SET_MAIN_OWNER = 26;
    public static final int FINISH_SHOP_TRANSACTION = 27;
    public static final int SET_DISPLAY_ITEM = 28;
    public static final int IS_OWNER = 29;
    public static final int REMOVE_MATCHING_ITEMS = 30;
    public static final int REMOVE_ITEM = 31;
    public static final int GET_ITEM = 32;
    public static final int FIND_ALTERNATIVES = 33;
    public static final int GET_SHOP_COUNT = 34;
    public static final int GET_ITEM_COUNT = 35;
    public static final int SET_LOOKUP_RADIUS = 36;
    public static final int GET_LOOKUP_RADIUS = 37;
    public static final int REMOVE_TEMPORARY_OWNER = 38;
    public static final int GET_SHOP_LOC = 39;
    public static final int GET_SHOP_OWNERS = 40;
    public static final int GET_ALL_SHOP_IDS = 41;

    static class DBParams {
        public static String  HOST, PORT, DATABASE, USERNAME, PASSWORD, PREFIX, DB;
    }
    private static boolean moderation = false;
    private static boolean useCoreProtect = false;
    public static int defaultLookupRadius = 20;
    private static Logger logger;
    private static Connection connection = null;

    enum TableNames {
        OWNERS, SHOPS, ITEMS, OWNER_SHOP_BRIDGE, SHOP_OPERATIONS, ITEM_OPERATIONS, COREPROTECT_LOOKUP_RADIUS
    }

    static class ShopOperationType {
        static int ADD_OWNER = 1, CONFIRM_SET_OWNER = 2, SET_DISPLAY_ITEM = 3, SHOP_DELETION = 4, SET_OWNER_NAME = 5, SHOP_REMOVAL = 6, SET_LOOKUP_RADIUS = 7;
    }

    static class ItemOperationType {
        static int PRICE_ADD = 1, QTY_ADD = 2;
    }

    static Map<TableNames,String> TableMappings;

    public static Connection getConnection() {
        if (!isConnected()) {
            try {
                connection = DriverManager.getConnection("jdbc:mysql://" + DBParams.HOST + ":" + DBParams.PORT + "/" + DBParams.DATABASE, DBParams.USERNAME, DBParams.PASSWORD);
            }
            catch (SQLException e) {
                logger.severe(e.getMessage());
            }
        }
        return connection;
    }

    private static void createDBTables() {
        Connection connection = getConnection();
        if (connection != null) {
            try {
                Statement statement = connection.createStatement();
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + DBParams.PREFIX + "owners(id INTEGER NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), uuid char (36), name varchar(16), INDEX(name), CONSTRAINT " + DBParams.PREFIX + "shops_ibuniq_1 UNIQUE (uuid))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + DBParams.PREFIX + "shops(id INTEGER NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), owner_id INTEGER NULL, name varchar(32) NOT NULL, description varchar(64) NOT NULL, loc VARCHAR(16) NOT NULL, displayItem varchar(32) NOT NULL DEFAULT \"WRITTEN_BOOK\", status TINYINT NOT NULL, INDEX(name), FOREIGN KEY(owner_id) REFERENCES " + DBParams.PREFIX + "owners (id) ON DELETE SET NULL, CONSTRAINT " + DBParams.PREFIX + "shops_ibuniq_1 UNIQUE (owner_id, name))" );
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + DBParams.PREFIX + "items(id INTEGER NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), shop_id INTEGER NOT NULL, FOREIGN KEY(shop_id) REFERENCES " + DBParams.PREFIX + "shops (id) ON DELETE CASCADE, price INTEGER NOT NULL, qty varchar(11) NOT NULL, name varchar(32) NOT NULL, customName varchar(36) NULL, customType varchar (16) NULL, extraInfo JSON NULL, status TINYINT NOT NULL, INDEX(name), INDEX(customName), INDEX(customType))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + DBParams.PREFIX + "owner_shop_bridge(id INTEGER NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), owner_id INTEGER NULL, FOREIGN KEY(owner_id) REFERENCES " + DBParams.PREFIX + "owners (id) ON DELETE SET NULL , shop_id INTEGER NOT NULL, FOREIGN KEY(shop_id) REFERENCES " + DBParams.PREFIX + "shops (id) ON DELETE CASCADE, CONSTRAINT " + DBParams.PREFIX + "owner_shop_bridge_ibuniq_1 UNIQUE (owner_id, shop_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + DBParams.PREFIX + "shop_operations(owner_id INTEGER NOT NULL, FOREIGN KEY(owner_id) REFERENCES " + DBParams.PREFIX + "owners (id), shop_id INTEGER NOT NULL, FOREIGN KEY(shop_id) REFERENCES " + DBParams.PREFIX + "shops (id) ON DELETE CASCADE, operation TINYINT NOT NULL, CONSTRAINT " + DBParams.PREFIX + "shop_operations_ibuniq_1 UNIQUE (owner_id, shop_id))");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + DBParams.PREFIX + "item_operations(owner_id INTEGER NOT NULL, FOREIGN KEY(owner_id) REFERENCES " + DBParams.PREFIX + "owners (id), shop_id INTEGER NOT NULL, FOREIGN KEY(shop_id) REFERENCES " + DBParams.PREFIX + "shops (id) ON DELETE CASCADE, item_id INTEGER NOT NULL, FOREIGN KEY (item_id) REFERENCES " + DBParams.PREFIX + "items (id) ON DELETE CASCADE, operation TINYINT NOT NULL, CONSTRAINT " + DBParams.PREFIX + "item_operations_ibuniq_1 UNIQUE (owner_id, shop_id, item_id))");

                TableMappings = new HashMap<>();
                TableMappings.put(TableNames.OWNERS, DBParams.PREFIX + "owners");
                TableMappings.put(TableNames.SHOPS, DBParams.PREFIX + "shops");
                TableMappings.put(TableNames.ITEMS, DBParams.PREFIX + "items");
                TableMappings.put(TableNames.OWNER_SHOP_BRIDGE, DBParams.PREFIX + "owner_shop_bridge");
                TableMappings.put(TableNames.SHOP_OPERATIONS, DBParams.PREFIX + "shop_operations");
                TableMappings.put(TableNames.ITEM_OPERATIONS, DBParams.PREFIX + "item_operations");

                if(useCoreProtect) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + DBParams.PREFIX + "lookup_radius(shop_id INTEGER PRIMARY KEY, FOREIGN KEY(shop_id) REFERENCES " + DBParams.PREFIX + "shops(id) ON DELETE CASCADE, radius INTEGER NOT NULL, CHECK(radius>0)) ");
                    TableMappings.put(TableNames.COREPROTECT_LOOKUP_RADIUS, DBParams.PREFIX + "lookup_radius");
                }
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
    }

    public static boolean isConnected() {
        return (connection != null);
    }

    public static void initiateConnection(GUIMarketplaceDirectory plugin) {
        Map<String,String> dbParams = plugin.getCustomConfig().getMySQLDetails();
        DBParams.HOST = dbParams.get("mysql-host");
        DBParams.PORT = dbParams.get("mysql-port");
        DBParams.DATABASE = dbParams.get("mysql-database");
        DBParams.USERNAME = dbParams.get("mysql-username");
        DBParams.PASSWORD = dbParams.get("mysql-password");
        DBParams.PREFIX = dbParams.get("table-prefix");
        DBParams.DB = dbParams.get("db");
        moderation = plugin.getCustomConfig().directoryModerationEnabled();
        useCoreProtect = plugin.getCustomConfig().useCoreProtect();
        defaultLookupRadius = plugin.getCustomConfig().getDefaultLookupRadius();
        logger = plugin.getLogger();
        createDBTables();
    }

    public static boolean isAddingOwner(int operation) {
        return !isRejectingShop(operation) && !isRemovingShop(operation);
    }

    public static boolean isRejectingShop(int operation) {
        return operation == ShopOperationType.SHOP_REMOVAL;
    }
    public static boolean isRemovingShop(int operation) {
        return operation == ShopOperationType.SHOP_DELETION;
    }

    public static String createShopAsOwner(String name, String desc, String owner, String uuid, String loc, String displayItem) {
        try {
            int ownerId = addPlayer(uuid, owner);
            if (ownerId < 0)
                return null;
            int status = moderation ? 0 : 1;
            PreparedStatement stmt = populateStatement(prepareStatement(SHOP_ADD), ownerId, name, desc, loc, displayItem, status);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            keys.next();
            int shopId = keys.getInt(1);
            keys.close();
            stmt.close();
            addToPlayerShopBridge(ownerId, shopId);
            if(useCoreProtect)
                setLookupRadius(shopId, defaultLookupRadius);
            return Integer.toString(shopId);
        }
        catch (SQLException ignored) {}
        return null;
    }

    public static String createShop(String name, String desc, String owner, String uuid, String loc, String displayItem) {
        try {
            int ownerId = addPlayer(uuid, owner);
            if (ownerId < 0)
                return null;

            int status = moderation ? 0 : 1;
            PreparedStatement stmt = populateStatement(prepareStatement(SHOP_ADD), ownerId, name, desc, loc, displayItem, status);
            stmt.executeUpdate();
            ResultSet keys = stmt.getGeneratedKeys();
            keys.next();
            int shopId = keys.getInt(1);
            keys.close();
            stmt.close();
            addToPlayerShopBridge(ownerId, shopId);
            if(useCoreProtect)
                setLookupRadius(shopId, defaultLookupRadius);
            addToShopOperations(ownerId, shopId, ShopOperationType.CONFIRM_SET_OWNER);
            return Integer.toString(shopId);
        }
        catch (SQLException e) {
            logger.severe("Failed to create shop, " + e.getMessage());
        }
        return null;
    }

    public static void addOwner(String uuid, String newUuid, String newName) {
        try {
            PreparedStatement st1 = populateStatement(prepareStatement(GET_SHOP_OPERATION_BY_OWNER), uuid);
            ResultSet rs1 = st1.executeQuery();
            rs1.next();
            int shopId = rs1.getInt(1), operation = rs1.getInt(2);
            rs1.close();
            st1.close();
            int ownerId = addPlayer(newUuid, newName);
            if (operation == ShopOperationType.SET_OWNER_NAME) {
                PreparedStatement st2 = populateStatement(prepareStatement(SET_MAIN_OWNER), ownerId, shopId);
                st2.executeUpdate();
                st2.close();
                PreparedStatement st3 = populateStatement(prepareStatement(REMOVE_TEMPORARY_OWNER), uuid, shopId);
                st3.executeUpdate();
                st3.close();
            }
            addToPlayerShopBridge(ownerId, shopId);
            finishShopTransaction(uuid);
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't add new owner, uuid: %s, new uuid: %s, error: %s", uuid, newUuid, e.getMessage()));
        }
    }

    public static void startAddingOwner(String uuid, int shopId) {
        try {
            PreparedStatement st1 = populateStatement(prepareStatement(GET_ID_FROM_UUID), uuid);
            ResultSet rs1 = st1.executeQuery();
            rs1.next();
            int ownerId = rs1.getInt(1);
            rs1.close();
            st1.close();
            addToShopOperations(ownerId, shopId, ShopOperationType.ADD_OWNER);
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't start adding another owner, uuid: %s, shop: %d, error: %s", uuid, shopId, e.getMessage()));
        }
    }

    public static void startSettingDisplayItem(String uuid, int shopId) {
        try {
            PreparedStatement st1 = populateStatement(prepareStatement(GET_ID_FROM_UUID), uuid);
            ResultSet rs1 = st1.executeQuery();
            rs1.next();
            int ownerId = rs1.getInt(1);
            rs1.close();
            st1.close();
            addToShopOperations(ownerId, shopId, ShopOperationType.SET_DISPLAY_ITEM);
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't start setting display item, uuid: %s, shop: %d, error: %s", uuid, shopId, e.getMessage()));
        }
    }

    public static void startSettingLookupRadius(String uuid, int shopId) {
        try {
            PreparedStatement st1 = populateStatement(prepareStatement(GET_ID_FROM_UUID), uuid);
            ResultSet rs1 = st1.executeQuery();
            rs1.next();
            int ownerId = rs1.getInt(1);
            rs1.close();
            st1.close();
            addToShopOperations(ownerId, shopId, ShopOperationType.SET_LOOKUP_RADIUS);
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't start setting lookup radius, uuid: %s, shop: %d, error: %s", uuid, shopId, e.getMessage()));
        }
    }

    public static void setDisplayItem(String uuid, String displayItem) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(SET_DISPLAY_ITEM), displayItem, uuid);
            stmt.executeUpdate();
            stmt.close();
            finishShopTransaction(uuid);
        }
        catch (SQLException e) {
            logger.severe(String.format("Failed to set display item, uuid: %s, display item: %s. error: %s", uuid, displayItem, e.getMessage()));
        }
    }

    public static void finishShopTransaction(String uuid) {
        try {
            PreparedStatement st3 = populateStatement(prepareStatement(FINISH_SHOP_TRANSACTION), uuid);
            st3.executeUpdate();
            st3.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Cannot finish shop transaction, uuid: %s, error: %s", uuid, e.getMessage()));
        }
    }

    public static void initShopOwnerAddition(String uuid) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(INIT_OWNER), ShopOperationType.SET_OWNER_NAME, uuid);
            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Cannot initiate setting owner, uuid: %s, error: %s", uuid, e.getMessage()));
        }
    }

    public static int addPlayer(String uuid, String name) {
        connection = getConnection();
        int key = -1;
        try {
            PreparedStatement query = populateStatement(prepareStatement(GET_ID_FROM_UUID), uuid);
            ResultSet users = query.executeQuery();
            if (users.next())
                key = users.getInt(1);
            else {
                PreparedStatement addUserStatement = populateStatement(prepareStatement(OWNER_ADD), uuid, name, name);
                addUserStatement.executeUpdate();
                ResultSet keys = addUserStatement.getGeneratedKeys();
                keys.next();
                key = keys.getInt(1);
                keys.close();
                addUserStatement.close();
            }
            users.close();
            query.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("User cannot be added, uuid: %s, name: %s, error: %s", uuid, name, e.getMessage()));
        }
        return key;
    }

    public static void startItemAddition(int shopId,String uuid, String name, String customName, String customType, String qty, int price, JSONObject extraInfo) {
        try {
            PreparedStatement s1 = populateStatement(prepareStatement(ITEM_ADD), shopId, name, customName.equals("") ? null : customName, "".equals(customType) ? null : customType, price, qty, (customType == null || customType.equals("")) ? null : extraInfo.toJSONString(), 0);
            s1.executeUpdate();
            ResultSet keys = s1.getGeneratedKeys();
            keys.next();
            int itemId = keys.getInt(1);
            keys.close();
            s1.close();
            PreparedStatement s2 = populateStatement(prepareStatement(GET_ID_FROM_UUID), uuid);
            ResultSet r2 = s2.executeQuery();
            r2.next();
            int ownerId = r2.getInt(1);
            r2.close();
            s2.close();
            PreparedStatement s3 = populateStatement(prepareStatement(START_ADD_ITEM), ownerId, shopId, itemId, ItemOperationType.QTY_ADD);
            s3.executeUpdate();
            s3.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Failed to add item, shopId: %s, name: %s", shopId, name));
        }
    }

    public static void setQty(String qty, String uuid) {
        try {
            PreparedStatement s1 = populateStatement(prepareStatement(SET_QTY), qty, uuid);
            s1.executeUpdate();
            PreparedStatement s2 = populateStatement(prepareStatement(CHANGE_ADD_ITEM_STATUS), ItemOperationType.PRICE_ADD, uuid);
            s2.executeUpdate();
            s1.close();
            s2.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't add qty, uuid: %s, error: %s", uuid, e.getMessage()));
        }
    }

    public static void setPrice(int price, String uuid) {
        try {
            PreparedStatement s1 = populateStatement(prepareStatement(SET_PRICE), price, uuid);
            s1.executeUpdate();
            PreparedStatement s2 = populateStatement(prepareStatement(FINISH_ITEM_TRANSACTION), uuid);
            s2.executeUpdate();
            s1.close();
            s2.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't add price, uuid: %s, error: %s", uuid, e.getMessage()));
        }
    }

    public static void addToPlayerShopBridge(int ownerId, int shopId) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(BRIDGE_ADD), ownerId, shopId);
            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't add to bridge, owner: %d, shop = %d", ownerId, shopId));
        }

    }

    public static void addToShopOperations(int ownerId, int shopId, int operation) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(SHOP_OPERATION), ownerId, shopId, operation);
            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't initiate shop operation, owner: %d, shop: %d. operation: %d", ownerId, shopId, operation));
        }
    }

    public static String getOwner(int shopId) {
        String owner = "";
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_SHOP_OWNER), shopId);
            ResultSet res = stmt.executeQuery();
            if(res.next()) {
                owner = res.getString(1);
            }
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't get owner of shop: %d", shopId));
        }
        return owner;
    }

    public static List<Map<String,Object>> getInv(int key) {
        List<Map<String,Object>> inv = new ArrayList<>();
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_INV), key);
            ResultSet res = stmt.executeQuery();
            ResultSetMetaData meta = res.getMetaData();
            int cols = meta.getColumnCount();
            while(res.next()) {
                Map<String,Object> item = new HashMap<>();
                for(int i = 1;i <= cols;i++)
                    item.put(meta.getColumnLabel(i), res.getObject(i));
                inv.add(item);
            }
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Failed to get inventory, shop: %d, error: %s", key, e.getMessage()));
        }
        return inv;
    }

    public static boolean isOwner(String uuid, int key) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(IS_OWNER), uuid, key);
            ResultSet res = stmt.executeQuery();
            boolean isOwner= res.next();
            res.close();
            stmt.close();
            return isOwner;
        }
        catch (SQLException e) {
            logger.severe(String.format("Cannot validate ownership, uuid: %s, shop: %d", uuid, key));
            return false;
        }
    }

    public static List<Map<String,String>> getShopDetails(int status) {
        List<Map<String,String>> shops = new ArrayList<>();
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_DIRECTORY), status);
            ResultSet res = stmt.executeQuery();
            ResultSetMetaData meta = res.getMetaData();
            int cols = meta.getColumnCount();
            while(res.next()) {
                Map<String,String> sr = new HashMap<>();
                for (int i = 1;i <= cols; i++) {
                    sr.put(meta.getColumnLabel(i), res.getString(i));
                }
                shops.add(sr);
            }
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Cannot get directory, error: %s", e.getMessage()));
        }
        return shops;
    }

    public static void approveShop(int key) {
        try {
           PreparedStatement stmt = populateStatement(prepareStatement(APPROVE_SHOP), key);
           stmt.executeUpdate();
           stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Cannot approve shop id: %d, error: %s", key, e.getMessage()));
        }
    }

    public static void cancelItemAddition(String uuid) {
        try {
            PreparedStatement st2 = populateStatement(prepareStatement(CANCEL_ITEM_ADDITION), uuid);
            st2.executeUpdate();
            st2.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Failed to cancel item addition, uuid: %s, error: %s", uuid, e.getMessage()));
        }
    }

    public static void startRejectingShop(String uuid, int key, boolean moderator) {
        try {
            PreparedStatement s1 = populateStatement(prepareStatement(GET_ID_FROM_UUID), uuid);
            ResultSet r1 = s1.executeQuery();
            r1.next();
            int ownerId = r1.getInt(1);
            r1.close();
            s1.close();
            PreparedStatement s2 = populateStatement(prepareStatement(START_REMOVE_SHOP), ownerId, key, moderator ? ShopOperationType.SHOP_REMOVAL : ShopOperationType.SHOP_DELETION);
            s2.executeUpdate();
            s2.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Cannot initiate shop rejection, uuid: %s, key: %d, error: %s", uuid, key, e.getMessage()));
        }
    }

    public static void removeShop(String uuid) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(REMOVE_SHOP), uuid, ShopOperationType.SHOP_DELETION, ShopOperationType.SHOP_REMOVAL);
            stmt.executeUpdate();
            stmt.close();
            finishShopTransaction(uuid);
        }
        catch (SQLException e) {
            logger.severe(String.format("Cannot remove shop id: %s, error: %s", uuid, e.getMessage()));
        }
    }

    public static boolean getIsInitOwner(String uuid) {
        boolean isInitOwner = false;
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_SHOP_OPERATION_BY_OWNER), uuid);
            ResultSet res = stmt.executeQuery();
            if(res.next()) {
                int operation = res.getInt(2);
                if (operation == ShopOperationType.CONFIRM_SET_OWNER || operation == ShopOperationType.SET_OWNER_NAME)
                    isInitOwner = true;
            }
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't get shop owner init status for owner %s", uuid));
        }
        return isInitOwner;
    }

    public static int getEditType(String uuid) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_SHOP_OPERATION_BY_OWNER), uuid);
            ResultSet res = stmt.executeQuery();
            res.next();
            int editType = res.getInt(2);
            res.close();
            stmt.close();
            return editType;
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't get edit type, uuid: %s", uuid));
            return 0;
        }
    }

    public static String getShopName(int key) {
        String name = "";
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_SHOP_NAME), key);
            ResultSet res = stmt.executeQuery();
            res.next();
            name = res.getString(1);
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't get shop name, shop: %d, error: %s", key, e.getMessage()));
        }
        return name;
    }

    public static int getEditType(int key) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_SHOP_OPERATION_BY_SHOP), key);
            ResultSet res = stmt.executeQuery();
            res.next();
            int editType = res.getInt(2);
            res.close();
            stmt.close();
            return editType;
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't get edit type, key: %s", key));
            return 0;
        }
    }

    public static List<Map<String,String>> filterShopsByPlayer(String searchKey) {
        List<Map<String,String>> searchResults = new ArrayList<>();
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(FIND_PLAYER), "%" + searchKey + "%");
            ResultSet res = stmt.executeQuery();
            ResultSetMetaData meta = res.getMetaData();
            int cols = meta.getColumnCount();
            while(res.next()) {
                Map<String,String> sr = new HashMap<>();
                for (int i = 1;i <= cols; i++)
                    sr.put(meta.getColumnLabel(i), res.getString(i));
                searchResults.add(sr);
            }
            res.close();
            stmt.close();
        } catch (SQLException e) {
            logger.severe(String.format("Cannot find shops for player search key: %s", searchKey));
        }

        return searchResults;
    }

    public static List<Map<String,String>> filterShopsByName(String searchKey) {
        List<Map<String,String>> searchResults = new ArrayList<>();
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(FIND_SHOP), "%" + searchKey + "%");
            ResultSet res = stmt.executeQuery();
            ResultSetMetaData meta = res.getMetaData();
            int cols = meta.getColumnCount();
            while(res.next()) {
                Map<String,String> sr = new HashMap<>();
                for (int i = 1;i <= cols; i++)
                    sr.put(meta.getColumnLabel(i), res.getString(i));
                searchResults.add(sr);
            }
            res.close();
            stmt.close();
        } catch (SQLException e) {
            logger.severe(String.format("Cannot find shops for shop search key: %s", searchKey));
        }

        return searchResults;
    }

    public static List<Map<String, Object>> getMatchingItems(int key, String itemName) {
        List<Map<String, Object>> items = new ArrayList<>();
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_MATCHING_ITEMS), key, itemName);
            ResultSet res = stmt.executeQuery();
            ResultSetMetaData meta = res.getMetaData();
            int cols = meta.getColumnCount();
            while(res.next()) {
                Map<String,Object> item = new HashMap<>();
                for(int i = 1;i <= cols;i++)
                    item.put(meta.getColumnLabel(i), res.getObject(i));
                items.add(item);
            }
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't find matching items, shop %d, item name: %s, error: %s", key, itemName, e.getMessage()));
        }

        return items;
    }
    public static Map<String, Object> getItem(int id) {
        Map<String, Object> data = new HashMap<>();
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_ITEM), id);
            ResultSet res = stmt.executeQuery();
            ResultSetMetaData meta = res.getMetaData();
            int cols = meta.getColumnCount();
            res.next();
            for(int i = 1;i <= cols;i++) {
                data.put(meta.getColumnLabel(i), res.getObject(i));
            }
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Cannot get item: %d", id));
        }
        return data;
    }

    public static List<Map<String,Object>> getAlternatives(String itemName) {
        List<Map<String, Object>> items = new ArrayList<>();
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(FIND_ALTERNATIVES), itemName);
            ResultSet res = stmt.executeQuery();
            ResultSetMetaData meta = res.getMetaData();
            int cols = meta.getColumnCount();
            while(res.next()) {
                Map<String,Object> item = new HashMap<>();
                for(int i = 1;i <= cols;i++)
                    item.put(meta.getColumnLabel(i), res.getObject(i));
                items.add(item);
            }
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't find alternatives for item: %s", itemName));
        }
        return items;
    }

    public static void removeMatchingItems(int key, String itemName) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(REMOVE_MATCHING_ITEMS), key, itemName);
            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't remove matching items, shop: %d, item name: %s, error: %s", key, itemName, e.getMessage()));
        }
    }

    public static void removeItem(int id) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(REMOVE_ITEM), id);
            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Failed to remove item with id: %d", id));
        }
    }

    public static List<Map<String, Object>> findItem(String key) {
        String itemName = key.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        List<Map<String, Object>> items = new ArrayList<>();
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(FIND_ITEM), "%" + itemName + "%", "%" + key + "%");
            ResultSet res = stmt.executeQuery();
            ResultSetMetaData meta = res.getMetaData();
            int cols = meta.getColumnCount();
            while(res.next()) {
                Map<String,Object> item = new HashMap<>();
                for(int i = 1;i <= cols;i++)
                    item.put(meta.getColumnLabel(i), res.getObject(i));
                items.add(item);
            }
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Couldn't find item for key %s", key));
        }

        return items;
    }

    public static int getShopCount() {
        try {
            PreparedStatement stmt = prepareStatement(GET_SHOP_COUNT);
            ResultSet res = stmt.executeQuery();
            res.next();
            int count = res.getInt(1);
            res.close();
            stmt.close();
            return count;
        }
        catch (SQLException e) {
            return 0;
        }
    }

    public static int getItemCount() {
        try {
            PreparedStatement stmt = prepareStatement(GET_ITEM_COUNT);
            ResultSet res = stmt.executeQuery();
            res.next();
            int count = res.getInt(1);
            res.close();
            stmt.close();
            return count;
        }
        catch (SQLException e) {
            return 0;
        }
    }

    public static int insertShop(int ownerId, String name, String desc, String loc, String displayItem, int status) {
        int key = -1;
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(SHOP_ADD), ownerId, name, desc, loc, displayItem, status);
            stmt.executeUpdate();
            ResultSet res = stmt.getGeneratedKeys();
            res.next();
            key = res.getInt(1);
            res.close();
            stmt.close();
        }
        catch (SQLException ignored){}
        return key;
    }

    public static void insertItem(int shopId, String name, String customName, String customType, String qty, int price, JSONObject extraInfo) {
        try {
            PreparedStatement s1 = populateStatement(prepareStatement(ITEM_ADD), shopId, name, customName.equals("") ? null : customName, "".equals(customType) ? null : customType, price, qty, (customType == null || customType.equals("")) ? null : extraInfo.toJSONString(), 1);
            s1.executeUpdate();
            s1.close();
        }
        catch (SQLException e) {
            logger.severe("cannot migrate item, " + e.getMessage());
        }
    }

    public static void setLookupRadius(int shopId, int radius) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(SET_LOOKUP_RADIUS), shopId, radius, radius);
            stmt.executeUpdate();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Failed to set lookup radius for shop: %d, radius: %d, error: %s", shopId, radius, e.getMessage()));
        }
    }

    public static int getLookupRadius(int shopId) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_LOOKUP_RADIUS), shopId);
            ResultSet res = stmt.executeQuery();
            res.next();
            int radius = res.getInt(1);
            res.close();
            stmt.close();
            return radius;
        }
        catch (SQLException e) {
            logger.severe(String.format("Failed to get lookup radius for shop %d, error: %s", shopId, e.getMessage()));
            return defaultLookupRadius;
        }
    }

    public static String getShopLoc(int key) {
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_SHOP_LOC), key);
            ResultSet res = stmt.executeQuery();
            res.next();
            String loc = res.getString(1);
            res.close();
            stmt.close();
            return loc;
        }
        catch (SQLException e) {
            logger.severe(String.format("Failed to get shop location, shop: %d, error: %s", key, e.getMessage()));
            return "0,0";
        }
    }

    public static List<String> getShopOwners(int key) {
        List<String> owners = new ArrayList<>();
        try {
            PreparedStatement stmt = populateStatement(prepareStatement(GET_SHOP_OWNERS), key);
            ResultSet res = stmt.executeQuery();
            while(res.next())
                owners.add(res.getString(1));
            res.close();
            stmt.close();
        }
        catch (SQLException e) {
            logger.severe(String.format("Cannot get owners of shop: %d, error:%s", key, e.getMessage()));
        }
        return owners;
    }

    public static Map<Integer, String> getAllShopIds() {
        Map<Integer, String> shops = new HashMap<>();
        try {
            PreparedStatement stmt = prepareStatement(GET_ALL_SHOP_IDS);
            ResultSet res = stmt.executeQuery();
            while(res.next())
                shops.put(res.getInt(1), res.getString(2));
            res.close();
            stmt.close();
        }
        catch (SQLException ignored){}
        return shops;
    }

    private static PreparedStatement prepareStatement(int type) throws SQLException{
        connection = getConnection();

        return switch (type) {
            case SHOP_ADD -> connection.prepareStatement("INSERT INTO " + TableMappings.get(TableNames.SHOPS) + " (owner_id, name, description, loc, displayItem, status) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            case OWNER_ADD -> connection.prepareStatement("INSERT INTO " + TableMappings.get(TableNames.OWNERS) + " (uuid, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = ?", Statement.RETURN_GENERATED_KEYS);
            case BRIDGE_ADD -> connection.prepareStatement("INSERT INTO " + TableMappings.get(TableNames.OWNER_SHOP_BRIDGE) + " (owner_id, shop_id) VALUES (?, ?)");
            case SHOP_OPERATION, START_REMOVE_SHOP -> connection.prepareStatement("INSERT INTO " + TableMappings.get(TableNames.SHOP_OPERATIONS) + " (owner_id, shop_id, operation) VALUES (?, ?, ?)");
            case GET_SHOP_OWNER -> connection.prepareStatement("SELECT o.name FROM " + TableMappings.get(TableNames.OWNERS) + " o INNER JOIN " + TableMappings.get(TableNames.SHOPS) + " s ON o.id = s.owner_id WHERE s.id = ?");
            case GET_SHOP_NAME -> connection.prepareStatement("SELECT name FROM " + TableMappings.get(TableNames.SHOPS) + " WHERE id = ?");
            case GET_SHOP_OPERATION_BY_OWNER -> connection.prepareStatement("SELECT so.shop_id, so.operation FROM " + TableMappings.get(TableNames.SHOP_OPERATIONS) + " so INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON o.id = so.owner_id WHERE o.uuid = ?");
            case GET_SHOP_OPERATION_BY_SHOP -> connection.prepareStatement("SELECT owner_id, operation FROM " + TableMappings.get(TableNames.SHOP_OPERATIONS) + "WHERE shop_id = ?");
            case GET_MATCHING_ITEMS -> connection.prepareStatement("SELECT * FROM " + TableMappings.get(TableNames.ITEMS) + " WHERE shop_id = ? AND TRIM(LOWER(name)) = TRIM(LOWER(?)) AND status = 1");
            case FIND_ITEM -> connection.prepareStatement("SELECT i.*, s.name AS \"shop_name\", s.loc AS \"shop_loc\" FROM " + TableMappings.get(TableNames.ITEMS) + " i INNER JOIN " + TableMappings.get(TableNames.SHOPS) + " s ON i.shop_id = s.id WHERE (TRIM(LOWER(i.name)) LIKE ? OR TRIM(LOWER(i.customName)) LIKE ?) AND s.status = 1 AND i.status = 1 ORDER BY i.price ASC");
            case FIND_PLAYER -> connection.prepareStatement("SELECT s.name, s.description AS \"desc\", s.loc, s.id AS \"key\", s.displayItem ,REPLACE(REPLACE(REPLACE(JSON_UNQUOTE(JSON_ARRAYAGG(o.name)), '\"', ''), '[', ''), ']', '') AS \"owners\" FROM " + TableMappings.get(TableNames.SHOPS) + " s INNER JOIN " + TableMappings.get(TableNames.OWNER_SHOP_BRIDGE) + " osb ON s.id = osb.shop_id INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON o.id = osb.owner_id WHERE s.id IN (SELECT osb1.shop_id FROM " + TableMappings.get(TableNames.OWNER_SHOP_BRIDGE) + " osb1 INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o1 ON o1.id = osb1.owner_id AND o1.name LIKE ?) AND s.status = 1 GROUP BY s.id ORDER BY s.name ASC");
            case FIND_SHOP -> connection.prepareStatement("SELECT s.name, s.description AS \"desc\", s.loc, s.id AS \"key\", s.displayItem ,REPLACE(REPLACE(REPLACE(JSON_UNQUOTE(JSON_ARRAYAGG(o.name)), '\"', ''), '[', ''), ']', '') AS \"owners\" FROM " + TableMappings.get(TableNames.SHOPS) + " s INNER JOIN " + TableMappings.get(TableNames.OWNER_SHOP_BRIDGE) + " osb ON s.id = osb.shop_id INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON o.id = osb.owner_id WHERE s.name LIKE ? AND s.status = 1 GROUP BY s.id ORDER BY s.name ASC");
            case GET_DIRECTORY -> connection.prepareStatement("SELECT s.name, s.description AS \"desc\", s.loc, s.id AS \"key\", s.displayItem ,REPLACE(REPLACE(REPLACE(JSON_UNQUOTE(JSON_ARRAYAGG(o.name)), '\"', ''), '[', ''), ']', '') AS \"owners\" FROM " + TableMappings.get(TableNames.SHOPS) + " s INNER JOIN " + TableMappings.get(TableNames.OWNER_SHOP_BRIDGE) + " osb ON s.id = osb.shop_id INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON o.id = osb.owner_id WHERE s.status = ? GROUP BY s.id ORDER BY s.name ASC;");
            case APPROVE_SHOP -> connection.prepareStatement("UPDATE " + TableMappings.get(TableNames.SHOPS) + " SET status = 1 WHERE id = ?");
            case REMOVE_SHOP -> connection.prepareStatement("DELETE FROM " + TableMappings.get(TableNames.SHOPS) + " WHERE id IN (SELECT shop_id FROM " + TableMappings.get(TableNames.SHOP_OPERATIONS) + " so INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON o.id = so.owner_id WHERE o.uuid = ? AND so.operation IN (?, ?))");
            case GET_ID_FROM_UUID -> connection.prepareStatement("SELECT id FROM " + TableMappings.get(TableNames.OWNERS) + " WHERE uuid = ?");
            case GET_INV -> connection.prepareStatement("SELECT * FROM " + TableMappings.get(TableNames.ITEMS) + " WHERE shop_id = ? AND status = 1 ORDER BY name ASC");
            case ITEM_ADD -> connection.prepareStatement("INSERT INTO " + TableMappings.get(TableNames.ITEMS) + " (shop_id, name, customName, customType, price, qty, extraInfo, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            case START_ADD_ITEM -> connection.prepareStatement("INSERT INTO " + TableMappings.get(TableNames.ITEM_OPERATIONS) + " (owner_id, shop_id, item_id, operation) VALUES (?, ?, ?, ?)");
            case SET_QTY -> connection.prepareStatement("UPDATE " + TableMappings.get(TableNames.ITEMS) + " i INNER JOIN " + TableMappings.get(TableNames.ITEM_OPERATIONS) + " io ON i.id = io.item_id INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON io.owner_id = o.id SET i.qty = ? WHERE o.uuid = ?");
            case SET_PRICE -> connection.prepareStatement("UPDATE " + TableMappings.get(TableNames.ITEMS) + " i INNER JOIN " + TableMappings.get(TableNames.ITEM_OPERATIONS) + " io ON i.id = io.item_id INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON io.owner_id = o.id SET i.price = ?, i.status = 1 WHERE o.uuid = ?");
            case CHANGE_ADD_ITEM_STATUS -> connection.prepareStatement("UPDATE " + TableMappings.get(TableNames.ITEM_OPERATIONS) + " io INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON o.id = io.owner_id SET operation = ? WHERE o.uuid = ?");
            case FINISH_ITEM_TRANSACTION -> connection.prepareStatement("DELETE FROM " + TableMappings.get(TableNames.ITEM_OPERATIONS) + " WHERE owner_id IN (SELECT o.id FROM " + TableMappings.get(TableNames.OWNERS) + " o WHERE o.uuid = ?)");
            case CANCEL_ITEM_ADDITION -> connection.prepareStatement("DELETE FROM " + TableMappings.get(TableNames.ITEMS) + " i  WHERE i.id IN (SELECT io.item_id FROM " + TableMappings.get(TableNames.ITEM_OPERATIONS) + " io INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON o.id = io.owner_id WHERE o.uuid = ?)");
            case INIT_OWNER -> connection.prepareStatement("UPDATE " + TableMappings.get(TableNames.SHOP_OPERATIONS) + " so SET so.operation = ? WHERE so.owner_id in (SELECT id FROM " + TableMappings.get(TableNames.OWNERS) + " o WHERE o.uuid = ?)");
            case SET_MAIN_OWNER -> connection.prepareStatement(" UPDATE " + TableMappings.get(TableNames.SHOPS) + " SET owner_id = ? WHERE id = ?");
            case FINISH_SHOP_TRANSACTION -> connection.prepareStatement("DELETE FROM " + TableMappings.get(TableNames.SHOP_OPERATIONS) + " WHERE owner_id IN (SELECT o.id FROM  " + TableMappings.get(TableNames.OWNERS) + " o WHERE o.uuid = ?)");
            case SET_DISPLAY_ITEM -> connection.prepareStatement("UPDATE " + TableMappings.get(TableNames.SHOPS) + " SET displayItem = ? WHERE id IN (SELECT so.shop_id FROM " + TableMappings.get(TableNames.SHOP_OPERATIONS) + " so INNER JOIN " + TableMappings.get(TableNames.OWNERS) + " o ON o.id = so.owner_id WHERE o.uuid = ?)");
            case IS_OWNER -> connection.prepareStatement("SELECT COUNT(*) FROM " + TableMappings.get(TableNames.OWNER_SHOP_BRIDGE) + " WHERE owner_id IN (SELECT id FROM " + TableMappings.get(TableNames.OWNERS) + " WHERE uuid = ?) AND shop_id = ?");
            case REMOVE_MATCHING_ITEMS -> connection.prepareStatement("DELETE FROM " + TableMappings.get(TableNames.ITEMS) + " WHERE shop_id = ? AND name = ?");
            case REMOVE_ITEM -> connection.prepareStatement("DELETE FROM " + TableMappings.get(TableNames.ITEMS) + " WHERE id = ? AND status = 1");
            case GET_ITEM -> connection.prepareStatement("SELECT * FROM " + TableMappings.get(TableNames.ITEMS) + " WHERE id = ? AND status = 1");
            case FIND_ALTERNATIVES -> connection.prepareStatement("SELECT i.*, s.name AS \"shop_name\" FROM " + TableMappings.get(TableNames.ITEMS) + " i INNER JOIN " + TableMappings.get(TableNames.SHOPS) + " s ON s.id = i.shop_id WHERE i.name = ? AND i.status = 1");
            case GET_SHOP_COUNT -> connection.prepareStatement("SELECT COUNT(*) FROM " + TableMappings.get(TableNames.SHOPS));
            case GET_ITEM_COUNT -> connection.prepareStatement("SELECT COUNT(*) FROM " + TableMappings.get(TableNames.ITEMS));
            case SET_LOOKUP_RADIUS -> connection.prepareStatement("INSERT INTO " + TableMappings.get(TableNames.COREPROTECT_LOOKUP_RADIUS) + " (shop_id, radius) VALUES (?, ?) ON DUPLICATE KEY UPDATE radius = ?");
            case GET_LOOKUP_RADIUS -> connection.prepareStatement("SELECT radius FROM " + TableMappings.get(TableNames.COREPROTECT_LOOKUP_RADIUS) + " WHERE shop_id = ?");
            case REMOVE_TEMPORARY_OWNER -> connection.prepareStatement("DELETE FROM " + TableMappings.get(TableNames.OWNER_SHOP_BRIDGE) + " WHERE owner_id IN (SELECT id FROM " + TableMappings.get(TableNames.OWNERS) + " WHERE uuid = ?) AND shop_id = ?");
            case GET_SHOP_LOC -> connection.prepareStatement("SELECT loc FROM " + TableMappings.get(TableNames.SHOPS) + " WHERE id = ?");
            case GET_SHOP_OWNERS -> connection.prepareStatement("SELECT o.name FROM " + TableMappings.get(TableNames.OWNERS) + " o where o.id IN (SELECT owner_id FROM " + TableMappings.get(TableNames.OWNER_SHOP_BRIDGE) + " WHERE shop_id = ?) ORDER BY o.name ASC");
            case GET_ALL_SHOP_IDS -> connection.prepareStatement("SELECT id,name FROM " + TableMappings.get(TableNames.SHOPS));
            default -> null;
        };
    }

    private static PreparedStatement populateStatement(PreparedStatement stmt, Object... args) throws SQLException {
        connection = getConnection();
        int index = 1;
        for(Object i : args)
            stmt.setObject(index++, i);
        return stmt;
    }

    public static void closeConnection() {
        if(isConnected())
            try {
                connection.close();
            }
        catch (SQLException ignored){}
    }

}
