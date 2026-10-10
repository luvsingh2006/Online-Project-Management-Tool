package com.pmtool.dao;

import com.pmtool.model.Priority;
import com.pmtool.model.Task;
import com.pmtool.model.TaskStatus;

import java.util.List;
import java.util.Optional;

public interface TaskDao extends GenericDao<Task, Long> {
    Optional<Task> findByCode(String code);
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssigneeId(Long assigneeId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> filterTasks(Long projectId, Long assigneeId, TaskStatus status, Priority priority, String search);
    boolean updateStatus(Long taskId, TaskStatus status);
    boolean updateLoggedHours(Long taskId, double addedHours);
}
