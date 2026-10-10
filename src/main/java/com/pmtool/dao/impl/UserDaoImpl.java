package com.pmtool.dao.impl;

import com.pmtool.dao.AbstractJdbcDao;
import com.pmtool.dao.UserDao;
import com.pmtool.db.DatabaseManager;
import com.pmtool.exception.DatabaseException;
import com.pmtool.model.Role;
import com.pmtool.model.User;
import com.pmtool.model.UserStatus;

import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public class UserDaoImpl extends AbstractJdbcDao<User, Long> implements UserDao {

    public UserDaoImpl(DatabaseManager databaseManager) {
        super(databaseManager);
    }

    @Override protected String getTableName() { return "users"; }

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setFullName(rs.getString("full_name"));
        String roleStr = rs.getString("role");
        user.setRole(roleStr != null ? Role.valueOf(roleStr) : Role.TEAM_MEMBER);
        user.setDepartment(rs.getString("department"));
        user.setJobTitle(rs.getString("job_title"));
        String statusStr = rs.getString("status");
        user.setStatus(statusStr != null ? UserStatus.valueOf(statusStr) : UserStatus.ACTIVE);
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        return user;
    }

    @Override
    protected String getInsertSql() {
        return "INSERT INTO users (email, password_hash, salt, full_name, role, department, job_title, status, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void setInsertParameters(PreparedStatement ps, User user) throws SQLException {
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getPasswordHash());
        ps.setString(3, user.getSalt());
        ps.setString(4, user.getFullName());
        ps.setString(5, user.getRole().name());
        ps.setString(6, user.getDepartment());
        ps.setString(7, user.getJobTitle());
        ps.setString(8, user.getStatus().name());
        ps.setTimestamp(9, user.getCreatedAt());
        ps.setTimestamp(10, user.getUpdatedAt());
    }

    @Override
    protected String getUpdateSql() {
        return "UPDATE users SET email = ?, full_name = ?, role = ?, department = ?, job_title = ?, status = ?, updated_at = ? WHERE id = ?";
    }

    @Override
    protected void setUpdateParameters(PreparedStatement ps, User user) throws SQLException {
        user.touch();
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getFullName());
        ps.setString(3, user.getRole().name());
        ps.setString(4, user.getDepartment());
        ps.setString(5, user.getJobTitle());
        ps.setString(6, user.getStatus().name());
        ps.setTimestamp(7, user.getUpdatedAt());
        ps.setLong(8, user.getId());
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return queryForObject(sql, email);
    }

    @Override
    public List<User> findByRole(Role role) {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name ASC";
        return queryForList(sql, role.name());
    }

    @Override
    public List<User> findActiveUsers() {
        String sql = "SELECT * FROM users WHERE status = ? ORDER BY full_name ASC";
        return queryForList(sql, UserStatus.ACTIVE.name());
    }

    @Override
    public boolean updatePassword(Long userId, String passwordHash, String salt) {
        String sql = "UPDATE users SET password_hash = ?, salt = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setString(2, salt);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            ps.setLong(4, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed updating password: " + userId, e);
            throw new DatabaseException("Database error updating password", e);
        }
    }

    @Override
    public boolean updateStatus(Long userId, UserStatus status) {
        String sql = "UPDATE users SET status = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed updating user status: " + userId, e);
            throw new DatabaseException("Database error updating status", e);
        }
    }

    @Override
    public List<User> searchUsers(String query) {
        String pattern = "%" + query + "%";
        String sql = "SELECT * FROM users WHERE full_name LIKE ? OR email LIKE ? ORDER BY full_name ASC";
        return queryForList(sql, pattern, pattern);
    }
}
