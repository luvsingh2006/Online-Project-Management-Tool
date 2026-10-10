package com.pmtool.model;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public class Task extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String code;
    private Long projectId;
    private Long milestoneId;
    private Long assigneeId;
    private Long creatorId;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private Double estimatedHours;
    private Double loggedHours;
    private Timestamp dueDate;
    private Timestamp completedAt;

    private String projectName;
    private String projectCode;
    private String assigneeName;
    private String creatorName;

    public Task() {
        super();
        this.status = TaskStatus.TODO;
        this.priority = Priority.MEDIUM;
        this.estimatedHours = 0.0;
        this.loggedHours = 0.0;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public Long getMilestoneId() { return milestoneId; }
    public void setMilestoneId(Long milestoneId) { this.milestoneId = milestoneId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }
    public Double getLoggedHours() { return loggedHours; }
    public void setLoggedHours(Double loggedHours) { this.loggedHours = loggedHours; }
    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public Timestamp getCompletedAt() { return completedAt; }
    public void setCompletedAt(Timestamp completedAt) { this.completedAt = completedAt; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public boolean isOverdue() {
        if (dueDate == null || status == TaskStatus.COMPLETED) {
            return false;
        }
        return System.currentTimeMillis() > dueDate.getTime();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("code", code);
        map.put("projectId", projectId);
        map.put("projectName", projectName != null ? projectName : "Project #" + projectId);
        map.put("projectCode", projectCode != null ? projectCode : "");
        map.put("milestoneId", milestoneId);
        map.put("assigneeId", assigneeId);
        map.put("assigneeName", assigneeName != null ? assigneeName : "Unassigned");
        map.put("creatorId", creatorId);
        map.put("creatorName", creatorName != null ? creatorName : "Manager");
        map.put("title", title);
        map.put("description", description);
        map.put("status", status != null ? status.name() : null);
        map.put("statusLabel", status != null ? status.getLabel() : null);
        map.put("priority", priority != null ? priority.name() : null);
        map.put("priorityLabel", priority != null ? priority.getLabel() : null);
        map.put("estimatedHours", estimatedHours != null ? estimatedHours : 0.0);
        map.put("loggedHours", loggedHours != null ? loggedHours : 0.0);
        map.put("dueDate", dueDate != null ? dueDate.toString() : null);
        map.put("completedAt", completedAt != null ? completedAt.toString() : null);
        map.put("isOverdue", isOverdue());
        map.put("createdAt", createdAt != null ? createdAt.toString() : null);
        map.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);
        return map;
    }
}
