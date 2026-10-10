package com.pmtool.web.controller;

import com.pmtool.service.ProjectService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;

import java.util.Map;

public class ProjectAnalyticsServlet extends HttpServlet {

    private final ProjectService projectService;

    public ProjectAnalyticsServlet(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long id = Long.parseLong(req.getPathParameter("id"));
        Map<String, Object> analytics = projectService.getProjectAnalytics(id);
        resp.sendJson(200, analytics);
    }
}
