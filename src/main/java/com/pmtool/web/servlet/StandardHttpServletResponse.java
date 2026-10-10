package com.pmtool.web.servlet;

import com.pmtool.web.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StandardHttpServletResponse implements HttpServletResponse {

    private final HttpExchange exchange;
    private int status = 200;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private boolean headersSent = false;

    public StandardHttpServletResponse(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override public int getStatus() { return status; }
    @Override public void setStatus(int status) { this.status = status; }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
        exchange.getResponseHeaders().set(name, value);
    }

    @Override public void setContentType(String contentType) { setHeader("Content-Type", contentType); }

    @Override
    public void setCookie(String name, String value, int maxAgeSeconds, boolean httpOnly, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        if (maxAgeSeconds >= 0) sb.append("; Max-Age=").append(maxAgeSeconds);
        if (path != null) sb.append("; Path=").append(path);
        if (httpOnly) sb.append("; HttpOnly");
        sb.append("; SameSite=Lax");
        exchange.getResponseHeaders().add("Set-Cookie", sb.toString());
    }

    @Override
    public void sendJson(int status, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        setContentType("application/json; charset=UTF-8");
        this.status = status;
        exchange.sendResponseHeaders(status, bytes.length);
        headersSent = true;
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Override
    public void sendJson(int status, Object data) throws IOException {
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("status", status);
        wrapper.put("success", status >= 200 && status < 300);
        wrapper.put("data", data);
        sendJson(status, JsonUtil.toJson(wrapper));
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", status);
        error.put("success", false);
        error.put("error", message);
        sendJson(status, JsonUtil.toJson(error));
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        setHeader("Location", location);
        this.status = 302;
        exchange.sendResponseHeaders(302, -1);
        headersSent = true;
    }

    @Override
    public OutputStream getOutputStream() {
        if (!headersSent) {
            try {
                exchange.sendResponseHeaders(status, 0);
                headersSent = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return exchange.getResponseBody();
    }

    @Override public Map<String, String> getHeaders() { return Collections.unmodifiableMap(headers); }
}
