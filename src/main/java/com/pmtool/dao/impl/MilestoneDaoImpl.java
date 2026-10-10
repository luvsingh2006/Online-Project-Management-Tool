package com.pmtool.dao.impl;

import com.pmtool.dao.AbstractJdbcDao;
import com.pmtool.dao.MilestoneDao;
import com.pmtool.db.DatabaseManager;
import com.pmtool.exception.DatabaseException;
import com.pmtool.model.Milestone;

import java.sql.*;
import java.util.List;
import java.util.logging.Level;

public class MilestoneDaoImpl extends AbstractJdbcDao<Milestone, Long> implements MilestoneDao {

    public MilestoneDaoImpl(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override protected String getTableName() { return "milestones"; }

    @Override
    protected Milestone mapRow(ResultSet rs) throws SQLException {
        Milestone m = new Milestone();
        m.setId(rs.getLong("id"));
        m.setProjectId(rs.getLong("project_id"));
        m.setTitle(rs.getString("title"));
        m.setDueDate(rs.getTimestamp("due_date"));
        m.setCompleted(rs.getBoolean("completed"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        return m;
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO milestones (project_id, title, due_date, completed, created_at) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Milestone m) throws SQLException {
        ps.setLong(1, m.getProjectId());
        ps.setString(2, m.getTitle());
        ps.setTimestamp(3, m.getDueDate());
        ps.setBoolean(4, m.isCompleted());
        ps.setTimestamp(5, m.getCreatedAt());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE milestones SET title = ?, due_date = ?, completed = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Milestone m) throws SQLException {
        ps.setString(1, m.getTitle());
        ps.setTimestamp(2, m.getDueDate());
        ps.setBoolean(3, m.isCompleted());
        ps.setLong(4, m.getId());
    }

    @Override
    public List<Milestone> findByProjectId(Long projectId) {
        String sql = "SELECT * FROM milestones WHERE project_id = ? ORDER BY due_date ASC";
        return queryForList(sql, projectId);
    }

    @Override
    public boolean markCompleted(Long milestoneId, boolean completed) {
        String sql = "UPDATE milestones SET completed = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, completed);
            ps.setLong(2, milestoneId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed marking milestone completed: " + milestoneId, e);
            throw new DatabaseException("Database error updating milestone", e);
        }
    }
}
