package com.pmtool.web.controller;

import com.pmtool.model.User;
import com.pmtool.service.UserService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.util.Map;

public class UserProfileServlet extends HttpServlet {

    private final UserService userService;

    public UserProfileServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User current = req.getCurrentUser();
        if (current == null) {
            resp.sendError(401, "Not authenticated");
            return;
        }
        User fresh = userService.getUserById(current.getId());
        resp.sendJson(200, fresh.toSafeMap());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User current = req.getCurrentUser();
        if (current == null) {
            resp.sendError(401, "Not authenticated");
            return;
        }
        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());
        User updated = new User();
        updated.setFullName((String) body.get("fullName"));
        updated.setDepartment((String) body.get("department"));
        updated.setJobTitle((String) body.get("jobTitle"));

        User saved = userService.updateUser(current.getId(), updated, current.getId());
        resp.sendJson(200, saved.toSafeMap());
    }
}
