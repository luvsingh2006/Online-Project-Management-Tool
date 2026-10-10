package com.pmtool.db.pool;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionPool extends AutoCloseable {
    Connection getConnection() throws SQLException;
    void releaseConnection(Connection connection);
    int getActiveConnections();
    int getIdleConnections();
    @Override
    void close();
}
