package com.pmtool.dao.impl;

import com.pmtool.dao.AbstractJdbcDao;
import com.pmtool.dao.TaskTimeLogDao;
import com.pmtool.db.DatabaseManager;
import com.pmtool.model.TaskTimeLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TaskTimeLogDaoImpl extends AbstractJdbcDao<TaskTimeLog, Long> implements TaskTimeLogDao {

    public TaskTimeLogDaoImpl(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override protected String getTableName() { return "task_time_logs"; }

    @Override
    protected TaskTimeLog mapRow(ResultSet rs) throws SQLException {
        TaskTimeLog log = new TaskTimeLog();
        log.setId(rs.getLong("id"));
        log.setTaskId(rs.getLong("task_id"));
        log.setUserId(rs.getLong("user_id"));
        log.setHoursSpent(rs.getDouble("hours_spent"));
        log.setLogDate(rs.getTimestamp("log_date"));
        log.setNote(rs.getString("note"));
        log.setCreatedAt(rs.getTimestamp("created_at"));
        try { log.setUserName(rs.getString("user_name")); } catch (SQLException ignored) {}
        return log;
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO task_time_logs (task_id, user_id, hours_spent, log_date, note, created_at) VALUES (?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, TaskTimeLog log) throws SQLException {
        ps.setLong(1, log.getTaskId());
        ps.setLong(2, log.getUserId());
        ps.setDouble(3, log.getHoursSpent());
        ps.setTimestamp(4, log.getLogDate());
        ps.setString(5, log.getNote());
        ps.setTimestamp(6, log.getCreatedAt());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE task_time_logs SET hours_spent = ?, log_date = ?, note = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, TaskTimeLog log) throws SQLException {
        ps.setDouble(1, log.getHoursSpent());
        ps.setTimestamp(2, log.getLogDate());
        ps.setString(3, log.getNote());
        ps.setLong(4, log.getId());
    }

    @Override
    public List<TaskTimeLog> findByTaskId(Long taskId) {
        String sql = "SELECT l.*, u.full_name as user_name FROM task_time_logs l LEFT JOIN users u ON l.user_id = u.id WHERE l.task_id = ? ORDER BY l.log_date DESC";
        return queryForList(sql, taskId);
    }

    @Override
    public List<TaskTimeLog> findByUserId(Long userId) {
        String sql = "SELECT l.*, u.full_name as user_name FROM task_time_logs l LEFT JOIN users u ON l.user_id = u.id WHERE l.user_id = ? ORDER BY l.log_date DESC";
        return queryForList(sql, userId);
    }

    @Override
    public double getTotalHoursForTask(Long taskId) {
        String sql = "SELECT SUM(hours_spent) FROM task_time_logs WHERE task_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException ignored) {}
        return 0.0;
    }
}
