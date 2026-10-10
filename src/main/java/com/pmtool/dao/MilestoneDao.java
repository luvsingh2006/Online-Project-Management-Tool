package com.pmtool.dao;

import com.pmtool.model.Milestone;
import java.util.List;

public interface MilestoneDao extends GenericDao<Milestone, Long> {
    List<Milestone> findByProjectId(Long projectId);
    boolean markCompleted(Long milestoneId, boolean completed);
}
