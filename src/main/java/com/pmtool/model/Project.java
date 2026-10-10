package com.pmtool.model;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public class Project extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String code;
    private String title;
    private String description;
    private Long managerId;
    private String managerName;
    private ProjectStatus status;
    private Priority priority;
    private Timestamp startDate;
    private Timestamp dueDate;
    private Double budget;
    private int progressPercent;

    private int totalTasks;
    private int completedTasks;
    private int inProgressTasks;

    public Project() {
        super();
        this.status = ProjectStatus.PLANNING;
        this.priority = Priority.MEDIUM;
        this.progressPercent = 0;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }
    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = Math.max(0, Math.min(100, progressPercent)); }
    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }
    public int getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }
    public int getInProgressTasks() { return inProgressTasks; }
    public void setInProgressTasks(int inProgressTasks) { this.inProgressTasks = inProgressTasks; }

    public void recalculateProgress() {
        if (totalTasks == 0) {
            this.progressPercent = (status == ProjectStatus.COMPLETED) ? 100 : 0;
        } else {
            this.progressPercent = Math.round(((float) completedTasks / totalTasks) * 100);
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("code", code);
        map.put("title", title);
        map.put("description", description);
        map.put("managerId", managerId);
        map.put("managerName", managerName != null ? managerName : "Unassigned");
        map.put("status", status != null ? status.name() : null);
        map.put("statusLabel", status != null ? status.getLabel() : null);
        map.put("priority", priority != null ? priority.name() : null);
        map.put("priorityLabel", priority != null ? priority.getLabel() : null);
        map.put("startDate", startDate != null ? startDate.toString() : null);
        map.put("dueDate", dueDate != null ? dueDate.toString() : null);
        map.put("budget", budget != null ? budget : 0.0);
        map.put("progressPercent", progressPercent);
        map.put("totalTasks", totalTasks);
        map.put("completedTasks", completedTasks);
        map.put("inProgressTasks", inProgressTasks);
        map.put("createdAt", createdAt != null ? createdAt.toString() : null);
        map.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);
        return map;
    }
}
