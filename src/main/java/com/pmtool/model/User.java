package com.pmtool.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class User extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String email;
    private String passwordHash;
    private String salt;
    private String fullName;
    private Role role;
    private String department;
    private String jobTitle;
    private UserStatus status;

    public User() {
        super();
        this.status = UserStatus.ACTIVE;
        this.role = Role.TEAM_MEMBER;
    }

    public User(Long id, String email, String fullName, Role role, String department, String jobTitle) {
        super(id);
        this.email = email;
        this.fullName = fullName;
        this.role = role != null ? role : Role.TEAM_MEMBER;
        this.department = department;
        this.jobTitle = jobTitle;
        this.status = UserStatus.ACTIVE;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public boolean isActive() { return status == UserStatus.ACTIVE; }

    public Map<String, Object> toSafeMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("email", email);
        map.put("fullName", fullName);
        map.put("role", role != null ? role.name() : null);
        map.put("roleName", role != null ? role.getDisplayName() : null);
        map.put("department", department);
        map.put("jobTitle", jobTitle);
        map.put("status", status != null ? status.name() : null);
        map.put("createdAt", createdAt != null ? createdAt.toString() : null);
        map.put("updatedAt", updatedAt != null ? updatedAt.toString() : null);
        return map;
    }
}
