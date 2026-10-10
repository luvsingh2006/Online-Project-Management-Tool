package com.pmtool.db.embedded;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

public class EmbeddedResultSetMetaData implements ResultSetMetaData {

    private final List<String> columnNames;

    public EmbeddedResultSetMetaData(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    @Override public int getColumnCount() throws SQLException { return columnNames.size(); }
    @Override public String getColumnName(int column) throws SQLException { checkIndex(column); return columnNames.get(column - 1); }
    @Override public String getColumnLabel(int column) throws SQLException { return getColumnName(column); }
    @Override public String getColumnTypeName(int column) throws SQLException { return "VARCHAR"; }
    @Override public int getColumnType(int column) throws SQLException { return java.sql.Types.VARCHAR; }

    private void checkIndex(int column) throws SQLException {
        if (column < 1 || column > columnNames.size()) {
            throw new SQLException("Invalid column index: " + column);
        }
    }

    @Override public boolean isAutoIncrement(int column) { return false; }
    @Override public boolean isCaseSensitive(int column) { return false; }
    @Override public boolean isSearchable(int column) { return true; }
    @Override public boolean isCurrency(int column) { return false; }
    @Override public int isNullable(int column) { return columnNullable; }
    @Override public boolean isSigned(int column) { return false; }
    @Override public int getColumnDisplaySize(int column) { return 255; }
    @Override public String getSchemaName(int column) { return ""; }
    @Override public int getPrecision(int column) { return 0; }
    @Override public int getScale(int column) { return 0; }
    @Override public String getTableName(int column) { return ""; }
    @Override public String getCatalogName(int column) { return ""; }
    @Override public boolean isReadOnly(int column) { return false; }
    @Override public boolean isWritable(int column) { return true; }
    @Override public boolean isDefinitelyWritable(int column) { return true; }
    @Override public String getColumnClassName(int column) { return String.class.getName(); }
    @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("Unsupported unwrap"); }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
}
