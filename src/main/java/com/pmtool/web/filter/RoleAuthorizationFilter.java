package com.pmtool.web.filter;

import com.pmtool.model.Role;
import com.pmtool.model.User;
import com.pmtool.web.servlet.Filter;
import com.pmtool.web.servlet.FilterChain;
import com.pmtool.web.servlet.HttpServletRequest;
import com.pmtool.web.servlet.HttpServletResponse;

public class RoleAuthorizationFilter implements Filter {

    @Override
    public void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws Exception {
        String path = req.getPath();
        User currentUser = req.getCurrentUser();

        if (path.startsWith("/api/admin/")) {
            if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
                resp.sendError(403, "Access Denied: Administrator role required");
                return;
            }
        }

        if (path.startsWith("/api/manager/")) {
            if (currentUser == null || (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.PROJECT_MANAGER)) {
                resp.sendError(403, "Access Denied: Project Manager role required");
                return;
            }
        }

        chain.doFilter(req, resp);
    }
}
