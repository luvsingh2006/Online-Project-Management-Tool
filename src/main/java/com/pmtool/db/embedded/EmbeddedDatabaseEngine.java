package com.pmtool.db.embedded;

import java.io.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmbeddedDatabaseEngine implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(EmbeddedDatabaseEngine.class.getName());

    private static final Map<String, EmbeddedDatabaseEngine> instances = new ConcurrentHashMap<>();

    private final String dbName;
    private final String dataFilePath;
    private final Map<String, TableData> tables = new ConcurrentHashMap<>();

    private EmbeddedDatabaseEngine(String dbName, String dataFilePath) {
        this.dbName = dbName;
        this.dataFilePath = dataFilePath;
        loadFromDisk();
    }

    public static EmbeddedDatabaseEngine getOrCreate(String dbPath) {
        return instances.computeIfAbsent(dbPath, path -> new EmbeddedDatabaseEngine("pmtool", path));
    }

    public synchronized void persist() {
        if (dataFilePath == null || dataFilePath.isBlank() || dataFilePath.equals(":memory:")) {
            return;
        }
        File file = new File(dataFilePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new HashMap<>(tables));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to persist embedded database: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromDisk() {
        if (dataFilePath == null || dataFilePath.isBlank() || dataFilePath.equals(":memory:")) {
            return;
        }
        File file = new File(dataFilePath);
        if (!file.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, TableData> loaded = (Map<String, TableData>) ois.readObject();
            tables.putAll(loaded);
            LOGGER.info("Successfully loaded " + tables.size() + " tables from " + dataFilePath);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load database snapshot; starting fresh: " + e.getMessage());
        }
    }

    public TableData getTable(String name) {
        return tables.get(name.toUpperCase(Locale.ROOT));
    }

    public synchronized void createTable(String tableName, List<ColumnDefinition> cols) {
        String key = tableName.toUpperCase(Locale.ROOT);
        TableData table = tables.computeIfAbsent(key, TableData::new);
        for (ColumnDefinition col : cols) {
            table.addColumn(col);
        }
        persist();
    }

    public int executeUpdate(String sql, List<Object> params, List<Long> generatedKeys) throws SQLException {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);

        if (upper.startsWith("CREATE TABLE")) {
            parseAndExecuteCreateTable(trimmed);
            return 0;
        } else if (upper.startsWith("INSERT INTO")) {
            return parseAndExecuteInsert(trimmed, params, generatedKeys);
        } else if (upper.startsWith("UPDATE")) {
            return parseAndExecuteUpdate(trimmed, params);
        } else if (upper.startsWith("DELETE FROM")) {
            return parseAndExecuteDelete(trimmed, params);
        } else if (upper.startsWith("DROP TABLE")) {
            parseAndExecuteDrop(trimmed);
            return 0;
        }
        throw new SQLException("Unsupported update SQL statement: " + sql);
    }

    public QueryResult executeQuery(String sql, List<Object> params) throws SQLException {
        String trimmed = sql.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);

        if (upper.startsWith("SELECT")) {
            return parseAndExecuteSelect(trimmed, params);
        }
        throw new SQLException("Unsupported query statement: " + sql);
    }

    private void parseAndExecuteCreateTable(String sql) throws SQLException {
        Pattern pattern = Pattern.compile("CREATE\\s+TABLE(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+([A-Za-z0-9_]+)\\s*\\((.*)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new SQLException("Malformed CREATE TABLE SQL: " + sql);
        }

        String tableName = matcher.group(1).trim().toUpperCase(Locale.ROOT);
        String body = matcher.group(2).trim();

        List<String> colDefs = splitSqlTokens(body);
        List<ColumnDefinition> cols = new ArrayList<>();

        for (String def : colDefs) {
            String colTrimmed = def.trim();
            if (colTrimmed.toUpperCase(Locale.ROOT).startsWith("CONSTRAINT") || colTrimmed.toUpperCase(Locale.ROOT).startsWith("PRIMARY KEY (") || colTrimmed.toUpperCase(Locale.ROOT).startsWith("FOREIGN KEY")) {
                continue;
            }
            String[] parts = colTrimmed.split("\\s+");
            if (parts.length < 2) continue;
            String colName = parts[0].replaceAll("[\\\"`\\[\\]]", "").toUpperCase(Locale.ROOT);
            String dataType = parts[1].replaceAll("\\(.*\\)", "").toUpperCase(Locale.ROOT);

            String upperDef = colTrimmed.toUpperCase(Locale.ROOT);
            boolean isPk = upperDef.contains("PRIMARY KEY");
            boolean isAi = upperDef.contains("AUTO_INCREMENT") || upperDef.contains("IDENTITY") || upperDef.contains("SERIAL");
            boolean isNullable = !upperDef.contains("NOT NULL");

            cols.add(new ColumnDefinition(colName, dataType, isPk, isAi, isNullable, null));
        }

        createTable(tableName, cols);
    }

    private int parseAndExecuteInsert(String sql, List<Object> params, List<Long> generatedKeys) throws SQLException {
        Pattern pattern = Pattern.compile("INSERT\\s+INTO\\s+([A-Za-z0-9_]+)\\s*(?:\\(([^)]+)\\))?\\s*VALUES\\s*\\((.+)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new SQLException("Malformed INSERT SQL: " + sql);
        }

        String tableName = matcher.group(1).trim().toUpperCase(Locale.ROOT);
        TableData table = getTable(tableName);
        if (table == null) {
            throw new SQLException("Table not found: " + tableName);
        }

        String colListStr = matcher.group(2);
        List<String> targetCols = new ArrayList<>();
        if (colListStr != null && !colListStr.isBlank()) {
            for (String c : colListStr.split(",")) {
                targetCols.add(c.trim().replaceAll("[\\\"`\\[\\]]", "").toUpperCase(Locale.ROOT));
            }
        } else {
            targetCols.addAll(table.getColumns().keySet());
        }

        RowData newRow = new RowData();
        int paramIdx = 0;

        String pkCol = table.getPrimaryKeyColumn();
        Long generatedId = null;
        if (pkCol != null && table.getColumns().get(pkCol).isAutoIncrement() && !targetCols.contains(pkCol)) {
            generatedId = table.nextAutoIncrement();
            newRow.set(pkCol, generatedId);
            if (generatedKeys != null) {
                generatedKeys.add(generatedId);
            }
        }

        for (String colName : targetCols) {
            Object val = (paramIdx < params.size()) ? params.get(paramIdx++) : null;
            if (colName.equals(pkCol) && val == null && table.getColumns().get(pkCol).isAutoIncrement()) {
                generatedId = table.nextAutoIncrement();
                val = generatedId;
                if (generatedKeys != null) {
                    generatedKeys.add(generatedId);
                }
            } else if (colName.equals(pkCol) && val instanceof Number n && generatedKeys != null) {
                generatedKeys.add(n.longValue());
            }
            newRow.set(colName, normalizeValue(val));
        }

        table.insertRow(newRow);
        persist();
        return 1;
    }

    private int parseAndExecuteUpdate(String sql, List<Object> params) throws SQLException {
        Pattern pattern = Pattern.compile("UPDATE\\s+([A-Za-z0-9_]+)\\s+SET\\s+(.+?)(?:\\s+WHERE\\s+(.+))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new SQLException("Malformed UPDATE SQL: " + sql);
        }

        String tableName = matcher.group(1).trim().toUpperCase(Locale.ROOT);
        TableData table = getTable(tableName);
        if (table == null) {
            throw new SQLException("Table not found: " + tableName);
        }

        String setClause = matcher.group(2).trim();
        String whereClause = matcher.group(3);

        String[] assignments = setClause.split(",");
        List<String> setCols = new ArrayList<>();
        for (String a : assignments) {
            String[] p = a.split("=");
            setCols.add(p[0].trim().replaceAll("[\\\"`\\[\\]]", "").toUpperCase(Locale.ROOT));
        }

        int paramOffset = 0;
        Map<String, Object> updateValues = new LinkedHashMap<>();
        for (String col : setCols) {
            Object val = (paramOffset < params.size()) ? params.get(paramOffset++) : null;
            updateValues.put(col, normalizeValue(val));
        }

        List<Object> whereParams = new ArrayList<>();
        for (int i = paramOffset; i < params.size(); i++) {
            whereParams.add(params.get(i));
        }

        int updated = table.updateMatching(row -> evaluateWhere(whereClause, row, whereParams), updateValues);
        if (updated > 0) {
            persist();
        }
        return updated;
    }

    private int parseAndExecuteDelete(String sql, List<Object> params) throws SQLException {
        Pattern pattern = Pattern.compile("DELETE\\s+FROM\\s+([A-Za-z0-9_]+)(?:\\s+WHERE\\s+(.+))?$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) {
            throw new SQLException("Malformed DELETE SQL: " + sql);
        }

        String tableName = matcher.group(1).trim().toUpperCase(Locale.ROOT);
        TableData table = getTable(tableName);
        if (table == null) {
            throw new SQLException("Table not found: " + tableName);
        }

        String whereClause = matcher.group(2);
        int count = table.deleteMatching(r -> evaluateWhere(whereClause, r, params));
        if (count > 0) {
            persist();
        }
        return count;
    }

    private void parseAndExecuteDrop(String sql) {
        String[] parts = sql.split("\\s+");
        if (parts.length >= 3) {
            String tableName = parts[2].replaceAll("[\\\"`\\[\\];,]", "").toUpperCase(Locale.ROOT);
            tables.remove(tableName);
            persist();
        }
    }

    private QueryResult parseAndExecuteSelect(String sql, List<Object> params) throws SQLException {
        String normalized = sql.replaceAll("\\s+", " ").trim();
        Pattern selectPattern = Pattern.compile("SELECT\\s+(.+?)\\s+FROM\\s+([A-Za-z0-9_]+)(?:\\s+(?:AS\\s+)?(?!WHERE\\b|JOIN\\b|LEFT\\b|RIGHT\\b|INNER\\b|ORDER\\b|GROUP\\b|LIMIT\\b)([A-Za-z0-9_]+))?(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher m = selectPattern.matcher(normalized);
        if (!m.find()) {
            throw new SQLException("Malformed SELECT SQL: " + sql);
        }

        String selectFields = m.group(1).trim();
        String primaryTableName = m.group(2).trim().toUpperCase(Locale.ROOT);
        String remainder = m.group(4) != null ? m.group(4).trim() : "";

        TableData primaryTable = getTable(primaryTableName);
        if (primaryTable == null) {
            throw new SQLException("Table not found: " + primaryTableName);
        }

        List<String[]> joins = new ArrayList<>();
        if (remainder.toUpperCase(Locale.ROOT).contains("JOIN")) {
            Pattern joinPat = Pattern.compile("(?:LEFT\\s+)?JOIN\\s+([A-Za-z0-9_]+)(?:\\s+([A-Za-z0-9_]+))?\\s+ON\\s+([A-Za-z0-9_\\.]+)\\s*=\\s*([A-Za-z0-9_\\.]+)", Pattern.CASE_INSENSITIVE);
            Matcher jm = joinPat.matcher(remainder);
            int lastEnd = 0;
            while (jm.find()) {
                String jTable = jm.group(1).trim().toUpperCase(Locale.ROOT);
                String jLeft = jm.group(3).trim().toUpperCase(Locale.ROOT);
                String jRight = jm.group(4).trim().toUpperCase(Locale.ROOT);
                joins.add(new String[]{jTable, jLeft, jRight});
                lastEnd = jm.end();
            }
            if (lastEnd > 0) {
                remainder = remainder.substring(lastEnd).trim();
            }
        }

        String whereClause = null;
        if (remainder.toUpperCase(Locale.ROOT).contains("WHERE")) {
            int whereIdx = indexOfIgnoreCase(remainder, "WHERE");
            int groupIdx = indexOfIgnoreCase(remainder, "GROUP BY");
            int orderIdx = indexOfIgnoreCase(remainder, "ORDER BY");
            int limitIdx = indexOfIgnoreCase(remainder, "LIMIT");

            int endIdx = remainder.length();
            if (groupIdx > whereIdx) endIdx = Math.min(endIdx, groupIdx);
            if (orderIdx > whereIdx) endIdx = Math.min(endIdx, orderIdx);
            if (limitIdx > whereIdx) endIdx = Math.min(endIdx, limitIdx);

            whereClause = remainder.substring(whereIdx + 5, endIdx).trim();
            remainder = remainder.substring(endIdx).trim();
        }

        String groupByCol = null;
        if (remainder.toUpperCase(Locale.ROOT).contains("GROUP BY")) {
            int groupIdx = indexOfIgnoreCase(remainder, "GROUP BY");
            int orderIdx = indexOfIgnoreCase(remainder, "ORDER BY");
            int limitIdx = indexOfIgnoreCase(remainder, "LIMIT");

            int endIdx = remainder.length();
            if (orderIdx > groupIdx) endIdx = Math.min(endIdx, orderIdx);
            if (limitIdx > groupIdx) endIdx = Math.min(endIdx, limitIdx);

            String groupClause = remainder.substring(groupIdx + 8, endIdx).trim();
            groupByCol = cleanCol(groupClause);
            remainder = remainder.substring(endIdx).trim();
        }

        String orderByCol = null;
        boolean ascending = true;
        if (remainder.toUpperCase(Locale.ROOT).contains("ORDER BY")) {
            int orderIdx = indexOfIgnoreCase(remainder, "ORDER BY");
            int limitIdx = indexOfIgnoreCase(remainder, "LIMIT");
            int endIdx = limitIdx > orderIdx ? limitIdx : remainder.length();

            String orderClause = remainder.substring(orderIdx + 8, endIdx).trim();
            String[] parts = orderClause.split("\\s+");
            orderByCol = parts[0].replaceAll("[\\\"`\\[\\]]", "").toUpperCase(Locale.ROOT);
            if (orderByCol.contains(".")) {
                orderByCol = orderByCol.substring(orderByCol.lastIndexOf('.') + 1);
            }
            if (parts.length > 1 && parts[1].equalsIgnoreCase("DESC")) {
                ascending = false;
            }
            remainder = remainder.substring(endIdx).trim();
        }

        Integer limit = null;
        Integer offset = null;
        if (remainder.toUpperCase(Locale.ROOT).contains("LIMIT")) {
            Pattern limitPat = Pattern.compile("LIMIT\\s+(\\d+)(?:\\s+OFFSET\\s+(\\d+))?", Pattern.CASE_INSENSITIVE);
            Matcher lm = limitPat.matcher(remainder);
            if (lm.find()) {
                limit = Integer.parseInt(lm.group(1));
                if (lm.group(2) != null) {
                    offset = Integer.parseInt(lm.group(2));
                }
            }
        }

        List<RowData> baseRows = primaryTable.getRowsSnapshot();
        List<RowData> combinedRows = new ArrayList<>(baseRows);

        for (String[] join : joins) {
            String jTable = join[0];
            String leftCol = join[1].contains(".") ? join[1].substring(join[1].lastIndexOf('.') + 1) : join[1];
            String rightCol = join[2].contains(".") ? join[2].substring(join[2].lastIndexOf('.') + 1) : join[2];

            TableData secondary = getTable(jTable);
            List<RowData> secRows = secondary != null ? secondary.getRowsSnapshot() : Collections.emptyList();
            List<RowData> nextCombined = new ArrayList<>();

            for (RowData pRow : combinedRows) {
                Object pKey = pRow.get(leftCol);
                boolean matched = false;
                for (RowData sRow : secRows) {
                    Object sKey = sRow.get(rightCol);
                    if (Objects.equals(pKey, sKey)) {
                        RowData merged = pRow.clone();
                        for (Map.Entry<String, Object> e : sRow.getAll().entrySet()) {
                            if (!merged.has(e.getKey())) {
                                merged.set(e.getKey(), e.getValue());
                            }
                        }
                        nextCombined.add(merged);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    nextCombined.add(pRow.clone());
                }
            }
            combinedRows = nextCombined;
        }

        if (!selectFields.equals("*")) {
            for (String colToken : selectFields.split(",")) {
                String token = colToken.trim();
                if (token.toUpperCase(Locale.ROOT).contains(" AS ")) {
                    String[] asParts = token.split("(?i)\\s+AS\\s+");
                    String sourceCol = cleanCol(asParts[0]);
                    String aliasCol = cleanCol(asParts[1]);
                    for (RowData r : combinedRows) {
                        if (r.has(sourceCol)) {
                            r.set(aliasCol, r.get(sourceCol));
                        }
                    }
                }
            }
        }

        List<RowData> filtered = new ArrayList<>();
        for (RowData r : combinedRows) {
            if (evaluateWhere(whereClause, r, params)) {
                filtered.add(r);
            }
        }

        if (groupByCol != null) {
            Map<Object, Integer> groups = new LinkedHashMap<>();
            for (RowData r : filtered) {
                Object gVal = r.get(groupByCol);
                groups.put(gVal, groups.getOrDefault(gVal, 0) + 1);
            }

            QueryResult qr = new QueryResult(List.of(groupByCol, "CNT", "COUNT"));
            for (Map.Entry<Object, Integer> entry : groups.entrySet()) {
                RowData r = new RowData();
                r.set(groupByCol, entry.getKey());
                r.set("CNT", (long) entry.getValue());
                r.set("COUNT", (long) entry.getValue());
                qr.addRow(r);
            }
            return qr;
        }

        if (selectFields.toUpperCase(Locale.ROOT).startsWith("COUNT(")) {
            QueryResult qr = new QueryResult(List.of("COUNT"));
            RowData countRow = new RowData();
            countRow.set("COUNT", (long) filtered.size());
            qr.addRow(countRow);
            return qr;
        }

        if (orderByCol != null) {
            final String sortCol = orderByCol;
            final boolean asc = ascending;
            filtered.sort((a, b) -> {
                Object valA = a.get(sortCol);
                Object valB = b.get(sortCol);
                if (valA == null && valB == null) return 0;
                if (valA == null) return asc ? -1 : 1;
                if (valB == null) return asc ? 1 : -1;

                if (valA instanceof Comparable cA && valB instanceof Comparable cB) {
                    try {
                        int cmp = cA.compareTo(cB);
                        return asc ? cmp : -cmp;
                    } catch (Exception ignored) {
                    }
                }
                int cmp = String.valueOf(valA).compareToIgnoreCase(String.valueOf(valB));
                return asc ? cmp : -cmp;
            });
        }

        int start = offset != null ? Math.min(offset, filtered.size()) : 0;
        int end = limit != null ? Math.min(start + limit, filtered.size()) : filtered.size();
        List<RowData> paged = filtered.subList(start, end);

        List<String> outputCols = new ArrayList<>();
        if (selectFields.equals("*")) {
            for (ColumnDefinition cd : primaryTable.getColumns().values()) {
                outputCols.add(cd.getName());
            }
        } else {
            for (String colToken : selectFields.split(",")) {
                String token = colToken.trim();
                if (token.toUpperCase(Locale.ROOT).contains(" AS ")) {
                    String[] asParts = token.split("(?i)\\s+AS\\s+");
                    outputCols.add(asParts[1].trim().replaceAll("[\\\"`\\[\\]]", "").toUpperCase(Locale.ROOT));
                } else {
                    String c = token.replaceAll("[\\\"`\\[\\]]", "").toUpperCase(Locale.ROOT);
                    if (c.contains(".")) {
                        c = c.substring(c.lastIndexOf('.') + 1);
                    }
                    outputCols.add(c);
                }
            }
        }

        QueryResult result = new QueryResult(outputCols);
        for (RowData row : paged) {
            result.addRow(row);
        }
        return result;
    }

    private boolean evaluateWhere(String whereClause, RowData row, List<Object> params) {
        if (whereClause == null || whereClause.isBlank()) {
            return true;
        }

        String normalized = whereClause.trim();
        String[] andTokens = normalized.split("(?i)\\s+AND\\s+");

        int paramIndex = 0;
        for (String cond : andTokens) {
            String c = cond.trim();
            if (c.startsWith("(") && c.endsWith(")")) {
                c = c.substring(1, c.length() - 1).trim();
            }
            if (c.toUpperCase(Locale.ROOT).contains(" OR ")) {
                String[] orTokens = c.split("(?i)\\s+OR\\s+");
                boolean anyMatched = false;
                int currentParam = paramIndex;
                for (String orCond : orTokens) {
                    if (evaluateSingleCondition(orCond.trim(), row, params, currentParam)) {
                        anyMatched = true;
                    }
                    for (char ch : orCond.toCharArray()) {
                        if (ch == '?') currentParam++;
                    }
                }
                for (char ch : c.toCharArray()) {
                    if (ch == '?') paramIndex++;
                }
                if (!anyMatched) return false;
                continue;
            }

            if (!evaluateSingleCondition(c, row, params, paramIndex)) {
                return false;
            }
            for (char ch : c.toCharArray()) {
                if (ch == '?') paramIndex++;
            }
        }
        return true;
    }

    private boolean evaluateSingleCondition(String c, RowData row, List<Object> params, int paramIndex) {
        if (c.contains("=")) {
            String[] parts = c.split("=");
            String p0 = parts[0].trim();
            String p1 = parts[1].trim();
            if (p0.equalsIgnoreCase(p1)) {
                return true; // Tautology like 1=1
            }
            String col = cleanCol(p0);
            Object targetVal = p1.equals("?") ? (paramIndex < params.size() ? params.get(paramIndex) : null) : p1.replace("'", "");
            Object rowVal = row.get(col);
            return Objects.equals(normalizeValue(rowVal), normalizeValue(targetVal));
        } else if (c.toUpperCase(Locale.ROOT).contains("LIKE")) {
            String[] parts = c.split("(?i)\\s+LIKE\\s+");
            String col = cleanCol(parts[0]);
            String pattern = parts[1].trim().equals("?") ? String.valueOf(paramIndex < params.size() ? params.get(paramIndex) : "") : parts[1].trim().replace("'", "");
            Object rowVal = row.get(col);
            if (rowVal == null) return false;
            String regex = pattern.replace("%", ".*").replace("_", ".");
            return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(String.valueOf(rowVal)).matches();
        } else if (c.toUpperCase(Locale.ROOT).contains("IS NULL")) {
            String col = cleanCol(c.replace("IS NULL", ""));
            return row.get(col) == null;
        } else if (c.toUpperCase(Locale.ROOT).contains("IS NOT NULL")) {
            String col = cleanCol(c.replace("IS NOT NULL", ""));
            return row.get(col) != null;
        }
        return true;
    }

    private String cleanCol(String str) {
        String c = str.trim().replaceAll("[\\\"`\\[\\]()]", "").toUpperCase(Locale.ROOT);
        if (c.contains(".")) {
            return c.substring(c.lastIndexOf('.') + 1);
        }
        return c;
    }

    private int indexOfIgnoreCase(String src, String target) {
        return src.toUpperCase(Locale.ROOT).indexOf(target.toUpperCase(Locale.ROOT));
    }

    private Object normalizeValue(Object val) {
        if (val == null) return null;
        if (val instanceof Timestamp ts) return ts;
        if (val instanceof java.util.Date d) return new Timestamp(d.getTime());
        if (val instanceof Instant inst) return Timestamp.from(inst);
        if (val instanceof Number) return val;
        if (val instanceof Boolean) return val;
        return String.valueOf(val);
    }

    private List<String> splitSqlTokens(String s) {
        List<String> list = new ArrayList<>();
        int level = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '(') level++;
            else if (c == ')') level--;
            if (c == ',' && level == 0) {
                list.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0) {
            list.add(sb.toString().trim());
        }
        return list;
    }
}
