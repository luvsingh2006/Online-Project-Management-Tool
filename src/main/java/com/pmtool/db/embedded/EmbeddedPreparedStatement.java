package com.pmtool.db.embedded;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class EmbeddedPreparedStatement implements PreparedStatement {

    private final EmbeddedConnection connection;
    private final String sql;
    private final Map<Integer, Object> parameters = new HashMap<>();
    private final List<Long> generatedKeys = new ArrayList<>();
    private ResultSet currentResultSet;
    private int updateCount = -1;
    private boolean closed = false;

    public EmbeddedPreparedStatement(EmbeddedConnection connection, String sql) {
        this.connection = connection;
        this.sql = sql;
    }

    private List<Object> buildOrderedParams() {
        int maxIndex = 0;
        for (Integer idx : parameters.keySet()) {
            if (idx > maxIndex) maxIndex = idx;
        }
        List<Object> list = new ArrayList<>(maxIndex);
        for (int i = 1; i <= maxIndex; i++) {
            list.add(parameters.get(i));
        }
        return list;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        checkClosed();
        QueryResult qr = connection.getEngine().executeQuery(sql, buildOrderedParams());
        this.currentResultSet = new EmbeddedResultSet(qr);
        this.updateCount = -1;
        return currentResultSet;
    }

    @Override
    public int executeUpdate() throws SQLException {
        checkClosed();
        generatedKeys.clear();
        this.updateCount = connection.getEngine().executeUpdate(sql, buildOrderedParams(), generatedKeys);
        return updateCount;
    }

    @Override
    public boolean execute() throws SQLException {
        checkClosed();
        String upper = sql.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("SELECT")) {
            executeQuery();
            return true;
        } else {
            executeUpdate();
            return false;
        }
    }

    @Override public ResultSet getResultSet() throws SQLException { return currentResultSet; }
    @Override public int getUpdateCount() throws SQLException { return updateCount; }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        QueryResult qr = new QueryResult(List.of("ID"));
        for (Long k : generatedKeys) {
            RowData r = new RowData();
            r.set("ID", k);
            qr.addRow(r);
        }
        return new EmbeddedResultSet(qr);
    }

    @Override public void setNull(int parameterIndex, int sqlType) throws SQLException { parameters.put(parameterIndex, null); }
    @Override public void setBoolean(int parameterIndex, boolean x) throws SQLException { parameters.put(parameterIndex, x); }
    @Override public void setInt(int parameterIndex, int x) throws SQLException { parameters.put(parameterIndex, x); }
    @Override public void setLong(int parameterIndex, long x) throws SQLException { parameters.put(parameterIndex, x); }
    @Override public void setDouble(int parameterIndex, double x) throws SQLException { parameters.put(parameterIndex, x); }
    @Override public void setString(int parameterIndex, String x) throws SQLException { parameters.put(parameterIndex, x); }
    @Override public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException { parameters.put(parameterIndex, x); }
    @Override public void setObject(int parameterIndex, Object x) throws SQLException { parameters.put(parameterIndex, x); }
    @Override public void clearParameters() throws SQLException { parameters.clear(); }

    @Override
    public void close() throws SQLException {
        this.closed = true;
        if (currentResultSet != null) {
            currentResultSet.close();
        }
    }

    @Override public boolean isClosed() throws SQLException { return closed; }

    private void checkClosed() throws SQLException {
        if (closed) throw new SQLException("PreparedStatement is closed");
    }

    @Override public ResultSet executeQuery(String sql) throws SQLException { return connection.createStatement().executeQuery(sql); }
    @Override public int executeUpdate(String sql) throws SQLException { return connection.createStatement().executeUpdate(sql); }
    @Override public int getMaxFieldSize() { return 0; }
    @Override public void setMaxFieldSize(int max) {}
    @Override public int getMaxRows() { return 0; }
    @Override public void setMaxRows(int max) {}
    @Override public void setEscapeProcessing(boolean enable) {}
    @Override public int getQueryTimeout() { return 0; }
    @Override public void setQueryTimeout(int seconds) {}
    @Override public void cancel() {}
    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() {}
    @Override public void setCursorName(String name) {}
    @Override public boolean execute(String sql) throws SQLException { return connection.createStatement().execute(sql); }
    @Override public boolean getMoreResults() { return false; }
    @Override public void setFetchDirection(int direction) {}
    @Override public int getFetchDirection() { return ResultSet.FETCH_FORWARD; }
    @Override public void setFetchSize(int rows) {}
    @Override public int getFetchSize() { return 0; }
    @Override public int getResultSetConcurrency() { return ResultSet.CONCUR_READ_ONLY; }
    @Override public int getResultSetType() { return ResultSet.TYPE_FORWARD_ONLY; }
    @Override public void addBatch(String sql) {}
    @Override public void clearBatch() {}
    @Override public int[] executeBatch() { return new int[0]; }
    @Override public Connection getConnection() { return connection; }
    @Override public boolean getMoreResults(int current) { return false; }
    @Override public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, int[] columnIndexes) throws SQLException { return executeUpdate(sql); }
    @Override public int executeUpdate(String sql, String[] columnNames) throws SQLException { return executeUpdate(sql); }
    @Override public boolean execute(String sql, int autoGeneratedKeys) throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, int[] columnIndexes) throws SQLException { return execute(sql); }
    @Override public boolean execute(String sql, String[] columnNames) throws SQLException { return execute(sql); }
    @Override public int getResultSetHoldability() { return 0; }
    @Override public void setPoolable(boolean poolable) {}
    @Override public boolean isPoolable() { return false; }
    @Override public void closeOnCompletion() {}
    @Override public boolean isCloseOnCompletion() { return false; }
    @Override public void setByte(int parameterIndex, byte x) { parameters.put(parameterIndex, (int) x); }
    @Override public void setShort(int parameterIndex, short x) { parameters.put(parameterIndex, (int) x); }
    @Override public void setFloat(int parameterIndex, float x) { parameters.put(parameterIndex, (double) x); }
    @Override public void setBigDecimal(int parameterIndex, BigDecimal x) { parameters.put(parameterIndex, x != null ? x.doubleValue() : null); }
    @Override public void setBytes(int parameterIndex, byte[] x) { parameters.put(parameterIndex, x); }
    @Override public void setDate(int parameterIndex, java.sql.Date x) { parameters.put(parameterIndex, x != null ? new Timestamp(x.getTime()) : null); }
    @Override public void setTime(int parameterIndex, Time x) { parameters.put(parameterIndex, x != null ? new Timestamp(x.getTime()) : null); }
    @Override public void setAsciiStream(int parameterIndex, InputStream x, int length) {}
    @Override public void setUnicodeStream(int parameterIndex, InputStream x, int length) {}
    @Override public void setBinaryStream(int parameterIndex, InputStream x, int length) {}
    @Override public void addBatch() {}
    @Override public void setCharacterStream(int parameterIndex, Reader reader, int length) {}
    @Override public void setRef(int parameterIndex, Ref x) {}
    @Override public void setBlob(int parameterIndex, Blob x) {}
    @Override public void setClob(int parameterIndex, Clob x) {}
    @Override public void setArray(int parameterIndex, Array x) {}
    @Override public ResultSetMetaData getMetaData() { return null; }
    @Override public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) {}
    @Override public void setTime(int parameterIndex, Time x, Calendar cal) {}
    @Override public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) {}
    @Override public void setNull(int parameterIndex, int sqlType, String typeName) {}
    @Override public void setURL(int parameterIndex, URL x) {}
    @Override public ParameterMetaData getParameterMetaData() { return null; }
    @Override public void setRowId(int parameterIndex, RowId x) {}
    @Override public void setNString(int parameterIndex, String value) throws SQLException { setString(parameterIndex, value); }
    @Override public void setNCharacterStream(int parameterIndex, Reader value, long length) {}
    @Override public void setNClob(int parameterIndex, NClob value) {}
    @Override public void setClob(int parameterIndex, Reader reader, long length) {}
    @Override public void setBlob(int parameterIndex, InputStream inputStream, long length) {}
    @Override public void setNClob(int parameterIndex, Reader reader, long length) {}
    @Override public void setSQLXML(int parameterIndex, SQLXML xmlObject) {}
    @Override public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException { setObject(parameterIndex, x); }
    @Override public void setAsciiStream(int parameterIndex, InputStream x, long length) {}
    @Override public void setBinaryStream(int parameterIndex, InputStream x, long length) {}
    @Override public void setCharacterStream(int parameterIndex, Reader reader, long length) {}
    @Override public void setAsciiStream(int parameterIndex, InputStream x) {}
    @Override public void setBinaryStream(int parameterIndex, InputStream x) {}
    @Override public void setCharacterStream(int parameterIndex, Reader reader) {}
    @Override public void setNCharacterStream(int parameterIndex, Reader value) {}
    @Override public void setClob(int parameterIndex, Reader reader) {}
    @Override public void setBlob(int parameterIndex, InputStream inputStream) {}
    @Override public void setNClob(int parameterIndex, Reader reader) {}
    @Override public <T> T unwrap(Class<T> iface) { return null; }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
}
