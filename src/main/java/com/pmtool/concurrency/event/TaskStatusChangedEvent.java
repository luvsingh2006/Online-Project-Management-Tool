package com.pmtool.concurrency.event;

import com.pmtool.model.TaskStatus;
import java.io.Serializable;

public class TaskStatusChangedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Long taskId;
    private final Long projectId;
    private final TaskStatus oldStatus;
    private final TaskStatus newStatus;

    public TaskStatusChangedEvent(Long taskId, Long projectId, TaskStatus oldStatus, TaskStatus newStatus) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Long getTaskId() { return taskId; }
    public Long getProjectId() { return projectId; }
    public TaskStatus getOldStatus() { return oldStatus; }
    public TaskStatus getNewStatus() { return newStatus; }
}
