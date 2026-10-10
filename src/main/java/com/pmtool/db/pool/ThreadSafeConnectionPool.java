package com.pmtool.db.pool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadSafeConnectionPool implements ConnectionPool {

    private static final Logger LOGGER = Logger.getLogger(ThreadSafeConnectionPool.class.getName());

    private final String url;
    private final String username;
    private final String password;
    private final int minSize;
    private final int maxSize;
    private final long maxWaitMillis;

    private final BlockingQueue<Connection> idlePool;
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private final AtomicInteger activeLeased = new AtomicInteger(0);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public ThreadSafeConnectionPool(String url, String username, String password,
                                    int minSize, int maxSize, long maxWaitMillis) throws SQLException {
        this.url = url;
        this.username = username;
        this.password = password;
        this.minSize = Math.max(1, minSize);
        this.maxSize = Math.max(this.minSize, maxSize);
        this.maxWaitMillis = maxWaitMillis > 0 ? maxWaitMillis : 5000L;
        this.idlePool = new ArrayBlockingQueue<>(this.maxSize);

        for (int i = 0; i < this.minSize; i++) {
            Connection physical = createPhysicalConnection();
            idlePool.offer(physical);
        }
        LOGGER.info(String.format("Initialized ConnectionPool with %d initial connections (max: %d)", totalCreated.get(), this.maxSize));
    }

    private Connection createPhysicalConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(url, username, password);
        totalCreated.incrementAndGet();
        return conn;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (isShutdown.get()) {
            throw new SQLException("Connection pool is terminated.");
        }

        long deadline = System.currentTimeMillis() + maxWaitMillis;
        Connection conn = idlePool.poll();

        if (conn == null && totalCreated.get() < maxSize) {
            synchronized (this) {
                if (totalCreated.get() < maxSize) {
                    conn = createPhysicalConnection();
                }
            }
        }

        if (conn == null) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining > 0) {
                try {
                    conn = idlePool.poll(remaining, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Thread interrupted while awaiting database connection", e);
                }
            }
        }

        if (conn == null) {
            throw new SQLException(String.format(
                    "Connection pool exhausted (active: %d, idle: %d, total: %d). Timeout: %d ms",
                    activeLeased.get(), idlePool.size(), totalCreated.get(), maxWaitMillis));
        }

        if (conn.isClosed()) {
            totalCreated.decrementAndGet();
            return getConnection();
        }

        activeLeased.incrementAndGet();
        return PooledConnection.wrap(conn, this);
    }

    @Override
    public void releaseConnection(Connection physicalConnection) {
        if (physicalConnection == null) {
            return;
        }

        activeLeased.decrementAndGet();

        if (isShutdown.get()) {
            closeSilently(physicalConnection);
            totalCreated.decrementAndGet();
            return;
        }

        try {
            if (physicalConnection.isClosed()) {
                totalCreated.decrementAndGet();
                return;
            }
            if (!idlePool.offer(physicalConnection)) {
                closeSilently(physicalConnection);
                totalCreated.decrementAndGet();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error returning connection to pool: " + e.getMessage(), e);
            closeSilently(physicalConnection);
            totalCreated.decrementAndGet();
        }
    }

    @Override
    public int getActiveConnections() {
        return activeLeased.get();
    }

    @Override
    public int getIdleConnections() {
        return idlePool.size();
    }

    @Override
    public synchronized void close() {
        if (isShutdown.compareAndSet(false, true)) {
            LOGGER.info("Shutting down database connection pool...");
            Connection conn;
            while ((conn = idlePool.poll()) != null) {
                closeSilently(conn);
                totalCreated.decrementAndGet();
            }
            LOGGER.info("Database connection pool cleanly closed.");
        }
    }

    private void closeSilently(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
