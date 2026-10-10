package com.pmtool.concurrency.event;

import java.io.Serializable;

public class AuditLogEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String action;
    private final String entityType;
    private final String entityId;
    private final String details;
    private final String ipAddress;

    public AuditLogEvent(Long userId, String action, String entityType, String entityId, String details, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Long getUserId() { return userId; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getDetails() { return details; }
    public String getIpAddress() { return ipAddress; }
}
