package com.pmtool.security;

import com.pmtool.model.User;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final User user;
    private final Instant createdAt;
    private volatile Instant lastAccessedAt;
    private final long timeoutMinutes;

    public UserSession(User user, long timeoutMinutes) {
        this.sessionId = UUID.randomUUID().toString();
        this.user = user;
        this.createdAt = Instant.now();
        this.lastAccessedAt = Instant.now();
        this.timeoutMinutes = timeoutMinutes > 0 ? timeoutMinutes : 120;
    }

    public String getSessionId() { return sessionId; }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAccessedAt() { return lastAccessedAt; }
    public void touch() { this.lastAccessedAt = Instant.now(); }
    public boolean isExpired() { return Instant.now().isAfter(lastAccessedAt.plusSeconds(timeoutMinutes * 60)); }
}
