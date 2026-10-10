package com.pmtool.dao;

import com.pmtool.model.TaskTimeLog;
import java.util.List;

public interface TaskTimeLogDao extends GenericDao<TaskTimeLog, Long> {
    List<TaskTimeLog> findByTaskId(Long taskId);
    List<TaskTimeLog> findByUserId(Long userId);
    double getTotalHoursForTask(Long taskId);
}
