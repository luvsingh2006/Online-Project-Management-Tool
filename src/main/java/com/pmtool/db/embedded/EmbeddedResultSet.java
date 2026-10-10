package com.pmtool.db.embedded;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmbeddedResultSet implements ResultSet {

    private final QueryResult queryResult;
    private final List<RowData> rows;
    private final List<String> columnNames;
    private int cursor = -1;
    private boolean lastWasNull = false;
    private boolean closed = false;

    public EmbeddedResultSet(QueryResult queryResult) {
        this.queryResult = queryResult;
        this.rows = queryResult.getRows();
        this.columnNames = queryResult.getColumnNames();
    }

    private RowData currentRow() throws SQLException {
        if (closed) throw new SQLException("ResultSet is closed");
        if (cursor < 0 || cursor >= rows.size()) throw new SQLException("Cursor out of bounds");
        return rows.get(cursor);
    }

    private Object getValue(String columnLabel) throws SQLException {
        RowData row = currentRow();
        Object val = row.get(columnLabel);
        lastWasNull = (val == null);
        return val;
    }

    private Object getValue(int columnIndex) throws SQLException {
        if (columnIndex < 1 || columnIndex > columnNames.size()) {
            throw new SQLException("Invalid column index: " + columnIndex);
        }
        return getValue(columnNames.get(columnIndex - 1));
    }

    @Override public boolean next() throws SQLException {
        if (closed) return false;
        cursor++;
        return cursor < rows.size();
    }

    @Override public void close() throws SQLException { this.closed = true; }
    @Override public boolean wasNull() throws SQLException { return lastWasNull; }
    @Override public String getString(int columnIndex) throws SQLException { Object v = getValue(columnIndex); return v != null ? String.valueOf(v) : null; }
    @Override public String getString(String columnLabel) throws SQLException { Object v = getValue(columnLabel); return v != null ? String.valueOf(v) : null; }

    @Override public boolean getBoolean(int columnIndex) throws SQLException { Object v = getValue(columnIndex); if (v == null) return false; return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v)); }
    @Override public boolean getBoolean(String columnLabel) throws SQLException { Object v = getValue(columnLabel); if (v == null) return false; return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v)); }

    @Override public int getInt(int columnIndex) throws SQLException { Object v = getValue(columnIndex); if (v == null) return 0; if (v instanceof Number n) return n.intValue(); try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; } }
    @Override public int getInt(String columnLabel) throws SQLException { Object v = getValue(columnLabel); if (v == null) return 0; if (v instanceof Number n) return n.intValue(); try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return 0; } }

    @Override public long getLong(int columnIndex) throws SQLException { Object v = getValue(columnIndex); if (v == null) return 0L; if (v instanceof Number n) return n.longValue(); try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; } }
    @Override public long getLong(String columnLabel) throws SQLException { Object v = getValue(columnLabel); if (v == null) return 0L; if (v instanceof Number n) return n.longValue(); try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return 0L; } }

    @Override public double getDouble(int columnIndex) throws SQLException { Object v = getValue(columnIndex); if (v == null) return 0.0; if (v instanceof Number n) return n.doubleValue(); try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; } }
    @Override public double getDouble(String columnLabel) throws SQLException { Object v = getValue(columnLabel); if (v == null) return 0.0; if (v instanceof Number n) return n.doubleValue(); try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return 0.0; } }

    @Override public Timestamp getTimestamp(int columnIndex) throws SQLException { Object v = getValue(columnIndex); if (v == null) return null; if (v instanceof Timestamp ts) return ts; if (v instanceof java.util.Date d) return new Timestamp(d.getTime()); try { return Timestamp.valueOf(String.valueOf(v)); } catch (Exception e) { return null; } }
    @Override public Timestamp getTimestamp(String columnLabel) throws SQLException { Object v = getValue(columnLabel); if (v == null) return null; if (v instanceof Timestamp ts) return ts; if (v instanceof java.util.Date d) return new Timestamp(d.getTime()); try { return Timestamp.valueOf(String.valueOf(v)); } catch (Exception e) { return null; } }

    @Override public Object getObject(int columnIndex) throws SQLException { return getValue(columnIndex); }
    @Override public Object getObject(String columnLabel) throws SQLException { return getValue(columnLabel); }
    @Override public ResultSetMetaData getMetaData() throws SQLException { return new EmbeddedResultSetMetaData(columnNames); }

    @Override public int findColumn(String columnLabel) throws SQLException {
        String upper = columnLabel.toUpperCase(Locale.ROOT);
        for (int i = 0; i < columnNames.size(); i++) {
            if (columnNames.get(i).equalsIgnoreCase(upper)) return i + 1;
        }
        throw new SQLException("Column not found: " + columnLabel);
    }

    @Override public boolean isClosed() throws SQLException { return closed; }

    @Override public byte getByte(int columnIndex) throws SQLException { return (byte) getInt(columnIndex); }
    @Override public short getShort(int columnIndex) throws SQLException { return (short) getInt(columnIndex); }
    @Override public float getFloat(int columnIndex) throws SQLException { return (float) getDouble(columnIndex); }
    @Override public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException { return BigDecimal.valueOf(getDouble(columnIndex)); }
    @Override public byte[] getBytes(int columnIndex) throws SQLException { return new byte[0]; }
    @Override public Date getDate(int columnIndex) throws SQLException { Timestamp ts = getTimestamp(columnIndex); return ts != null ? new Date(ts.getTime()) : null; }
    @Override public Time getTime(int columnIndex) throws SQLException { Timestamp ts = getTimestamp(columnIndex); return ts != null ? new Time(ts.getTime()) : null; }
    @Override public byte getByte(String columnLabel) throws SQLException { return (byte) getInt(columnLabel); }
    @Override public short getShort(String columnLabel) throws SQLException { return (short) getInt(columnLabel); }
    @Override public float getFloat(String columnLabel) throws SQLException { return (float) getDouble(columnLabel); }
    @Override public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException { return BigDecimal.valueOf(getDouble(columnLabel)); }
    @Override public byte[] getBytes(String columnLabel) throws SQLException { return new byte[0]; }
    @Override public Date getDate(String columnLabel) throws SQLException { Timestamp ts = getTimestamp(columnLabel); return ts != null ? new Date(ts.getTime()) : null; }
    @Override public Time getTime(String columnLabel) throws SQLException { Timestamp ts = getTimestamp(columnLabel); return ts != null ? new Time(ts.getTime()) : null; }
    @Override public InputStream getAsciiStream(int columnIndex) { return null; }
    @Override public InputStream getUnicodeStream(int columnIndex) { return null; }
    @Override public InputStream getBinaryStream(int columnIndex) { return null; }
    @Override public InputStream getAsciiStream(String columnLabel) { return null; }
    @Override public InputStream getUnicodeStream(String columnLabel) { return null; }
    @Override public InputStream getBinaryStream(String columnLabel) { return null; }
    @Override public SQLWarning getWarnings() { return null; }
    @Override public void clearWarnings() {}
    @Override public String getCursorName() { return null; }
    @Override public Reader getCharacterStream(int columnIndex) { return null; }
    @Override public Reader getCharacterStream(String columnLabel) { return null; }
    @Override public BigDecimal getBigDecimal(int columnIndex) { return BigDecimal.valueOf(0); }
    @Override public BigDecimal getBigDecimal(String columnLabel) { return BigDecimal.valueOf(0); }
    @Override public boolean isBeforeFirst() { return cursor == -1; }
    @Override public boolean isAfterLast() { return cursor >= rows.size(); }
    @Override public boolean isFirst() { return cursor == 0; }
    @Override public boolean isLast() { return cursor == rows.size() - 1; }
    @Override public void beforeFirst() { cursor = -1; }
    @Override public void afterLast() { cursor = rows.size(); }
    @Override public boolean first() { if (rows.isEmpty()) return false; cursor = 0; return true; }
    @Override public boolean last() { if (rows.isEmpty()) return false; cursor = rows.size() - 1; return true; }
    @Override public int getRow() { return cursor + 1; }
    @Override public boolean absolute(int row) { cursor = row - 1; return cursor >= 0 && cursor < rows.size(); }
    @Override public boolean relative(int rowsDelta) { cursor += rowsDelta; return cursor >= 0 && cursor < rows.size(); }
    @Override public boolean previous() { cursor--; return cursor >= 0; }
    @Override public void setFetchDirection(int direction) {}
    @Override public int getFetchDirection() { return FETCH_FORWARD; }
    @Override public void setFetchSize(int rows) {}
    @Override public int getFetchSize() { return 0; }
    @Override public int getType() { return TYPE_FORWARD_ONLY; }
    @Override public int getConcurrency() { return CONCUR_READ_ONLY; }
    @Override public boolean rowUpdated() { return false; }
    @Override public boolean rowInserted() { return false; }
    @Override public boolean rowDeleted() { return false; }
    @Override public void updateNull(int columnIndex) {}
    @Override public void updateBoolean(int columnIndex, boolean x) {}
    @Override public void updateByte(int columnIndex, byte x) {}
    @Override public void updateShort(int columnIndex, short x) {}
    @Override public void updateInt(int columnIndex, int x) {}
    @Override public void updateLong(int columnIndex, long x) {}
    @Override public void updateFloat(int columnIndex, float x) {}
    @Override public void updateDouble(int columnIndex, double x) {}
    @Override public void updateBigDecimal(int columnIndex, BigDecimal x) {}
    @Override public void updateString(int columnIndex, String x) {}
    @Override public void updateBytes(int columnIndex, byte[] x) {}
    @Override public void updateDate(int columnIndex, Date x) {}
    @Override public void updateTime(int columnIndex, Time x) {}
    @Override public void updateTimestamp(int columnIndex, Timestamp x) {}
    @Override public void updateAsciiStream(int columnIndex, InputStream x, int length) {}
    @Override public void updateBinaryStream(int columnIndex, InputStream x, int length) {}
    @Override public void updateCharacterStream(int columnIndex, Reader x, int length) {}
    @Override public void updateObject(int columnIndex, Object x, int scaleOrLength) {}
    @Override public void updateObject(int columnIndex, Object x) {}
    @Override public void updateNull(String columnLabel) {}
    @Override public void updateBoolean(String columnLabel, boolean x) {}
    @Override public void updateByte(String columnLabel, byte x) {}
    @Override public void updateShort(String columnLabel, short x) {}
    @Override public void updateInt(String columnLabel, int x) {}
    @Override public void updateLong(String columnLabel, long x) {}
    @Override public void updateFloat(String columnLabel, float x) {}
    @Override public void updateDouble(String columnLabel, double x) {}
    @Override public void updateBigDecimal(String columnLabel, BigDecimal x) {}
    @Override public void updateString(String columnLabel, String x) {}
    @Override public void updateBytes(String columnLabel, byte[] x) {}
    @Override public void updateDate(String columnLabel, Date x) {}
    @Override public void updateTime(String columnLabel, Time x) {}
    @Override public void updateTimestamp(String columnLabel, Timestamp x) {}
    @Override public void updateAsciiStream(String columnLabel, InputStream x, int length) {}
    @Override public void updateBinaryStream(String columnLabel, InputStream x, int length) {}
    @Override public void updateCharacterStream(String columnLabel, Reader reader, int length) {}
    @Override public void updateObject(String columnLabel, Object x, int scaleOrLength) {}
    @Override public void updateObject(String columnLabel, Object x) {}
    @Override public void insertRow() {}
    @Override public void updateRow() {}
    @Override public void deleteRow() {}
    @Override public void refreshRow() {}
    @Override public void cancelRowUpdates() {}
    @Override public void moveToInsertRow() {}
    @Override public void moveToCurrentRow() {}
    @Override public Statement getStatement() { return null; }
    @Override public Object getObject(int columnIndex, Map<String, Class<?>> map) { return null; }
    @Override public Ref getRef(int columnIndex) { return null; }
    @Override public Blob getBlob(int columnIndex) { return null; }
    @Override public Clob getClob(int columnIndex) { return null; }
    @Override public Array getArray(int columnIndex) { return null; }
    @Override public Object getObject(String columnLabel, Map<String, Class<?>> map) { return null; }
    @Override public Ref getRef(String columnLabel) { return null; }
    @Override public Blob getBlob(String columnLabel) { return null; }
    @Override public Clob getClob(String columnLabel) { return null; }
    @Override public Array getArray(String columnLabel) { return null; }
    @Override public Date getDate(int columnIndex, Calendar cal) { return null; }
    @Override public Date getDate(String columnLabel, Calendar cal) { return null; }
    @Override public Time getTime(int columnIndex, Calendar cal) { return null; }
    @Override public Time getTime(String columnLabel, Calendar cal) { return null; }
    @Override public Timestamp getTimestamp(int columnIndex, Calendar cal) { return null; }
    @Override public Timestamp getTimestamp(String columnLabel, Calendar cal) { return null; }
    @Override public URL getURL(int columnIndex) { return null; }
    @Override public URL getURL(String columnLabel) { return null; }
    @Override public void updateRef(int columnIndex, Ref x) {}
    @Override public void updateRef(String columnLabel, Ref x) {}
    @Override public void updateBlob(int columnIndex, Blob x) {}
    @Override public void updateBlob(String columnLabel, Blob x) {}
    @Override public void updateClob(int columnIndex, Clob x) {}
    @Override public void updateClob(String columnLabel, Clob x) {}
    @Override public void updateArray(int columnIndex, Array x) {}
    @Override public void updateArray(String columnLabel, Array x) {}
    @Override public RowId getRowId(int columnIndex) { return null; }
    @Override public RowId getRowId(String columnLabel) { return null; }
    @Override public void updateRowId(int columnIndex, RowId x) {}
    @Override public void updateRowId(String columnLabel, RowId x) {}
    @Override public int getHoldability() { return 0; }
    @Override public void updateNString(int columnIndex, String nString) {}
    @Override public void updateNString(String columnLabel, String nString) {}
    @Override public void updateNClob(int columnIndex, NClob nClob) {}
    @Override public void updateNClob(String columnLabel, NClob nClob) {}
    @Override public NClob getNClob(int columnIndex) { return null; }
    @Override public NClob getNClob(String columnLabel) { return null; }
    @Override public SQLXML getSQLXML(int columnIndex) { return null; }
    @Override public SQLXML getSQLXML(String columnLabel) { return null; }
    @Override public void updateSQLXML(int columnIndex, SQLXML xmlObject) {}
    @Override public void updateSQLXML(String columnLabel, SQLXML xmlObject) {}
    @Override public String getNString(int columnIndex) { return null; }
    @Override public String getNString(String columnLabel) { return null; }
    @Override public Reader getNCharacterStream(int columnIndex) { return null; }
    @Override public Reader getNCharacterStream(String columnLabel) { return null; }
    @Override public void updateNCharacterStream(int columnIndex, Reader x, long length) {}
    @Override public void updateNCharacterStream(String columnLabel, Reader reader, long length) {}
    @Override public void updateAsciiStream(int columnIndex, InputStream x, long length) {}
    @Override public void updateBinaryStream(int columnIndex, InputStream x, long length) {}
    @Override public void updateCharacterStream(int columnIndex, Reader x, long length) {}
    @Override public void updateAsciiStream(String columnLabel, InputStream x, long length) {}
    @Override public void updateBinaryStream(String columnLabel, InputStream x, long length) {}
    @Override public void updateCharacterStream(String columnLabel, Reader reader, long length) {}
    @Override public void updateBlob(int columnIndex, InputStream inputStream, long length) {}
    @Override public void updateBlob(String columnLabel, InputStream inputStream, long length) {}
    @Override public void updateClob(int columnIndex, Reader reader, long length) {}
    @Override public void updateClob(String columnLabel, Reader reader, long length) {}
    @Override public void updateNClob(int columnIndex, Reader reader, long length) {}
    @Override public void updateNClob(String columnLabel, Reader reader, long length) {}
    @Override public void updateNCharacterStream(int columnIndex, Reader x) {}
    @Override public void updateNCharacterStream(String columnLabel, Reader reader) {}
    @Override public void updateAsciiStream(int columnIndex, InputStream x) {}
    @Override public void updateBinaryStream(int columnIndex, InputStream x) {}
    @Override public void updateCharacterStream(int columnIndex, Reader x) {}
    @Override public void updateAsciiStream(String columnLabel, InputStream x) {}
    @Override public void updateBinaryStream(String columnLabel, InputStream x) {}
    @Override public void updateCharacterStream(String columnLabel, Reader reader) {}
    @Override public void updateBlob(int columnIndex, InputStream inputStream) {}
    @Override public void updateBlob(String columnLabel, InputStream inputStream) {}
    @Override public void updateClob(int columnIndex, Reader reader) {}
    @Override public void updateClob(String columnLabel, Reader reader) {}
    @Override public void updateNClob(int columnIndex, Reader reader) {}
    @Override public void updateNClob(String columnLabel, Reader reader) {}
    @Override public <T> T getObject(int columnIndex, Class<T> type) { return null; }
    @Override public <T> T getObject(String columnLabel, Class<T> type) { return null; }
    @Override public <T> T unwrap(Class<T> iface) { return null; }
    @Override public boolean isWrapperFor(Class<?> iface) { return false; }
}
