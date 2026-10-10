package com.pmtool.service;

import com.pmtool.model.User;
import java.util.Optional;

public interface AuthService {
    User authenticate(String email, String password, String ipAddress);
    String createSession(User user);
    Optional<User> validateSession(String sessionId);
    void invalidateSession(String sessionId);
    void changePassword(Long userId, String oldPassword, String newPassword);
}
