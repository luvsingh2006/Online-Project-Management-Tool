package com.pmtool.web.controller;

import com.pmtool.service.UserService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;

import java.util.Map;

public class SystemOverviewServlet extends HttpServlet {

    private final UserService userService;

    public SystemOverviewServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, Object> overview = userService.getSystemOverviewStats();
        resp.sendJson(200, overview);
    }
}
