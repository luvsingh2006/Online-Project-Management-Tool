package com.pmtool.service;

import com.pmtool.model.AuditLog;
import com.pmtool.model.Role;
import com.pmtool.model.User;
import com.pmtool.model.UserStatus;

import java.util.List;
import java.util.Map;

public interface UserService {
    User createUser(User user, String initialPassword, Long adminId);
    User updateUser(Long userId, User user, Long adminId);
    void toggleUserStatus(Long userId, UserStatus status, Long adminId);
    User getUserById(Long userId);
    List<User> listUsers(Role role, String search);
    List<User> getAssignableMembers();
    List<AuditLog> getRecentAuditLogs(int limit);
    Map<String, String> getSystemSettings();
    void updateSystemSetting(String key, String value, Long adminId);
    Map<String, Object> getSystemOverviewStats();
}
