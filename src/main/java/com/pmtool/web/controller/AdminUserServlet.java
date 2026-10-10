package com.pmtool.web.controller;

import com.pmtool.model.Role;
import com.pmtool.model.User;
import com.pmtool.model.UserStatus;
import com.pmtool.service.UserService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminUserServlet extends HttpServlet {

    private final UserService userService;

    public AdminUserServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Role role = null;
        String rStr = req.getParameter("role");
        if (rStr != null && !rStr.isBlank() && !rStr.equals("ALL")) {
            role = Role.valueOf(rStr);
        }
        String search = req.getParameter("search");
        List<User> list = userService.listUsers(role, search);
        List<Map<String, Object>> res = new ArrayList<>();
        for (User u : list) {
            res.add(u.toSafeMap());
        }
        resp.sendJson(200, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User admin = req.getCurrentUser();
        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());

        User newUser = new User();
        newUser.setEmail((String) body.get("email"));
        newUser.setFullName((String) body.get("fullName"));
        newUser.setDepartment((String) body.get("department"));
        newUser.setJobTitle((String) body.get("jobTitle"));

        String rStr = (String) body.get("role");
        newUser.setRole(rStr != null ? Role.valueOf(rStr) : Role.TEAM_MEMBER);

        String stStr = (String) body.get("status");
        newUser.setStatus(stStr != null ? UserStatus.valueOf(stStr) : UserStatus.ACTIVE);

        String password = (String) body.get("password");
        if (password == null || password.isBlank()) {
            password = "TempPassword@123";
        }

        User created = userService.createUser(newUser, password, admin != null ? admin.getId() : null);
        resp.sendJson(201, created.toSafeMap());
    }
}
