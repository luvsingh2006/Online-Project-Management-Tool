package com.pmtool.web.controller;

import com.pmtool.model.Priority;
import com.pmtool.model.Project;
import com.pmtool.model.ProjectStatus;
import com.pmtool.model.User;
import com.pmtool.service.ProjectService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.sql.Timestamp;
import java.util.Map;

public class ProjectDetailServlet extends HttpServlet {

    private final ProjectService projectService;

    public ProjectDetailServlet(ProjectService projectService) {
        this.projectService = projectService;
    }

    private Long parseId(HttpServletRequest req) {
        String idStr = req.getPathParameter("id");
        return Long.parseLong(idStr);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long id = parseId(req);
        Project project = projectService.getProjectById(id);
        resp.sendJson(200, project.toMap());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = req.getCurrentUser();
        Long id = parseId(req);

        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());
        Project project = new Project();
        project.setTitle((String) body.get("title"));
        project.setDescription((String) body.get("description"));

        Object mgrIdVal = body.get("managerId");
        if (mgrIdVal instanceof Number n) project.setManagerId(n.longValue());

        String stStr = (String) body.get("status");
        if (stStr != null) project.setStatus(ProjectStatus.valueOf(stStr));

        String prioStr = (String) body.get("priority");
        if (prioStr != null) project.setPriority(Priority.valueOf(prioStr));

        Object budgetVal = body.get("budget");
        if (budgetVal instanceof Number n) project.setBudget(n.doubleValue());

        String startDateStr = (String) body.get("startDate");
        if (startDateStr != null && !startDateStr.isBlank()) {
            project.setStartDate(Timestamp.valueOf(startDateStr + " 00:00:00"));
        }

        String dueDateStr = (String) body.get("dueDate");
        if (dueDateStr != null && !dueDateStr.isBlank()) {
            project.setDueDate(Timestamp.valueOf(dueDateStr + " 23:59:59"));
        }

        Project updated = projectService.updateProject(id, project, user != null ? user.getId() : null);
        resp.sendJson(200, updated.toMap());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = req.getCurrentUser();
        Long id = parseId(req);
        projectService.deleteProject(id, user != null ? user.getId() : null);
        resp.sendJson(200, Map.of("message", "Project deleted successfully"));
    }
}
