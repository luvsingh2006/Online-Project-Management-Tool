package com.pmtool.web.filter;

import com.pmtool.config.AppConfig;
import com.pmtool.model.User;
import com.pmtool.security.SecurityContext;
import com.pmtool.service.AuthService;
import com.pmtool.web.servlet.Filter;
import com.pmtool.web.servlet.FilterChain;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;

import java.util.Optional;
import java.util.Set;

public class AuthenticationFilter implements Filter {

    private final AuthService authService;
    private final String sessionCookieName;
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/status",
            "/api/auth/demo-accounts"
    );

    public AuthenticationFilter(AuthService authService) {
        this.authService = authService;
        this.sessionCookieName = AppConfig.getString("auth.session.cookieName", "PMTOOL_SESSION_ID");
    }

    @Override
    public void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws Exception {
        String path = req.getPath();

        if (!path.startsWith("/api/") || PUBLIC_PATHS.contains(path)) {
            chain.doFilter(req, resp);
            return;
        }

        String sessionId = resolveSessionId(req);
        if (sessionId != null) {
            Optional<User> userOpt = authService.validateSession(sessionId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                req.setCurrentUser(user);
                SecurityContext.setCurrentUser(user);
                try {
                    chain.doFilter(req, resp);
                } finally {
                    SecurityContext.clear();
                }
                return;
            }
        }

        resp.sendError(401, "Authentication required to access this resource");
    }

    private String resolveSessionId(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return req.getCookie(sessionCookieName);
    }
}
