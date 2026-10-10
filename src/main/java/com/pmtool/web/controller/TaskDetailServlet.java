package com.pmtool.web.controller;

import com.pmtool.model.Priority;
import com.pmtool.model.Task;
import com.pmtool.model.User;
import com.pmtool.service.TaskService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.sql.Timestamp;
import java.util.Map;

public class TaskDetailServlet extends HttpServlet {

    private final TaskService taskService;

    public TaskDetailServlet(TaskService taskService) {
        this.taskService = taskService;
    }

    private Long parseId(HttpServletRequest req) {
        return Long.parseLong(req.getPathParameter("id"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long id = parseId(req);
        Task task = taskService.getTaskById(id);
        resp.sendJson(200, task.toMap());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = req.getCurrentUser();
        Long id = parseId(req);

        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());
        Task task = new Task();
        task.setTitle((String) body.get("title"));
        task.setDescription((String) body.get("description"));

        Object aVal = body.get("assigneeId");
        if (aVal instanceof Number n) task.setAssigneeId(n.longValue());

        String prioStr = (String) body.get("priority");
        if (prioStr != null) task.setPriority(Priority.valueOf(prioStr));

        Object estVal = body.get("estimatedHours");
        if (estVal instanceof Number n) task.setEstimatedHours(n.doubleValue());

        String dueStr = (String) body.get("dueDate");
        if (dueStr != null && !dueStr.isBlank()) {
            if (dueStr.length() == 10) {
                task.setDueDate(Timestamp.valueOf(dueStr + " 23:59:59"));
            } else {
                task.setDueDate(Timestamp.valueOf(dueStr));
            }
        }

        Task updated = taskService.updateTask(id, task, user != null ? user.getId() : null);
        resp.sendJson(200, updated.toMap());
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = req.getCurrentUser();
        Long id = parseId(req);
        taskService.deleteTask(id, user != null ? user.getId() : null);
        resp.sendJson(200, Map.of("message", "Task deleted successfully"));
    }
}
