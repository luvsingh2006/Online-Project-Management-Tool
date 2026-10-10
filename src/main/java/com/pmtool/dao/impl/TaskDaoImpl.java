package com.pmtool.dao.impl;

import com.pmtool.dao.AbstractJdbcDao;
import com.pmtool.dao.TaskDao;
import com.pmtool.db.DatabaseManager;
import com.pmtool.exception.DatabaseException;
import com.pmtool.model.Priority;
import com.pmtool.model.Task;
import com.pmtool.model.TaskStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class TaskDaoImpl extends AbstractJdbcDao<Task, Long> implements TaskDao {

    public TaskDaoImpl(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override protected String getTableName() { return "tasks"; }

    @Override
    protected Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getLong("id"));
        t.setCode(rs.getString("code"));
        t.setProjectId(rs.getLong("project_id"));

        Object mid = rs.getObject("milestone_id");
        if (mid instanceof Number n) t.setMilestoneId(n.longValue());

        Object aid = rs.getObject("assignee_id");
        if (aid instanceof Number n) t.setAssigneeId(n.longValue());

        t.setCreatorId(rs.getLong("creator_id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));

        String statusStr = rs.getString("status");
        t.setStatus(statusStr != null ? TaskStatus.valueOf(statusStr) : TaskStatus.TODO);

        String prioStr = rs.getString("priority");
        t.setPriority(prioStr != null ? Priority.valueOf(prioStr) : Priority.MEDIUM);

        t.setEstimatedHours(rs.getDouble("estimated_hours"));
        t.setLoggedHours(rs.getDouble("logged_hours"));
        t.setDueDate(rs.getTimestamp("due_date"));
        t.setCompletedAt(rs.getTimestamp("completed_at"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        t.setUpdatedAt(rs.getTimestamp("updated_at"));

        try { t.setProjectName(rs.getString("project_title")); } catch (SQLException ignored) {}
        try { t.setProjectCode(rs.getString("project_code")); } catch (SQLException ignored) {}
        try { t.setAssigneeName(rs.getString("assignee_name")); } catch (SQLException ignored) {}
        try { t.setCreatorName(rs.getString("creator_name")); } catch (SQLException ignored) {}

        return t;
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO tasks (code, project_id, milestone_id, assignee_id, creator_id, title, description, status, priority, estimated_hours, logged_hours, due_date, completed_at, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Task task) throws SQLException {
        ps.setString(1, task.getCode());
        ps.setLong(2, task.getProjectId());
        ps.setObject(3, task.getMilestoneId());
        ps.setObject(4, task.getAssigneeId());
        ps.setLong(5, task.getCreatorId());
        ps.setString(6, task.getTitle());
        ps.setString(7, task.getDescription());
        ps.setString(8, task.getStatus().name());
        ps.setString(9, task.getPriority().name());
        ps.setObject(10, task.getEstimatedHours());
        ps.setObject(11, task.getLoggedHours());
        ps.setTimestamp(12, task.getDueDate());
        ps.setTimestamp(13, task.getCompletedAt());
        ps.setTimestamp(14, task.getCreatedAt());
        ps.setTimestamp(15, task.getUpdatedAt());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE tasks SET project_id = ?, milestone_id = ?, assignee_id = ?, title = ?, description = ?, status = ?, priority = ?, estimated_hours = ?, logged_hours = ?, due_date = ?, completed_at = ?, updated_at = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Task task) throws SQLException {
        task.touch();
        ps.setLong(1, task.getProjectId());
        ps.setObject(2, task.getMilestoneId());
        ps.setObject(3, task.getAssigneeId());
        ps.setString(4, task.getTitle());
        ps.setString(5, task.getDescription());
        ps.setString(6, task.getStatus().name());
        ps.setString(7, task.getPriority().name());
        ps.setObject(8, task.getEstimatedHours());
        ps.setObject(9, task.getLoggedHours());
        ps.setTimestamp(10, task.getDueDate());
        ps.setTimestamp(11, task.getCompletedAt());
        ps.setTimestamp(12, task.getUpdatedAt());
        ps.setLong(13, task.getId());
    }

    private String getBaseSelectQuery() {
        return "SELECT t.*, p.title as project_title, p.code as project_code, u.full_name as assignee_name, c.full_name as creator_name " +
                "FROM tasks t " +
                "LEFT JOIN projects p ON t.project_id = p.id " +
                "LEFT JOIN users u ON t.assignee_id = u.id " +
                "LEFT JOIN users c ON t.creator_id = c.id ";
    }

    @Override
    public Optional<Task> findById(Long id) {
        String sql = getBaseSelectQuery() + "WHERE t.id = ?";
        return queryForObject(sql, id);
    }

    @Override
    public Optional<Task> findByCode(String code) {
        String sql = getBaseSelectQuery() + "WHERE t.code = ?";
        return queryForObject(sql, code);
    }

    @Override
    public List<Task> findByProjectId(Long projectId) {
        String sql = getBaseSelectQuery() + "WHERE t.project_id = ? ORDER BY t.id DESC";
        return queryForList(sql, projectId);
    }

    @Override
    public List<Task> findByAssigneeId(Long assigneeId) {
        String sql = getBaseSelectQuery() + "WHERE t.assignee_id = ? ORDER BY t.due_date ASC";
        return queryForList(sql, assigneeId);
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        String sql = getBaseSelectQuery() + "WHERE t.status = ? ORDER BY t.id DESC";
        return queryForList(sql, status.name());
    }

    @Override
    public List<Task> filterTasks(Long projectId, Long assigneeId, TaskStatus status, Priority priority, String search) {
        StringBuilder sql = new StringBuilder(getBaseSelectQuery()).append("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (projectId != null) {
            sql.append(" AND t.project_id = ?");
            params.add(projectId);
        }
        if (assigneeId != null) {
            sql.append(" AND t.assignee_id = ?");
            params.add(assigneeId);
        }
        if (status != null) {
            sql.append(" AND t.status = ?");
            params.add(status.name());
        }
        if (priority != null) {
            sql.append(" AND t.priority = ?");
            params.add(priority.name());
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (t.title LIKE ? OR t.code LIKE ?)");
            String p = "%" + search.trim() + "%";
            params.add(p);
            params.add(p);
        }

        sql.append(" ORDER BY t.id DESC");
        return queryForList(sql.toString(), params.toArray());
    }

    @Override
    public boolean updateStatus(Long taskId, TaskStatus status) {
        Timestamp completedAt = (status == TaskStatus.COMPLETED) ? new Timestamp(System.currentTimeMillis()) : null;
        String sql = "UPDATE tasks SET status = ?, completed_at = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, completedAt);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setLong(4, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed updating task status: " + taskId, e);
            throw new DatabaseException("Database error updating status", e);
        }
    }

    @Override
    public boolean updateLoggedHours(Long taskId, double addedHours) {
        Optional<Task> opt = findById(taskId);
        if (opt.isEmpty()) return false;
        double newTotal = (opt.get().getLoggedHours() != null ? opt.get().getLoggedHours() : 0.0) + addedHours;

        String sql = "UPDATE tasks SET logged_hours = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newTotal);
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setLong(3, taskId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed updating logged hours: " + taskId, e);
            throw new DatabaseException("Database error updating task hours", e);
        }
    }
}
