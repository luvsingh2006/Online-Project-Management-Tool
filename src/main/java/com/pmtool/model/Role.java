package com.pmtool.model;

public enum Role {
    ADMIN("Administrator", "Full system access, user administration, and organizational settings"),
    PROJECT_MANAGER("Project Manager", "Creates and oversees projects, milestones, task assignments, and progress tracking"),
    TEAM_MEMBER("Team Member", "Executes assigned tasks, updates progress status, logs hours, and posts comments");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public boolean canManageUsers() { return this == ADMIN; }
    public boolean canCreateProjects() { return this == ADMIN || this == PROJECT_MANAGER; }
    public boolean canAssignTasks() { return this == ADMIN || this == PROJECT_MANAGER; }
    public boolean canConfigureSystem() { return this == ADMIN; }
}
