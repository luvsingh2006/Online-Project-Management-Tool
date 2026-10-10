package com.pmtool.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class AuditLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String userName;
    private String action;
    private String entityType;
    private String entityId;
    private String details;
    private String ipAddress;

    public AuditLog() { super(); }

    public AuditLog(Long id, Long userId, String action, String entityType, String entityId, String details, String ipAddress) {
        super(id);
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("userName", userName != null ? userName : "System");
        map.put("action", action);
        map.put("entityType", entityType);
        map.put("entityId", entityId);
        map.put("details", details);
        map.put("ipAddress", ipAddress != null ? ipAddress : "-");
        map.put("createdAt", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
