package com.pmtool.db.embedded;

import java.io.Serializable;
import java.util.*;

public class RowData implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Object> values = new LinkedHashMap<>();

    public RowData() {}

    public RowData(Map<String, Object> initialValues) {
        if (initialValues != null) {
            for (Map.Entry<String, Object> entry : initialValues.entrySet()) {
                values.put(entry.getKey().toUpperCase(Locale.ROOT), entry.getValue());
            }
        }
    }

    public void set(String columnName, Object value) {
        values.put(columnName.toUpperCase(Locale.ROOT), value);
    }

    public Object get(String columnName) {
        return values.get(columnName.toUpperCase(Locale.ROOT));
    }

    public boolean has(String columnName) {
        return values.containsKey(columnName.toUpperCase(Locale.ROOT));
    }

    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(values);
    }

    @Override
    public RowData clone() {
        RowData copy = new RowData();
        copy.values.putAll(this.values);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RowData rowData = (RowData) o;
        return Objects.equals(values, rowData.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
