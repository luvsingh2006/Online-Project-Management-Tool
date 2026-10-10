package com.pmtool.db.transaction;

import com.pmtool.db.pool.ConnectionPool;
import com.pmtool.exception.DatabaseException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionManager {

    private static final Logger LOGGER = Logger.getLogger(TransactionManager.class.getName());
    private final ConnectionPool connectionPool;

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction(Connection connection) throws Exception;
    }

    public TransactionManager(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public <T> T execute(TransactionCallback<T> callback) {
        Connection conn = null;
        boolean originalAutoCommit = true;
        try {
            conn = connectionPool.getConnection();
            originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            T result = callback.doInTransaction(conn);

            conn.commit();
            return result;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    LOGGER.warning("Rolling back active transaction due to failure: " + e.getMessage());
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
            }
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new DatabaseException("Transaction execution failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
