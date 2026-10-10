package com.pmtool.web.controller;

import com.pmtool.model.AuditLog;
import com.pmtool.service.UserService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminAuditLogServlet extends HttpServlet {

    private final UserService userService;

    public AdminAuditLogServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int limit = 50;
        String lStr = req.getParameter("limit");
        if (lStr != null && !lStr.isBlank()) {
            limit = Integer.parseInt(lStr);
        }
        List<AuditLog> list = userService.getRecentAuditLogs(limit);
        List<Map<String, Object>> res = new ArrayList<>();
        for (AuditLog log : list) {
            res.add(log.toMap());
        }
        resp.sendJson(200, res);
    }
}
