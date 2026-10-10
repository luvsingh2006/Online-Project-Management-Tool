package com.pmtool.web.controller;

import com.pmtool.model.User;
import com.pmtool.service.UserService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AssignableUserServlet extends HttpServlet {

    private final UserService userService;

    public AssignableUserServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        List<User> list = userService.getAssignableMembers();
        List<Map<String, Object>> res = new ArrayList<>();
        for (User u : list) {
            res.add(u.toSafeMap());
        }
        resp.sendJson(200, res);
    }
}
