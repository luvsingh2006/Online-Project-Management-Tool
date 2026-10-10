package com.pmtool.service;

import com.pmtool.model.Milestone;
import com.pmtool.model.Priority;
import com.pmtool.model.Project;
import com.pmtool.model.ProjectStatus;

import java.util.List;
import java.util.Map;

public interface ProjectService {
    Project createProject(Project project, Long creatorId);
    Project updateProject(Long projectId, Project project, Long modifierId);
    void deleteProject(Long projectId, Long operatorId);
    Project getProjectById(Long projectId);
    List<Project> listProjects(Long managerId, ProjectStatus status, Priority priority, String search);
    Map<String, Object> getProjectAnalytics(Long projectId);
    Milestone addMilestone(Long projectId, Milestone milestone);
    List<Milestone> getMilestones(Long projectId);
    void toggleMilestone(Long milestoneId, boolean completed);
}
