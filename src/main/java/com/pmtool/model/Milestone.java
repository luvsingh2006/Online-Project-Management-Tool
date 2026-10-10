package com.pmtool.model;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public class Milestone extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long projectId;
    private String title;
    private Timestamp dueDate;
    private boolean completed;

    public Milestone() {
        super();
        this.completed = false;
    }

    public Milestone(Long id, Long projectId, String title, Timestamp dueDate, boolean completed) {
        super(id);
        this.projectId = projectId;
        this.title = title;
        this.dueDate = dueDate;
        this.completed = completed;
    }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) { this.dueDate = dueDate; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("projectId", projectId);
        map.put("title", title);
        map.put("dueDate", dueDate != null ? dueDate.toString() : null);
        map.put("completed", completed);
        map.put("createdAt", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
