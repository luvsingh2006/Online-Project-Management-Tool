package com.pmtool.db.embedded;

import java.io.Serializable;
import java.util.Locale;

public class ColumnDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String dataType;
    private final boolean isPrimaryKey;
    private final boolean isAutoIncrement;
    private final boolean isNullable;
    private final String defaultValue;

    public ColumnDefinition(String name, String dataType, boolean isPrimaryKey, boolean isAutoIncrement, boolean isNullable, String defaultValue) {
        this.name = name.toUpperCase(Locale.ROOT);
        this.dataType = dataType.toUpperCase(Locale.ROOT);
        this.isPrimaryKey = isPrimaryKey;
        this.isAutoIncrement = isAutoIncrement;
        this.isNullable = isNullable;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public String getDataType() { return dataType; }
    public boolean isPrimaryKey() { return isPrimaryKey; }
    public boolean isAutoIncrement() { return isAutoIncrement; }
    public boolean isNullable() { return isNullable; }
    public String getDefaultValue() { return defaultValue; }
}
