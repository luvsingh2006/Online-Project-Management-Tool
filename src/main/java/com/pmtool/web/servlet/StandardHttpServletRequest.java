package com.pmtool.web.servlet;

import com.pmtool.model.User;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StandardHttpServletRequest implements HttpServletRequest {

    private final HttpExchange exchange;
    private final Map<String, String> pathParameters = new HashMap<>();
    private final Map<String, String> queryParameters = new LinkedHashMap<>();
    private final Map<String, Object> attributes = new HashMap<>();
    private String body;
    private User currentUser;

    public StandardHttpServletRequest(HttpExchange exchange) {
        this.exchange = exchange;
        parseQuery();
    }

    private void parseQuery() {
        URI uri = exchange.getRequestURI();
        String rawQuery = uri.getRawQuery();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String pair : rawQuery.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    String k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                    queryParameters.put(k, v);
                } else if (!pair.isBlank()) {
                    queryParameters.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
                }
            }
        }
    }

    @Override public String getMethod() { return exchange.getRequestMethod(); }
    @Override public String getPath() { return exchange.getRequestURI().getPath(); }
    @Override public String getQueryString() { return exchange.getRequestURI().getQuery(); }
    @Override public String getParameter(String name) { return queryParameters.get(name); }
    @Override public Map<String, String> getParameters() { return Collections.unmodifiableMap(queryParameters); }
    @Override public String getPathParameter(String name) { return pathParameters.get(name); }
    @Override public void setPathParameter(String name, String value) { pathParameters.put(name, value); }
    @Override public String getHeader(String name) { return exchange.getRequestHeaders().getFirst(name); }

    @Override
    public String getCookie(String name) {
        String cookieHeader = getHeader("Cookie");
        if (cookieHeader != null) {
            for (String c : cookieHeader.split(";")) {
                String[] p = c.trim().split("=");
                if (p.length == 2 && p[0].trim().equals(name)) {
                    return p[1].trim();
                }
            }
        }
        return null;
    }

    @Override
    public String getBody() {
        if (body == null) {
            try (InputStream in = exchange.getRequestBody()) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                body = "";
            }
        }
        return body;
    }

    @Override public InputStream getInputStream() { return exchange.getRequestBody(); }
    @Override public Object getAttribute(String name) { return attributes.get(name); }
    @Override public void setAttribute(String name, Object value) { attributes.put(name, value); }
    @Override public User getCurrentUser() { return currentUser; }
    @Override public void setCurrentUser(User user) { this.currentUser = user; }
}
