package com.pmtool.dao.impl;

import com.pmtool.dao.AbstractJdbcDao;
import com.pmtool.dao.AuditLogDao;
import com.pmtool.db.DatabaseManager;
import com.pmtool.model.AuditLog;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AuditLogDaoImpl extends AbstractJdbcDao<AuditLog, Long> implements AuditLogDao {

    public AuditLogDaoImpl(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override protected String getTableName() { return "audit_logs"; }

    @Override
    protected AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("id"));
        Object uid = rs.getObject("user_id");
        if (uid instanceof Number n) log.setUserId(n.longValue());
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        log.setEntityId(rs.getString("entity_id"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setCreatedAt(rs.getTimestamp("created_at"));
        try { log.setUserName(rs.getString("user_name")); } catch (SQLException ignored) {}
        return log;
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, details, ip_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, AuditLog log) throws SQLException {
        ps.setObject(1, log.getUserId());
        ps.setString(2, log.getAction());
        ps.setString(3, log.getEntityType());
        ps.setString(4, log.getEntityId());
        ps.setString(5, log.getDetails());
        ps.setString(6, log.getIpAddress());
        ps.setTimestamp(7, log.getCreatedAt());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE audit_logs SET details = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, AuditLog log) throws SQLException {
        ps.setString(1, log.getDetails());
        ps.setLong(2, log.getId());
    }

    @Override
    public List<AuditLog> findRecentLogs(int limit) {
        String sql = "SELECT a.*, u.full_name as user_name FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id ORDER BY a.id DESC LIMIT " + Math.max(1, limit);
        return queryForList(sql);
    }

    @Override
    public List<AuditLog> findByEntityType(String entityType, String entityId) {
        String sql = "SELECT a.*, u.full_name as user_name FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id WHERE a.entity_type = ? AND a.entity_id = ? ORDER BY a.id DESC";
        return queryForList(sql, entityType, entityId);
    }

    @Override
    public List<AuditLog> findByUserId(Long userId) {
        String sql = "SELECT a.*, u.full_name as user_name FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id WHERE a.user_id = ? ORDER BY a.id DESC";
        return queryForList(sql, userId);
    }
}
