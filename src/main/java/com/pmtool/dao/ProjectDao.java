package com.pmtool.dao;

import com.pmtool.model.Priority;
import com.pmtool.model.Project;
import com.pmtool.model.ProjectStatus;

import java.util.List;
import java.util.Optional;

public interface ProjectDao extends GenericDao<Project, Long> {
    Optional<Project> findByCode(String code);
    List<Project> findByManagerId(Long managerId);
    List<Project> findByStatus(ProjectStatus status);
    List<Project> searchAndFilter(String query, ProjectStatus status, Priority priority);
    boolean updateProgress(Long projectId, int progressPercent);
    void syncAggregatedTaskCounts(Long projectId);
}
