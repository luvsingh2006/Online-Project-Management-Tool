package com.pmtool.web.servlet;

import com.pmtool.model.User;
import java.io.InputStream;
import java.util.Map;

public interface HttpServletRequest {
    String getMethod();
    String getPath();
    String getQueryString();
    String getParameter(String name);
    Map<String, String> getParameters();
    String getPathParameter(String name);
    void setPathParameter(String name, String value);
    String getHeader(String name);
    String getCookie(String name);
    String getBody();
    InputStream getInputStream();
    Object getAttribute(String name);
    void setAttribute(String name, Object value);
    User getCurrentUser();
    void setCurrentUser(User user);
}
