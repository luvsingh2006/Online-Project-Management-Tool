package com.pmtool.web.controller;

import com.pmtool.model.*;
import com.pmtool.service.ProjectService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectServlet extends HttpServlet {

    private final ProjectService projectService;

    public ProjectServlet(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = req.getCurrentUser();
        Long managerId = null;

        String mgrParam = req.getParameter("managerId");
        if (mgrParam != null && !mgrParam.isBlank()) {
            managerId = Long.parseLong(mgrParam);
        } else if (user != null && user.getRole() == Role.PROJECT_MANAGER) {
            managerId = user.getId();
        }

        ProjectStatus status = null;
        String stParam = req.getParameter("status");
        if (stParam != null && !stParam.isBlank() && !stParam.equals("ALL")) {
            status = ProjectStatus.valueOf(stParam);
        }

        Priority priority = null;
        String prioParam = req.getParameter("priority");
        if (prioParam != null && !prioParam.isBlank() && !prioParam.equals("ALL")) {
            priority = Priority.valueOf(prioParam);
        }

        String search = req.getParameter("search");
        List<Project> list = projectService.listProjects(managerId, status, priority, search);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Project p : list) {
            res.add(p.toMap());
        }
        resp.sendJson(200, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = req.getCurrentUser();
        if (user == null || (!user.getRole().canCreateProjects())) {
            resp.sendError(403, "You do not have permission to create projects");
            return;
        }

        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());
        Project project = new Project();
        project.setTitle((String) body.get("title"));
        project.setDescription((String) body.get("description"));
        project.setCode((String) body.get("code"));

        Object mgrIdVal = body.get("managerId");
        if (mgrIdVal instanceof Number n) {
            project.setManagerId(n.longValue());
        } else {
            project.setManagerId(user.getId());
        }

        String stStr = (String) body.get("status");
        project.setStatus(stStr != null ? ProjectStatus.valueOf(stStr) : ProjectStatus.PLANNING);

        String prioStr = (String) body.get("priority");
        project.setPriority(prioStr != null ? Priority.valueOf(prioStr) : Priority.MEDIUM);

        Object budgetVal = body.get("budget");
        if (budgetVal instanceof Number n) {
            project.setBudget(n.doubleValue());
        }

        String startDateStr = (String) body.get("startDate");
        if (startDateStr != null && !startDateStr.isBlank()) {
            project.setStartDate(Timestamp.valueOf(startDateStr + " 00:00:00"));
        }

        String dueDateStr = (String) body.get("dueDate");
        if (dueDateStr != null && !dueDateStr.isBlank()) {
            project.setDueDate(Timestamp.valueOf(dueDateStr + " 23:59:59"));
        }

        Project saved = projectService.createProject(project, user.getId());
        resp.sendJson(201, saved.toMap());
    }
}
