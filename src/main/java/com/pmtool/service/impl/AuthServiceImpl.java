package com.pmtool.service.impl;

import com.pmtool.concurrency.EventBus;
import com.pmtool.concurrency.event.AuditLogEvent;
import com.pmtool.config.AppConfig;
import com.pmtool.dao.UserDao;
import com.pmtool.exception.AuthenticationException;
import com.pmtool.exception.ResourceNotFoundException;
import com.pmtool.exception.ValidationException;
import com.pmtool.model.User;
import com.pmtool.security.PasswordHasher;
import com.pmtool.security.UserSession;
import com.pmtool.service.AuthService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = Logger.getLogger(AuthServiceImpl.class.getName());
    private final UserDao userDao;
    private final EventBus eventBus;
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();
    private final long sessionTimeoutMinutes;

    public AuthServiceImpl(UserDao userDao, EventBus eventBus) {
        this.userDao = userDao;
        this.eventBus = eventBus;
        this.sessionTimeoutMinutes = AppConfig.getLong("auth.session.timeoutMinutes", 120);
    }

    @Override
    public User authenticate(String email, String password, String ipAddress) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Email and password are required");
        }

        Optional<User> userOpt = userDao.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            eventBus.publishAsync(new AuditLogEvent(null, "LOGIN_FAILED", "USER", email, "Email not found", ipAddress));
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            eventBus.publishAsync(new AuditLogEvent(user.getId(), "LOGIN_BLOCKED", "USER", user.getId().toString(), "Account deactivated", ipAddress));
            throw new AuthenticationException("Account has been deactivated. Please contact an administrator.");
        }

        if (!PasswordHasher.verify(password, user.getSalt(), user.getPasswordHash())) {
            eventBus.publishAsync(new AuditLogEvent(user.getId(), "LOGIN_FAILED", "USER", user.getId().toString(), "Incorrect password", ipAddress));
            throw new AuthenticationException("Invalid email or password");
        }

        eventBus.publishAsync(new AuditLogEvent(user.getId(), "LOGIN_SUCCESS", "USER", user.getId().toString(), "Authenticated successfully", ipAddress));
        return user;
    }

    @Override
    public String createSession(User user) {
        cleanExpiredSessions();
        UserSession session = new UserSession(user, sessionTimeoutMinutes);
        sessions.put(session.getSessionId(), session);
        return session.getSessionId();
    }

    @Override
    public Optional<User> validateSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();
        UserSession session = sessions.get(sessionId);
        if (session == null) return Optional.empty();
        if (session.isExpired()) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        session.touch();
        return Optional.of(session.getUser());
    }

    @Override
    public void invalidateSession(String sessionId) {
        if (sessionId != null) sessions.remove(sessionId);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userDao.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!PasswordHasher.verify(oldPassword, user.getSalt(), user.getPasswordHash())) {
            throw new ValidationException("Existing password does not match");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new ValidationException("New password must be at least 6 characters long");
        }
        String newSalt = PasswordHasher.generateSalt();
        String newHash = PasswordHasher.hashPassword(newPassword, newSalt);
        userDao.updatePassword(userId, newHash, newSalt);
        eventBus.publishAsync(new AuditLogEvent(userId, "PASSWORD_CHANGED", "USER", userId.toString(), "Password updated", null));
    }

    private void cleanExpiredSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
