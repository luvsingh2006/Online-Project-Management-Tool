package com.pmtool.db.embedded;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryResult {
    private final List<String> columnNames;
    private final List<RowData> rows = new ArrayList<>();

    public QueryResult(List<String> columnNames) {
        this.columnNames = new ArrayList<>(columnNames);
    }

    public void addRow(RowData row) { rows.add(row); }
    public List<String> getColumnNames() { return Collections.unmodifiableList(columnNames); }
    public List<RowData> getRows() { return Collections.unmodifiableList(rows); }
    public int size() { return rows.size(); }
}
