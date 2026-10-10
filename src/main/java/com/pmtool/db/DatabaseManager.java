package com.pmtool.db;

import com.pmtool.config.AppConfig;
import com.pmtool.db.pool.ConnectionPool;
import com.pmtool.db.pool.ThreadSafeConnectionPool;
import com.pmtool.db.transaction.TransactionManager;
import com.pmtool.exception.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static volatile DatabaseManager instance;

    private final ConnectionPool connectionPool;
    private final TransactionManager transactionManager;

    private DatabaseManager() {
        try {
            String driverClass = AppConfig.getString("db.driver", "com.pmtool.db.embedded.EmbeddedJdbcDriver");
            try {
                Class.forName(driverClass);
                LOGGER.info("Registered JDBC Driver: " + driverClass);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "JDBC Driver class not found: " + driverClass, e);
            }

            String url = AppConfig.getString("db.url", "jdbc:embedded:data/pmtool.db");
            String user = AppConfig.getString("db.username", "admin");
            String pass = AppConfig.getString("db.password", "pmtool_secure_pass");
            int initialSize = AppConfig.getInt("db.pool.initialSize", 5);
            int maxSize = AppConfig.getInt("db.pool.maxSize", 20);
            long waitMillis = AppConfig.getLong("db.pool.maxWaitMillis", 5000);

            this.connectionPool = new ThreadSafeConnectionPool(url, user, pass, initialSize, maxSize, waitMillis);
            this.transactionManager = new TransactionManager(this.connectionPool);
            LOGGER.info("DatabaseManager initialized with URL: " + url);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize DatabaseManager", e);
            throw new DatabaseException("Failed to initialize database connection pool", e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void shutdown() {
        if (connectionPool != null) {
            connectionPool.close();
        }
    }
}
