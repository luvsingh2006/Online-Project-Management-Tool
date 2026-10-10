package com.pmtool.web.controller;

import com.pmtool.model.User;
import com.pmtool.service.UserService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.util.Map;

public class AdminSettingsServlet extends HttpServlet {

    private final UserService userService;

    public AdminSettingsServlet(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Map<String, String> settings = userService.getSystemSettings();
        resp.sendJson(200, settings);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User admin = req.getCurrentUser();
        Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());
        String key = (String) body.get("key");
        String value = (String) body.get("value");

        if (key == null || value == null) {
            resp.sendError(400, "key and value are required");
            return;
        }

        userService.updateSystemSetting(key, value, admin != null ? admin.getId() : null);
        resp.sendJson(200, Map.of("key", key, "value", value, "message", "Setting updated"));
    }
}
