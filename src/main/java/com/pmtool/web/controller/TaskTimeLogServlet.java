package com.pmtool.web.controller;

import com.pmtool.model.TaskTimeLog;
import com.pmtool.model.User;
import com.pmtool.service.TaskService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskTimeLogServlet extends HttpServlet {

    private final TaskService taskService;

    public TaskTimeLogServlet(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long taskId = Long.parseLong(req.getPathParameter("id"));
        List<TaskTimeLog> logs = taskService.getTaskTimeLogs(taskId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (TaskTimeLog log : logs) {
            res.add(log.toMap());
        }
        resp.sendJson(200, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = req.getCurrentUser();
        if (user == null) {
            resp.sendError(401, "Authentication required");
            return;
        }
        Long taskId = Long.parseLong(req.getPathParameter("id"));
        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());

        double hours = 0.0;
        Object hVal = body.get("hoursSpent");
        if (hVal == null) hVal = body.get("hours");
        if (hVal instanceof Number n) {
            hours = n.doubleValue();
        }

        String note = (String) body.get("note");
        TaskTimeLog saved = taskService.logWorkHours(taskId, user.getId(), hours, note);
        resp.sendJson(201, saved.toMap());
    }
}
