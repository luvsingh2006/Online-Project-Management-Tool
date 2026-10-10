package com.pmtool.web.controller;

import com.pmtool.config.AppConfig;
import com.pmtool.model.User;
import com.pmtool.service.AuthService;
import com.pmtool.web.servlet.HttpServlet;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;
import com.pmtool.web.util.JsonUtil;

import java.util.*;

public class AuthServlet extends HttpServlet {

    private final AuthService authService;
    private final String cookieName;

    public AuthServlet(AuthService authService) {
        this.authService = authService;
        this.cookieName = AppConfig.getString("auth.session.cookieName", "PMTOOL_SESSION_ID");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String path = req.getPath();

        if ("/api/auth/status".equals(path)) {
            User user = req.getCurrentUser();
            if (user != null) {
                resp.sendJson(200, Map.of("authenticated", true, "user", user.toSafeMap()));
            } else {
                resp.sendJson(200, Map.of("authenticated", false));
            }
            return;
        }

        if ("/api/auth/demo-accounts".equals(path)) {
            List<Map<String, String>> accounts = List.of(
                    Map.of("role", "ADMIN", "roleLabel", "System Administrator", "email", "admin@pmtool.local", "name", "Alex Rivera (Admin)"),
                    Map.of("role", "PROJECT_MANAGER", "roleLabel", "Lead Project Manager", "email", "sarah.pm@pmtool.local", "name", "Sarah Connor (Lead PM)"),
                    Map.of("role", "TEAM_MEMBER", "roleLabel", "Senior Backend Engineer", "email", "marcus.dev@pmtool.local", "name", "Marcus Vance (Backend)"),
                    Map.of("role", "TEAM_MEMBER", "roleLabel", "UI/UX & Frontend Engineer", "email", "elena.ui@pmtool.local", "name", "Elena Rostova (UI/UX)")
            );
            resp.sendJson(200, accounts);
            return;
        }

        resp.sendError(404, "Not found");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String path = req.getPath();

        if ("/api/auth/login".equals(path)) {
            Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());
            String email = (String) body.get("email");
            String password = (String) body.get("password");

            User user = authService.authenticate(email, password, req.getHeader("X-Forwarded-For"));
            String sessionId = authService.createSession(user);

            resp.setCookie(cookieName, sessionId, 86400, false, "/");
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("token", sessionId);
            res.put("user", user.toSafeMap());
            resp.sendJson(200, res);
            return;
        }

        if ("/api/auth/logout".equals(path)) {
            String token = req.getCookie(cookieName);
            if (token == null && req.getHeader("Authorization") != null) {
                token = req.getHeader("Authorization").replace("Bearer ", "").trim();
            }
            authService.invalidateSession(token);
            resp.setCookie(cookieName, "", 0, false, "/");
            resp.sendJson(200, Map.of("message", "Logged out successfully"));
            return;
        }

        if ("/api/auth/change-password".equals(path)) {
            User user = req.getCurrentUser();
            if (user == null) {
                resp.sendError(401, "Not authenticated");
                return;
            }
            Map<String, Object> body = JsonUtil.parseJsonObject(req.getBody());
            String oldPass = (String) body.get("oldPassword");
            String newPass = (String) body.get("newPassword");
            authService.changePassword(user.getId(), oldPass, newPass);
            resp.sendJson(200, Map.of("message", "Password changed successfully"));
            return;
        }

        resp.sendError(404, "Not found");
    }
}
