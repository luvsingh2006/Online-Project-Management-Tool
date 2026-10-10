package com.pmtool.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Comment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long userId;
    private String userName;
    private String content;

    public Comment() { super(); }

    public Comment(Long id, Long taskId, Long userId, String content) {
        super(id);
        this.taskId = taskId;
        this.userId = userId;
        this.content = content;
    }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("taskId", taskId);
        map.put("userId", userId);
        map.put("userName", userName != null ? userName : "Author");
        map.put("content", content);
        map.put("createdAt", createdAt != null ? createdAt.toString() : null);
        map.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);
        return map;
    }
}
