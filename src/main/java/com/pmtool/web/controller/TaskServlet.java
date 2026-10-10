package com.pmtool.web.controller;

import com.pmtool.model.Priority;
import com.pmtool.model.Role;
import com.pmtool.model.Task;
import com.pmtool.model.TaskStatus;
import com.pmtool.model.User;
import com.pmtool.service.TaskService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskServlet extends HttpServlet {

    private final TaskService taskService;

    public TaskServlet(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = req.getCurrentUser();
        Long projectId = null;
        String pStr = req.getParameter("projectId");
        if (pStr != null && !pStr.isBlank() && !pStr.equals("ALL")) {
            projectId = Long.parseLong(pStr);
        }

        Long assigneeId = null;
        String aStr = req.getParameter("assigneeId");
        if (aStr != null && !aStr.isBlank() && !aStr.equals("ALL")) {
            assigneeId = Long.parseLong(aStr);
        } else if ("true".equalsIgnoreCase(req.getParameter("myTasksOnly")) && user != null) {
            assigneeId = user.getId();
        }

        TaskStatus status = null;
        String sStr = req.getParameter("status");
        if (sStr != null && !sStr.isBlank() && !sStr.equals("ALL")) {
            status = TaskStatus.valueOf(sStr);
        }

        Priority priority = null;
        String prStr = req.getParameter("priority");
        if (prStr != null && !prStr.isBlank() && !prStr.equals("ALL")) {
            priority = Priority.valueOf(prStr);
        }

        String search = req.getParameter("search");

        List<Task> tasks = taskService.listTasks(projectId, assigneeId, status, priority, search);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Task t : tasks) {
            res.add(t.toMap());
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

        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());
        Task task = new Task();
        task.setTitle((String) body.get("title"));
        task.setDescription((String) body.get("description"));

        Object pVal = body.get("projectId");
        if (pVal instanceof Number n) {
            task.setProjectId(n.longValue());
        }

        Object aVal = body.get("assigneeId");
        if (aVal instanceof Number n) {
            task.setAssigneeId(n.longValue());
        }

        Object mVal = body.get("milestoneId");
        if (mVal instanceof Number n) {
            task.setMilestoneId(n.longValue());
        }

        String stStr = (String) body.get("status");
        task.setStatus(stStr != null ? TaskStatus.valueOf(stStr) : TaskStatus.TODO);

        String prioStr = (String) body.get("priority");
        task.setPriority(prioStr != null ? Priority.valueOf(prioStr) : Priority.MEDIUM);

        Object estVal = body.get("estimatedHours");
        if (estVal instanceof Number n) {
            task.setEstimatedHours(n.doubleValue());
        }

        String dueStr = (String) body.get("dueDate");
        if (dueStr != null && !dueStr.isBlank()) {
            if (dueStr.length() == 10) {
                task.setDueDate(Timestamp.valueOf(dueStr + " 23:59:59"));
            } else {
                task.setDueDate(Timestamp.valueOf(dueStr));
            }
        }

        Task saved = taskService.createTask(task, user.getId());
        resp.sendJson(201, saved.toMap());
    }
}
