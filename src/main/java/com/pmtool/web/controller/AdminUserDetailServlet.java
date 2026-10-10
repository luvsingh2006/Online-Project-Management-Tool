package com.pmtool.web.controller;

import com.pmtool.model.Role;
import com.pmtool.model.User;
import com.pmtool.model.UserStatus;
import com.pmtool.service.UserService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.util.Map;

public class AdminUserDetailServlet extends HttpServlet {

    private final UserService userService;

    public AdminUserDetailServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long id = Long.parseLong(req.getPathParameter("id"));
        User user = userService.getUserById(id);
        resp.sendJson(200, user.toSafeMap());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User admin = req.getCurrentUser();
        Long id = Long.parseLong(req.getPathParameter("id"));
        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());

        if (body.containsKey("status") && body.size() <= 2) {
            String stStr = (String) body.get("status");
            UserStatus st = UserStatus.valueOf(stStr);
            userService.toggleUserStatus(id, st, admin != null ? admin.getId() : null);
            resp.sendJson(200, Map.of("message", "User status updated to " + st));
            return;
        }

        User updated = new User();
        updated.setFullName((String) body.get("fullName"));
        updated.setEmail((String) body.get("email"));
        updated.setDepartment((String) body.get("department"));
        updated.setJobTitle((String) body.get("jobTitle"));

        String rStr = (String) body.get("role");
        if (rStr != null) updated.setRole(Role.valueOf(rStr));

        String stStr = (String) body.get("status");
        if (stStr != null) updated.setStatus(UserStatus.valueOf(stStr));

        User saved = userService.updateUser(id, updated, admin != null ? admin.getId() : null);
        resp.sendJson(200, saved.toSafeMap());
    }
}
