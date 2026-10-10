package com.pmtool.dao;

import com.pmtool.db.DatabaseManager;
import com.pmtool.exception.DatabaseException;
import com.pmtool.model.Identifiable;

import java.io.Serializable;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractJdbcDao<T extends Identifiable<ID>, ID extends Serializable> implements GenericDao<T, ID> {

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected final DatabaseManager databaseManager;

    public AbstractJdbcDao(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    protected abstract String getTableName();
    protected abstract T mapRow(ResultSet rs) throws SQLException;
    protected abstract String getInsertSql();
    protected abstract void setInsertParameters(PreparedStatement ps, T entity) throws SQLException;
    protected abstract String getUpdateSql();
    protected abstract void setUpdateParameters(PreparedStatement ps, T entity) throws SQLException;

    protected Connection getConnection() throws SQLException {
        return databaseManager.getConnection();
    }

    @Override
    public Optional<T> findById(ID id) {
        String sql = "SELECT * FROM " + getTableName() + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed finding entity by id: " + id, e);
            throw new DatabaseException("Database error finding " + getTableName() + " by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<T> findAll() {
        String sql = "SELECT * FROM " + getTableName() + " ORDER BY id ASC";
        return queryForList(sql);
    }

    @Override
    public T save(T entity) {
        String sql = getInsertSql();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setInsertParameters(ps, entity);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Object generatedId = rs.getObject(1);
                    if (generatedId instanceof Number n) {
                        @SuppressWarnings("unchecked")
                        ID typedId = (ID) Long.valueOf(n.longValue());
                        entity.setId(typedId);
                    }
                }
            }
            return entity;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed saving entity into " + getTableName(), e);
            throw new DatabaseException("Database error saving " + getTableName(), e);
        }
    }

    @Override
    public boolean update(T entity) {
        String sql = getUpdateSql();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setUpdateParameters(ps, entity);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed updating entity in " + getTableName(), e);
            throw new DatabaseException("Database error updating " + getTableName(), e);
        }
    }

    @Override
    public boolean deleteById(ID id) {
        String sql = "DELETE FROM " + getTableName() + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed deleting entity from " + getTableName() + " by id: " + id, e);
            throw new DatabaseException("Database error deleting from " + getTableName(), e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + getTableName();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed counting rows in " + getTableName(), e);
            throw new DatabaseException("Database error counting " + getTableName(), e);
        }
    }

    protected List<T> queryForList(String sql, Object... params) {
        List<T> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Query for list failed: " + sql, e);
            throw new DatabaseException("Database query error on " + getTableName(), e);
        }
        return list;
    }

    protected Optional<T> queryForObject(String sql, Object... params) {
        List<T> list = queryForList(sql, params);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
