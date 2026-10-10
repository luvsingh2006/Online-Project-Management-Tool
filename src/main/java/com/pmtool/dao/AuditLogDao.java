package com.pmtool.dao;

import com.pmtool.model.AuditLog;
import java.util.List;

public interface AuditLogDao extends GenericDao<AuditLog, Long> {
    List<AuditLog> findRecentLogs(int limit);
    List<AuditLog> findByEntityType(String entityType, String entityId);
    List<AuditLog> findByUserId(Long userId);
}
