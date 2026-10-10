package com.pmtool.service.impl;

import com.pmtool.concurrency.EventBus;
import com.pmtool.concurrency.event.AuditLogEvent;
import com.pmtool.dao.MilestoneDao;
import com.pmtool.dao.ProjectDao;
import com.pmtool.dao.TaskDao;
import com.pmtool.dao.UserDao;
import com.pmtool.exception.ResourceNotFoundException;
import com.pmtool.exception.ValidationException;
import com.pmtool.model.Milestone;
import com.pmtool.model.Priority;
import com.pmtool.model.Project;
import com.pmtool.model.ProjectStatus;
import com.pmtool.model.Task;
import com.pmtool.service.ProjectService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class ProjectServiceImpl implements ProjectService {

    private final ProjectDao projectDao;
    private final MilestoneDao milestoneDao;
    private final TaskDao taskDao;
    private final UserDao userDao;
    private final EventBus eventBus;

    public ProjectServiceImpl(ProjectDao projectDao, MilestoneDao milestoneDao, TaskDao taskDao, UserDao userDao, EventBus eventBus) {
        this.projectDao = projectDao;
        this.milestoneDao = milestoneDao;
        this.taskDao = taskDao;
        this.userDao = userDao;
        this.eventBus = eventBus;
    }

    @Override
    public Project createProject(Project project, Long creatorId) {
        validateProject(project);

        if (project.getCode() == null || project.getCode().isBlank()) {
            project.setCode("PRJ-" + (100 + projectDao.count() + 1));
        }

        if (projectDao.findByCode(project.getCode()).isPresent()) {
            throw new ValidationException("Project code '" + project.getCode() + "' is already in use");
        }

        if (project.getManagerId() == null) {
            project.setManagerId(creatorId);
        }

        if (userDao.findById(project.getManagerId()).isEmpty()) {
            throw new ValidationException("Assigned manager does not exist");
        }

        project.setCreatedAt(Timestamp.from(Instant.now()));
        project.setUpdatedAt(Timestamp.from(Instant.now()));
        project.setProgressPercent(0);

        Project saved = projectDao.save(project);
        eventBus.publishAsync(new AuditLogEvent(creatorId, "PROJECT_CREATED", "PROJECT", saved.getId().toString(), "Created " + saved.getTitle(), null));
        return getProjectById(saved.getId());
    }

    @Override
    public Project updateProject(Long projectId, Project updated, Long modifierId) {
        Project existing = getProjectById(projectId);
        validateProject(updated);

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        if (updated.getManagerId() != null) existing.setManagerId(updated.getManagerId());
        if (updated.getStatus() != null) existing.setStatus(updated.getStatus());
        if (updated.getPriority() != null) existing.setPriority(updated.getPriority());
        existing.setStartDate(updated.getStartDate());
        existing.setDueDate(updated.getDueDate());
        existing.setBudget(updated.getBudget());
        existing.touch();

        projectDao.update(existing);
        projectDao.syncAggregatedTaskCounts(projectId);

        eventBus.publishAsync(new AuditLogEvent(modifierId, "PROJECT_UPDATED", "PROJECT", projectId.toString(), "Updated " + existing.getTitle(), null));
        return getProjectById(projectId);
    }

    @Override
    public void deleteProject(Long projectId, Long operatorId) {
        Project existing = getProjectById(projectId);

        List<Task> tasks = taskDao.findByProjectId(projectId);
        for (Task t : tasks) {
            taskDao.deleteById(t.getId());
        }

        List<Milestone> milestones = milestoneDao.findByProjectId(projectId);
        for (Milestone m : milestones) {
            milestoneDao.deleteById(m.getId());
        }

        projectDao.deleteById(projectId);
        eventBus.publishAsync(new AuditLogEvent(operatorId, "PROJECT_DELETED", "PROJECT", projectId.toString(), "Deleted " + existing.getTitle(), null));
    }

    @Override
    public Project getProjectById(Long projectId) {
        return projectDao.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    @Override
    public List<Project> listProjects(Long managerId, ProjectStatus status, Priority priority, String search) {
        if (search != null || status != null || priority != null) {
            List<Project> list = projectDao.searchAndFilter(search, status, priority);
            if (managerId != null) {
                list.removeIf(p -> !Objects.equals(p.getManagerId(), managerId));
            }
            return list;
        }
        if (managerId != null) {
            return projectDao.findByManagerId(managerId);
        }
        return projectDao.findAll();
    }

    @Override
    public Map<String, Object> getProjectAnalytics(Long projectId) {
        Project project = getProjectById(projectId);
        List<Task> tasks = taskDao.findByProjectId(projectId);

        int total = tasks.size();
        int todo = 0, inProgress = 0, inReview = 0, completed = 0, blocked = 0;
        double totalEstimated = 0.0;
        double totalLogged = 0.0;
        int overdue = 0;

        for (Task t : tasks) {
            switch (t.getStatus()) {
                case TODO -> todo++;
                case IN_PROGRESS -> inProgress++;
                case IN_REVIEW -> inReview++;
                case COMPLETED -> completed++;
                case BLOCKED -> blocked++;
            }
            if (t.getEstimatedHours() != null) totalEstimated += t.getEstimatedHours();
            if (t.getLoggedHours() != null) totalLogged += t.getLoggedHours();
            if (t.isOverdue()) overdue++;
        }

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("projectId", projectId);
        analytics.put("projectTitle", project.getTitle());
        analytics.put("projectCode", project.getCode());
        analytics.put("progressPercent", project.getProgressPercent());
        analytics.put("totalTasks", total);
        analytics.put("todoTasks", todo);
        analytics.put("inProgressTasks", inProgress);
        analytics.put("inReviewTasks", inReview);
        analytics.put("completedTasks", completed);
        analytics.put("blockedTasks", blocked);
        analytics.put("overdueTasks", overdue);
        analytics.put("estimatedHours", totalEstimated);
        analytics.put("loggedHours", totalLogged);
        analytics.put("budget", project.getBudget() != null ? project.getBudget() : 0.0);
        return analytics;
    }

    @Override
    public Milestone addMilestone(Long projectId, Milestone milestone) {
        getProjectById(projectId);
        if (milestone.getTitle() == null || milestone.getTitle().isBlank()) {
            throw new ValidationException("Milestone title is required");
        }
        milestone.setProjectId(projectId);
        milestone.setCreatedAt(Timestamp.from(Instant.now()));
        return milestoneDao.save(milestone);
    }

    @Override
    public List<Milestone> getMilestones(Long projectId) {
        return milestoneDao.findByProjectId(projectId);
    }

    @Override
    public void toggleMilestone(Long milestoneId, boolean completed) {
        milestoneDao.markCompleted(milestoneId, completed);
    }

    private void validateProject(Project project) {
        if (project.getTitle() == null || project.getTitle().trim().length() < 3) {
            throw new ValidationException("Project title must be at least 3 characters");
        }
        if (project.getStartDate() != null && project.getDueDate() != null) {
            if (project.getDueDate().before(project.getStartDate())) {
                throw new ValidationException("Due date cannot be earlier than start date");
            }
        }
    }
}
