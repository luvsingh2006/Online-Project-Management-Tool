package com.pmtool.dao.impl;

import com.pmtool.dao.SystemSettingDao;
import com.pmtool.db.DatabaseManager;
import com.pmtool.exception.DatabaseException;
import com.pmtool.model.SystemSetting;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SystemSettingDaoImpl implements SystemSettingDao {

    private static final Logger LOGGER = Logger.getLogger(SystemSettingDaoImpl.class.getName());
    private final DatabaseManager databaseManager;

    public SystemSettingDaoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    private Connection getConnection() throws SQLException {
        return databaseManager.getConnection();
    }

    @Override
    public Optional<SystemSetting> findByKey(String key) {
        String sql = "SELECT * FROM system_settings WHERE setting_key = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    SystemSetting s = new SystemSetting();
                    s.setSettingKey(rs.getString("setting_key"));
                    s.setSettingValue(rs.getString("setting_value"));
                    s.setDescription(rs.getString("description"));
                    s.setUpdatedAt(rs.getTimestamp("updated_at"));
                    return Optional.of(s);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed finding setting: " + key, e);
            throw new DatabaseException("Database error finding setting: " + key, e);
        }
        return Optional.empty();
    }

    @Override
    public List<SystemSetting> findAll() {
        List<SystemSetting> list = new ArrayList<>();
        String sql = "SELECT * FROM system_settings ORDER BY setting_key ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SystemSetting s = new SystemSetting();
                s.setSettingKey(rs.getString("setting_key"));
                s.setSettingValue(rs.getString("setting_value"));
                s.setDescription(rs.getString("description"));
                s.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(s);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed listing settings", e);
            throw new DatabaseException("Database error listing settings", e);
        }
        return list;
    }

    @Override
    public void setSetting(String key, String value, String description) {
        String updateSql = "UPDATE system_settings SET setting_value = ?, description = ?, updated_at = ? WHERE setting_key = ?";
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setString(1, value);
                ps.setString(2, description);
                ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                ps.setString(4, key);
                if (ps.executeUpdate() > 0) return;
            }

            String insertSql = "INSERT INTO system_settings (setting_key, setting_value, description, updated_at) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.setString(3, description);
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed setting: " + key, e);
            throw new DatabaseException("Database error setting: " + key, e);
        }
    }

    @Override
    public String getSettingValue(String key, String defaultValue) {
        return findByKey(key).map(SystemSetting::getSettingValue).orElse(defaultValue);
    }
}
