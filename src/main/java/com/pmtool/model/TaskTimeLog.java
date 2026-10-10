package com.pmtool.model;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;

public class TaskTimeLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long userId;
    private String userName;
    private double hoursSpent;
    private Timestamp logDate;
    private String note;

    public TaskTimeLog() { super(); }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public double getHoursSpent() { return hoursSpent; }
    public void setHoursSpent(double hoursSpent) { this.hoursSpent = hoursSpent; }
    public Timestamp getLogDate() { return logDate; }
    public void setLogDate(Timestamp logDate) { this.logDate = logDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("taskId", taskId);
        map.put("userId", userId);
        map.put("userName", userName != null ? userName : "Team Member");
        map.put("hoursSpent", hoursSpent);
        map.put("logDate", logDate != null ? logDate.toString() : null);
        map.put("note", note);
        map.put("createdAt", createdAt != null ? createdAt.toString() : null);
        return map;
    }
}
