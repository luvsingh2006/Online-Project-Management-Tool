package com.pmtool.web.controller;

import com.pmtool.model.Task;
import com.pmtool.model.TaskStatus;
import com.pmtool.model.User;
import com.pmtool.service.TaskService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.util.Map;

public class TaskStatusServlet extends HttpServlet {

    private final TaskService taskService;

    public TaskStatusServlet(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = req.getCurrentUser();
        Long id = Long.parseLong(req.getPathParameter("id"));

        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());
        String statusStr = (String) body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            resp.sendError(400, "status is required");
            return;
        }

        TaskStatus newStatus = TaskStatus.valueOf(statusStr);
        Task updated = taskService.updateTaskStatus(id, newStatus, user != null ? user.getId() : null);
        resp.sendJson(200, updated.toMap());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        doPost(req, resp);
    }
}
