package com.pmtool.service.impl;

import com.pmtool.concurrency.EventBus;
import com.pmtool.concurrency.event.AuditLogEvent;
import com.pmtool.dao.AuditLogDao;
import com.pmtool.dao.ProjectDao;
import com.pmtool.dao.SystemSettingDao;
import com.pmtool.dao.TaskDao;
import com.pmtool.dao.UserDao;
import com.pmtool.exception.ResourceNotFoundException;
import com.pmtool.exception.ValidationException;
import com.pmtool.model.AuditLog;
import com.pmtool.model.Role;
import com.pmtool.model.SystemSetting;
import com.pmtool.model.User;
import com.pmtool.model.UserStatus;
import com.pmtool.security.PasswordHasher;
import com.pmtool.service.UserService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final ProjectDao projectDao;
    private final TaskDao taskDao;
    private final AuditLogDao auditLogDao;
    private final SystemSettingDao systemSettingDao;
    private final EventBus eventBus;

    public UserServiceImpl(UserDao userDao, ProjectDao projectDao, TaskDao taskDao,
                           AuditLogDao auditLogDao, SystemSettingDao systemSettingDao, EventBus eventBus) {
        this.userDao = userDao;
        this.projectDao = projectDao;
        this.taskDao = taskDao;
        this.auditLogDao = auditLogDao;
        this.systemSettingDao = systemSettingDao;
        this.eventBus = eventBus;
    }

    @Override
    public User createUser(User user, String initialPassword, Long adminId) {
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new ValidationException("Valid email address is required");
        }
        if (user.getFullName() == null || user.getFullName().trim().length() < 2) {
            throw new ValidationException("Full name must be at least 2 characters");
        }
        if (initialPassword == null || initialPassword.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }

        String normalizedEmail = user.getEmail().trim().toLowerCase();
        if (userDao.findByEmail(normalizedEmail).isPresent()) {
            throw new ValidationException("An account with email " + normalizedEmail + " already exists");
        }

        user.setEmail(normalizedEmail);
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword(initialPassword, salt);
        user.setSalt(salt);
        user.setPasswordHash(hash);
        user.setStatus(user.getStatus() != null ? user.getStatus() : UserStatus.ACTIVE);
        user.setRole(user.getRole() != null ? user.getRole() : Role.TEAM_MEMBER);
        user.setCreatedAt(Timestamp.from(Instant.now()));
        user.setUpdatedAt(Timestamp.from(Instant.now()));

        User saved = userDao.save(user);
        eventBus.publishAsync(new AuditLogEvent(adminId, "USER_CREATED", "USER", saved.getId().toString(),
                String.format("Created user %s (%s)", saved.getFullName(), saved.getEmail()), null));
        return saved;
    }

    @Override
    public User updateUser(Long userId, User updated, Long adminId) {
        User existing = getUserById(userId);

        if (updated.getFullName() != null && updated.getFullName().trim().length() >= 2) {
            existing.setFullName(updated.getFullName().trim());
        }
        if (updated.getRole() != null) existing.setRole(updated.getRole());
        if (updated.getDepartment() != null) existing.setDepartment(updated.getDepartment());
        if (updated.getJobTitle() != null) existing.setJobTitle(updated.getJobTitle());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        existing.touch();

        userDao.update(existing);
        eventBus.publishAsync(new AuditLogEvent(adminId, "USER_UPDATED", "USER", userId.toString(), "Updated profile for " + existing.getFullName(), null));
        return existing;
    }

    @Override
    public void toggleUserStatus(Long userId, UserStatus status, Long adminId) {
        User existing = getUserById(userId);
        userDao.updateStatus(userId, status);
        eventBus.publishAsync(new AuditLogEvent(adminId, "USER_STATUS_CHANGED", "USER", userId.toString(),
                String.format("Changed status of %s to %s", existing.getFullName(), status.getLabel()), null));
    }

    @Override
    public User getUserById(Long userId) {
        return userDao.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    @Override
    public List<User> listUsers(Role role, String search) {
        if (search != null && !search.isBlank()) {
            List<User> list = userDao.searchUsers(search.trim());
            if (role != null) list.removeIf(u -> u.getRole() != role);
            return list;
        }
        if (role != null) return userDao.findByRole(role);
        return userDao.findAll();
    }

    @Override
    public List<User> getAssignableMembers() {
        return userDao.findActiveUsers();
    }

    @Override
    public List<AuditLog> getRecentAuditLogs(int limit) {
        return auditLogDao.findRecentLogs(limit);
    }

    @Override
    public Map<String, String> getSystemSettings() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SystemSetting s : systemSettingDao.findAll()) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }

    @Override
    public void updateSystemSetting(String key, String value, Long adminId) {
        systemSettingDao.setSetting(key, value, "System property configured by administrator");
        eventBus.publishAsync(new AuditLogEvent(adminId, "SETTING_CHANGED", "SETTING", key, "Set " + key + " = " + value, null));
    }

    @Override
    public Map<String, Object> getSystemOverviewStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", userDao.count());
        stats.put("totalProjects", projectDao.count());
        stats.put("totalTasks", taskDao.count());
        stats.put("activeUsers", (long) userDao.findActiveUsers().size());
        return stats;
    }
}
