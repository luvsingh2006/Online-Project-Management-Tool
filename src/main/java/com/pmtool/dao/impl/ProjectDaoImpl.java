package com.pmtool.dao.impl;

import com.pmtool.dao.AbstractJdbcDao;
import com.pmtool.dao.ProjectDao;
import com.pmtool.db.DatabaseManager;
import com.pmtool.exception.DatabaseException;
import com.pmtool.model.Priority;
import com.pmtool.model.Project;
import com.pmtool.model.ProjectStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class ProjectDaoImpl extends AbstractJdbcDao<Project, Long> implements ProjectDao {

    public ProjectDaoImpl(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override protected String getTableName() { return "projects"; }

    @Override
    protected Project mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getLong("id"));
        p.setCode(rs.getString("code"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setManagerId(rs.getLong("manager_id"));

        try { p.setManagerName(rs.getString("manager_name")); } catch (SQLException ignored) {}

        String statusStr = rs.getString("status");
        p.setStatus(statusStr != null ? ProjectStatus.valueOf(statusStr) : ProjectStatus.PLANNING);
        String prioStr = rs.getString("priority");
        p.setPriority(prioStr != null ? Priority.valueOf(prioStr) : Priority.MEDIUM);
        p.setStartDate(rs.getTimestamp("start_date"));
        p.setDueDate(rs.getTimestamp("due_date"));
        p.setBudget(rs.getDouble("budget"));
        p.setProgressPercent(rs.getInt("progress_percent"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));

        populateTaskAggregates(p);
        return p;
    }

    private void populateTaskAggregates(Project project) {
        if (project.getId() == null) return;
        String sql = "SELECT status, COUNT(*) as cnt FROM tasks WHERE project_id = ? GROUP BY status";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, project.getId());
            int total = 0;
            int completed = 0;
            int inProgress = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String st = rs.getString("status");
                    int count = rs.getInt("cnt");
                    total += count;
                    if ("COMPLETED".equalsIgnoreCase(st)) {
                        completed += count;
                    } else if ("IN_PROGRESS".equalsIgnoreCase(st) || "IN_REVIEW".equalsIgnoreCase(st)) {
                        inProgress += count;
                    }
                }
            }
            project.setTotalTasks(total);
            project.setCompletedTasks(completed);
            project.setInProgressTasks(inProgress);
            if (total > 0) {
                project.recalculateProgress();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO projects (code, title, description, manager_id, status, priority, start_date, due_date, budget, progress_percent, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Project project) throws SQLException {
        ps.setString(1, project.getCode());
        ps.setString(2, project.getTitle());
        ps.setString(3, project.getDescription());
        ps.setLong(4, project.getManagerId());
        ps.setString(5, project.getStatus().name());
        ps.setString(6, project.getPriority().name());
        ps.setTimestamp(7, project.getStartDate());
        ps.setTimestamp(8, project.getDueDate());
        ps.setObject(9, project.getBudget());
        ps.setInt(10, project.getProgressPercent());
        ps.setTimestamp(11, project.getCreatedAt());
        ps.setTimestamp(12, project.getUpdatedAt());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE projects SET title = ?, description = ?, manager_id = ?, status = ?, priority = ?, start_date = ?, due_date = ?, budget = ?, progress_percent = ?, updated_at = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Project project) throws SQLException {
        project.touch();
        ps.setString(1, project.getTitle());
        ps.setString(2, project.getDescription());
        ps.setLong(3, project.getManagerId());
        ps.setString(4, project.getStatus().name());
        ps.setString(5, project.getPriority().name());
        ps.setTimestamp(6, project.getStartDate());
        ps.setTimestamp(7, project.getDueDate());
        ps.setObject(8, project.getBudget());
        ps.setInt(9, project.getProgressPercent());
        ps.setTimestamp(10, project.getUpdatedAt());
        ps.setLong(11, project.getId());
    }

    @Override
    public Optional<Project> findByCode(String code) {
        String sql = "SELECT p.*, u.full_name as manager_name FROM projects p LEFT JOIN users u ON p.manager_id = u.id WHERE p.code = ?";
        return queryForObject(sql, code);
    }

    @Override
    public List<Project> findByManagerId(Long managerId) {
        String sql = "SELECT p.*, u.full_name as manager_name FROM projects p LEFT JOIN users u ON p.manager_id = u.id WHERE p.manager_id = ? ORDER BY p.id DESC";
        return queryForList(sql, managerId);
    }

    @Override
    public List<Project> findByStatus(ProjectStatus status) {
        String sql = "SELECT p.*, u.full_name as manager_name FROM projects p LEFT JOIN users u ON p.manager_id = u.id WHERE p.status = ? ORDER BY p.id DESC";
        return queryForList(sql, status.name());
    }

    @Override
    public List<Project> searchAndFilter(String query, ProjectStatus status, Priority priority) {
        StringBuilder sql = new StringBuilder("SELECT p.*, u.full_name as manager_name FROM projects p LEFT JOIN users u ON p.manager_id = u.id WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            sql.append(" AND (p.title LIKE ? OR p.code LIKE ?)");
            String p = "%" + query.trim() + "%";
            params.add(p);
            params.add(p);
        }
        if (status != null) {
            sql.append(" AND p.status = ?");
            params.add(status.name());
        }
        if (priority != null) {
            sql.append(" AND p.priority = ?");
            params.add(priority.name());
        }
        sql.append(" ORDER BY p.id DESC");
        return queryForList(sql.toString(), params.toArray());
    }

    @Override
    public boolean updateProgress(Long projectId, int progressPercent) {
        String sql = "UPDATE projects SET progress_percent = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, progressPercent);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setLong(3, projectId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed updating project progress: " + projectId, e);
            throw new DatabaseException("Database error updating progress", e);
        }
    }

    @Override
    public void syncAggregatedTaskCounts(Long projectId) {
        Optional<Project> opt = findById(projectId);
        if (opt.isPresent()) {
            Project p = opt.get();
            populateTaskAggregates(p);
            updateProgress(projectId, p.getProgressPercent());
        }
    }
}
