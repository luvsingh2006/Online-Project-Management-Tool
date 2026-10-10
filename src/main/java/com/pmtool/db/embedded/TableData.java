package com.pmtool.db.embedded;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TableData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Map<String, ColumnDefinition> columns = new LinkedHashMap<>();
    private final List<RowData> rows = new ArrayList<>();
    private final Map<Object, RowData> primaryKeyIndex = new HashMap<>();
    private final AtomicLong autoIncrementSeq = new AtomicLong(1);
    private String primaryKeyColumn = null;

    private transient ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public TableData(String name) {
        this.name = name.toUpperCase(Locale.ROOT);
    }

    private void ensureLock() {
        if (rwLock == null) {
            rwLock = new ReentrantReadWriteLock();
        }
    }

    public String getName() { return name; }

    public void addColumn(ColumnDefinition col) {
        ensureLock();
        rwLock.writeLock().lock();
        try {
            columns.put(col.getName(), col);
            if (col.isPrimaryKey()) {
                this.primaryKeyColumn = col.getName();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Map<String, ColumnDefinition> getColumns() {
        ensureLock();
        rwLock.readLock().lock();
        try {
            return Collections.unmodifiableMap(new LinkedHashMap<>(columns));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public String getPrimaryKeyColumn() { return primaryKeyColumn; }

    public long nextAutoIncrement() { return autoIncrementSeq.getAndIncrement(); }

    public void setAutoIncrementFloor(long val) {
        autoIncrementSeq.updateAndGet(cur -> Math.max(cur, val + 1));
    }

    public void insertRow(RowData row) {
        ensureLock();
        rwLock.writeLock().lock();
        try {
            if (primaryKeyColumn != null) {
                Object pkVal = row.get(primaryKeyColumn);
                if (pkVal != null) {
                    primaryKeyIndex.put(pkVal, row);
                    if (pkVal instanceof Number n) {
                        setAutoIncrementFloor(n.longValue());
                    }
                }
            }
            rows.add(row);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public List<RowData> getRowsSnapshot() {
        ensureLock();
        rwLock.readLock().lock();
        try {
            List<RowData> copy = new ArrayList<>(rows.size());
            for (RowData r : rows) {
                copy.add(r.clone());
            }
            return copy;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int updateMatching(java.util.function.Predicate<RowData> matcher, Map<String, Object> updateValues) {
        ensureLock();
        rwLock.writeLock().lock();
        try {
            int updated = 0;
            for (RowData r : rows) {
                if (matcher.test(r)) {
                    for (Map.Entry<String, Object> entry : updateValues.entrySet()) {
                        r.set(entry.getKey(), entry.getValue());
                    }
                    if (primaryKeyColumn != null && updateValues.containsKey(primaryKeyColumn)) {
                        primaryKeyIndex.put(r.get(primaryKeyColumn), r);
                    }
                    updated++;
                }
            }
            return updated;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public int deleteMatching(java.util.function.Predicate<RowData> matcher) {
        ensureLock();
        rwLock.writeLock().lock();
        try {
            int initialSize = rows.size();
            Iterator<RowData> it = rows.iterator();
            while (it.hasNext()) {
                RowData r = it.next();
                if (matcher.test(r)) {
                    if (primaryKeyColumn != null) {
                        primaryKeyIndex.remove(r.get(primaryKeyColumn));
                    }
                    it.remove();
                }
            }
            return initialSize - rows.size();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public int deleteRows(List<RowData> targets) {
        ensureLock();
        rwLock.writeLock().lock();
        try {
            int count = 0;
            for (RowData target : targets) {
                if (rows.remove(target)) {
                    count++;
                    if (primaryKeyColumn != null) {
                        Object pk = target.get(primaryKeyColumn);
                        if (pk != null) {
                            primaryKeyIndex.remove(pk);
                        }
                    }
                }
            }
            return count;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void truncate() {
        ensureLock();
        rwLock.writeLock().lock();
        try {
            rows.clear();
            primaryKeyIndex.clear();
            autoIncrementSeq.set(1);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
