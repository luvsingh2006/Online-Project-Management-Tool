package com.pmtool.web.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface HttpServletResponse {
    int getStatus();
    void setStatus(int status);
    void setHeader(String name, String value);
    void setContentType(String contentType);
    void setCookie(String name, String value, int maxAgeSeconds, boolean httpOnly, String path);
    void sendJson(int status, String jsonBody) throws IOException;
    void sendJson(int status, Object data) throws IOException;
    void sendError(int status, String message) throws IOException;
    void sendRedirect(String location) throws IOException;
    OutputStream getOutputStream();
    Map<String, String> getHeaders();
}
