package com.pmtool.service;

import com.pmtool.model.Comment;
import com.pmtool.model.Priority;
import com.pmtool.model.Task;
import com.pmtool.model.TaskStatus;
import com.pmtool.model.TaskTimeLog;

import java.util.List;

public interface TaskService {
    Task createTask(Task task, Long creatorId);
    Task updateTask(Long taskId, Task task, Long modifierId);
    Task updateTaskStatus(Long taskId, TaskStatus newStatus, Long operatorId);
    void deleteTask(Long taskId, Long operatorId);
    Task getTaskById(Long taskId);
    List<Task> listTasks(Long projectId, Long assigneeId, TaskStatus status, Priority priority, String search);
    TaskTimeLog logWorkHours(Long taskId, Long userId, double hours, String note);
    List<TaskTimeLog> getTaskTimeLogs(Long taskId);
    Comment addComment(Long taskId, Long userId, String content);
    List<Comment> getTaskComments(Long taskId);
}
