package me.PSK1103.GUIMarketplaceDirectory.database;

public class DBConfig {

    public static String DB_TYPE;
    public static String DB_URL;
    public static String DB_PORT;
    public static String DB_NAME;
    public static String DB_USER;
    public static String DB_PASS;
    public static String DB_DRIVER;

    public static class DB {
        public static String URL;
        public static String USER;
        public static String PASS;
        public static String TYPE;

        public static String DRIVER;

        public static String DIALECT;
    }

    public static void setDB(String type, String prefix, String host, String port, String db, String username, String password) {
        DB.TYPE = type;
        DB.USER = username;
        DB.PASS = password;
        DB.URL = createJDBCString(type, host, port, db);
        DB.DRIVER = getDriver(type);
        DB.DIALECT = getDialect(type);
        PrefixPhysicalNamingStrategy.TABLE_NAME_PREFIX = prefix;
    }

    private static String createJDBCString(String type, String host, String port, String db) {
        return "jdbc:" + type + "://" + host + ":" + port + "/" + db;
    }

    private static String getDialect(String type) {
        return switch (type) {
            case "mysql" -> "org.hibernate.dialect.MySQL5Dialect";
            case "sqlite" -> "org.sqlite.hibernate.dialect.SQLiteDialect";
            case "postgresql" -> "org.hibernate.dialect.PostgreSQLDialect";
            case "oracle" -> "org.hibernate.dialect.Oracle10gDialect";
            case "mssql" -> "org.hibernate.dialect.SQLServer2008Dialect";
            case "maria" -> "org.hibernate.dialect.MariaDBDialect";
            case "h2" -> "org.hibernate.dialect.H2Dialect";
            default -> "";
        };
    }

    private static String getDriver(String type) {
        return switch (type) {
            case "mysql" -> "com.mysql.jdbc.Driver";
            case "sqlite" -> "org.sqlite.JDBC";
            case "postgresql" -> "org.postgresql.Driver";
            case "oracle" -> "oracle.jdbc.driver.OracleDriver";
            case "mssql" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "maria" -> "org.mariadb.jdbc.Driver";
            case "h2" -> "org.h2.Driver";
            default -> "";
        };
    }
}
