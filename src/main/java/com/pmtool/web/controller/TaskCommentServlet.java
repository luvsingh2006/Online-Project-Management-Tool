package com.pmtool.web.controller;

import com.pmtool.model.Comment;
import com.pmtool.model.User;
import com.pmtool.service.TaskService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskCommentServlet extends HttpServlet {

    private final TaskService taskService;

    public TaskCommentServlet(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long taskId = Long.parseLong(req.getPathParameter("id"));
        List<Comment> comments = taskService.getTaskComments(taskId);
        List<Map<String, Object>> res = new ArrayList<>();
        for (Comment c : comments) {
            res.add(c.toMap());
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
        String content = (String) body.get("content");

        Comment saved = taskService.addComment(taskId, user.getId(), content);
        resp.sendJson(201, saved.toMap());
    }
}
