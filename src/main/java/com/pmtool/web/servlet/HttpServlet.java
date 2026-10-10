package com.pmtool.web.servlet;

import java.io.IOException;

public abstract class HttpServlet {

    public void service(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String method = req.getMethod().toUpperCase();
        switch (method) {
            case "GET" -> doGet(req, resp);
            case "POST" -> doPost(req, resp);
            case "PUT" -> doPut(req, resp);
            case "DELETE" -> doDelete(req, resp);
            case "OPTIONS" -> doOptions(req, resp);
            default -> resp.sendError(405, "Method Not Allowed: " + method);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws Exception { resp.sendError(405, "GET not supported"); }
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws Exception { resp.sendError(405, "POST not supported"); }
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws Exception { resp.sendError(405, "PUT not supported"); }
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws Exception { resp.sendError(405, "DELETE not supported"); }
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setHeader("Allow", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setStatus(204);
    }
}
