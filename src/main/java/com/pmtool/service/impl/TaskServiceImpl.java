package com.pmtool.service.impl;

import com.pmtool.concurrency.EventBus;
import com.pmtool.concurrency.event.AuditLogEvent;
import com.pmtool.concurrency.event.TaskStatusChangedEvent;
import com.pmtool.dao.CommentDao;
import com.pmtool.dao.ProjectDao;
import com.pmtool.dao.TaskDao;
import com.pmtool.dao.TaskTimeLogDao;
import com.pmtool.dao.UserDao;
import com.pmtool.exception.ResourceNotFoundException;
import com.pmtool.exception.ValidationException;
import com.pmtool.model.Comment;
import com.pmtool.model.Priority;
import com.pmtool.model.Project;
import com.pmtool.model.Task;
import com.pmtool.model.TaskStatus;
import com.pmtool.model.TaskTimeLog;
import com.pmtool.service.TaskService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public class TaskServiceImpl implements TaskService {

    private final TaskDao taskDao;
    private final ProjectDao projectDao;
    private final UserDao userDao;
    private final TaskTimeLogDao taskTimeLogDao;
    private final CommentDao commentDao;
    private final EventBus eventBus;

    public TaskServiceImpl(TaskDao taskDao, ProjectDao projectDao, UserDao userDao,
                           TaskTimeLogDao taskTimeLogDao, CommentDao commentDao, EventBus eventBus) {
        this.taskDao = taskDao;
        this.projectDao = projectDao;
        this.userDao = userDao;
        this.taskTimeLogDao = taskTimeLogDao;
        this.commentDao = commentDao;
        this.eventBus = eventBus;

        this.eventBus.subscribe(TaskStatusChangedEvent.class, event -> {
            projectDao.syncAggregatedTaskCounts(event.getProjectId());
        });
    }

    @Override
    public Task createTask(Task task, Long creatorId) {
        validateTask(task);

        Project project = projectDao.findById(task.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", task.getProjectId()));

        if (task.getCode() == null || task.getCode().isBlank()) {
            task.setCode(project.getCode() + "-T" + (taskDao.count() + 1));
        }

        if (task.getAssigneeId() != null) {
            userDao.findById(task.getAssigneeId())
                    .orElseThrow(() -> new ValidationException("Assignee user not found"));
        }

        task.setCreatorId(creatorId);
        task.setStatus(task.getStatus() != null ? task.getStatus() : TaskStatus.TODO);
        task.setPriority(task.getPriority() != null ? task.getPriority() : Priority.MEDIUM);
        task.setEstimatedHours(task.getEstimatedHours() != null ? task.getEstimatedHours() : 0.0);
        task.setLoggedHours(0.0);
        task.setCreatedAt(Timestamp.from(Instant.now()));
        task.setUpdatedAt(Timestamp.from(Instant.now()));

        Task saved = taskDao.save(task);
        projectDao.syncAggregatedTaskCounts(task.getProjectId());

        eventBus.publishAsync(new AuditLogEvent(creatorId, "TASK_CREATED", "TASK", saved.getId().toString(), "Created " + saved.getTitle(), null));
        return getTaskById(saved.getId());
    }

    @Override
    public Task updateTask(Long taskId, Task updated, Long modifierId) {
        Task existing = getTaskById(taskId);
        validateTask(updated);

        TaskStatus oldStatus = existing.getStatus();
        TaskStatus newStatus = updated.getStatus() != null ? updated.getStatus() : existing.getStatus();

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setAssigneeId(updated.getAssigneeId());
        existing.setMilestoneId(updated.getMilestoneId());
        existing.setStatus(newStatus);
        existing.setPriority(updated.getPriority() != null ? updated.getPriority() : existing.getPriority());
        existing.setEstimatedHours(updated.getEstimatedHours());
        existing.setDueDate(updated.getDueDate());
        existing.touch();

        if (newStatus == TaskStatus.COMPLETED && oldStatus != TaskStatus.COMPLETED) {
            existing.setCompletedAt(Timestamp.from(Instant.now()));
        } else if (newStatus != TaskStatus.COMPLETED) {
            existing.setCompletedAt(null);
        }

        taskDao.update(existing);

        if (oldStatus != newStatus) {
            eventBus.publishAsync(new TaskStatusChangedEvent(taskId, existing.getProjectId(), oldStatus, newStatus));
        } else {
            projectDao.syncAggregatedTaskCounts(existing.getProjectId());
        }

        eventBus.publishAsync(new AuditLogEvent(modifierId, "TASK_UPDATED", "TASK", taskId.toString(), "Updated " + existing.getTitle(), null));
        return getTaskById(taskId);
    }

    @Override
    public Task updateTaskStatus(Long taskId, TaskStatus newStatus, Long operatorId) {
        Task existing = getTaskById(taskId);
        TaskStatus oldStatus = existing.getStatus();

        if (oldStatus == newStatus) return existing;

        taskDao.updateStatus(taskId, newStatus);
        eventBus.publishSync(new TaskStatusChangedEvent(taskId, existing.getProjectId(), oldStatus, newStatus));
        eventBus.publishAsync(new AuditLogEvent(operatorId, "TASK_STATUS_CHANGED", "TASK", taskId.toString(),
                String.format("Transitioned from %s to %s", oldStatus.getLabel(), newStatus.getLabel()), null));

        return getTaskById(taskId);
    }

    @Override
    public void deleteTask(Long taskId, Long operatorId) {
        Task existing = getTaskById(taskId);
        Long projectId = existing.getProjectId();

        taskDao.deleteById(taskId);
        projectDao.syncAggregatedTaskCounts(projectId);

        eventBus.publishAsync(new AuditLogEvent(operatorId, "TASK_DELETED", "TASK", taskId.toString(), "Deleted " + existing.getTitle(), null));
    }

    @Override
    public Task getTaskById(Long taskId) {
        return taskDao.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    }

    @Override
    public List<Task> listTasks(Long projectId, Long assigneeId, TaskStatus status, Priority priority, String search) {
        return taskDao.filterTasks(projectId, assigneeId, status, priority, search);
    }

    @Override
    public TaskTimeLog logWorkHours(Long taskId, Long userId, double hours, String note) {
        if (hours <= 0) throw new ValidationException("Logged hours must be greater than 0");
        Task task = getTaskById(taskId);

        TaskTimeLog log = new TaskTimeLog();
        log.setTaskId(taskId);
        log.setUserId(userId);
        log.setHoursSpent(hours);
        log.setLogDate(Timestamp.from(Instant.now()));
        log.setNote(note != null ? note.trim() : "Work logged");
        log.setCreatedAt(Timestamp.from(Instant.now()));

        TaskTimeLog saved = taskTimeLogDao.save(log);
        taskDao.updateLoggedHours(taskId, hours);

        eventBus.publishAsync(new AuditLogEvent(userId, "TIME_LOGGED", "TASK", taskId.toString(),
                String.format("Logged %.1f hours on %s", hours, task.getTitle()), null));

        return saved;
    }

    @Override
    public List<TaskTimeLog> getTaskTimeLogs(Long taskId) {
        return taskTimeLogDao.findByTaskId(taskId);
    }

    @Override
    public Comment addComment(Long taskId, Long userId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ValidationException("Comment content cannot be empty");
        }
        getTaskById(taskId);

        Comment comment = new Comment();
        comment.setTaskId(taskId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setCreatedAt(Timestamp.from(Instant.now()));
        comment.setUpdatedAt(Timestamp.from(Instant.now()));

        Comment saved = commentDao.save(comment);
        eventBus.publishAsync(new AuditLogEvent(userId, "COMMENT_ADDED", "TASK", taskId.toString(), "Added discussion comment", null));
        return saved;
    }

    @Override
    public List<Comment> getTaskComments(Long taskId) {
        return commentDao.findByTaskId(taskId);
    }

    private void validateTask(Task task) {
        if (task.getTitle() == null || task.getTitle().trim().length() < 3) {
            throw new ValidationException("Task title must be at least 3 characters");
        }
        if (task.getProjectId() == null) {
            throw new ValidationException("Task must be associated with a valid project");
        }
    }
}
