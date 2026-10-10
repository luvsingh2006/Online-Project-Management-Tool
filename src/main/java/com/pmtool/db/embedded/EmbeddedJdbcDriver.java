package com.pmtool.db.embedded;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class EmbeddedJdbcDriver implements Driver {

    public static final String URL_PREFIX = "jdbc:embedded:";
    private static final Logger LOGGER = Logger.getLogger(EmbeddedJdbcDriver.class.getName());

    static {
        try {
            DriverManager.registerDriver(new EmbeddedJdbcDriver());
        } catch (SQLException e) {
            LOGGER.severe("Failed to register EmbeddedJdbcDriver: " + e.getMessage());
        }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        String dbPath = url.substring(URL_PREFIX.length()).trim();
        if (dbPath.isEmpty()) {
            dbPath = "data/pmtool.db";
        }
        EmbeddedDatabaseEngine engine = EmbeddedDatabaseEngine.getOrCreate(dbPath);
        return new EmbeddedConnection(engine, url);
    }

    @Override public boolean acceptsURL(String url) throws SQLException { return url != null && url.startsWith(URL_PREFIX); }
    @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
    @Override public int getMajorVersion() { return 1; }
    @Override public int getMinorVersion() { return 0; }
    @Override public boolean jdbcCompliant() { return false; }
    @Override public Logger getParentLogger() { return LOGGER; }
}
