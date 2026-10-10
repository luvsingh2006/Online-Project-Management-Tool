package com.pmtool.dao.impl;

import com.pmtool.dao.AbstractJdbcDao;
import com.pmtool.dao.CommentDao;
import com.pmtool.db.DatabaseManager;
import com.pmtool.model.Comment;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CommentDaoImpl extends AbstractJdbcDao<Comment, Long> implements CommentDao {

    public CommentDaoImpl(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override protected String getTableName() { return "comments"; }

    @Override
    protected Comment mapRow(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getLong("id"));
        c.setTaskId(rs.getLong("task_id"));
        c.setUserId(rs.getLong("user_id"));
        c.setContent(rs.getString("content"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        try { c.setUserName(rs.getString("user_name")); } catch (SQLException ignored) {}
        return c;
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO comments (task_id, user_id, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, Comment c) throws SQLException {
        ps.setLong(1, c.getTaskId());
        ps.setLong(2, c.getUserId());
        ps.setString(3, c.getContent());
        ps.setTimestamp(4, c.getCreatedAt());
        ps.setTimestamp(5, c.getUpdatedAt());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE comments SET content = ?, updated_at = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, Comment c) throws SQLException {
        c.touch();
        ps.setString(1, c.getContent());
        ps.setTimestamp(2, c.getUpdatedAt());
        ps.setLong(3, c.getId());
    }

    @Override
    public List<Comment> findByTaskId(Long taskId) {
        String sql = "SELECT c.*, u.full_name as user_name FROM comments c LEFT JOIN users u ON c.user_id = u.id WHERE c.task_id = ? ORDER BY c.created_at ASC";
        return queryForList(sql, taskId);
    }
}
