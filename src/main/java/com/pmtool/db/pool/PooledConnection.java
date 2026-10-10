package com.pmtool.db.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PooledConnection implements InvocationHandler {

    private final Connection physicalConnection;
    private final ConnectionPool pool;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final long borrowedTimestamp;

    private PooledConnection(Connection physicalConnection, ConnectionPool pool) {
        this.physicalConnection = physicalConnection;
        this.pool = pool;
        this.borrowedTimestamp = System.currentTimeMillis();
    }

    public static Connection wrap(Connection physicalConnection, ConnectionPool pool) {
        PooledConnection handler = new PooledConnection(physicalConnection, pool);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if ("close".equals(methodName)) {
            if (isClosed.compareAndSet(false, true)) {
                try {
                    if (!physicalConnection.getAutoCommit()) {
                        physicalConnection.setAutoCommit(true);
                    }
                } catch (SQLException ignored) {
                }
                pool.releaseConnection(physicalConnection);
            }
            return null;
        }

        if ("isClosed".equals(methodName)) {
            return isClosed.get() || physicalConnection.isClosed();
        }

        if (isClosed.get()) {
            throw new SQLException("Connection has already been closed and recycled to the pool.");
        }

        try {
            return method.invoke(physicalConnection, args);
        } catch (Throwable t) {
            if (t instanceof java.lang.reflect.InvocationTargetException ite) {
                throw ite.getTargetException();
            }
            throw t;
        }
    }

    public long getBorrowedTimestamp() {
        return borrowedTimestamp;
    }
}
