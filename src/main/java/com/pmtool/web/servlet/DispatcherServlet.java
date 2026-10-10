package com.pmtool.web.servlet;

import com.pmtool.exception.AppException;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DispatcherServlet {

    private static final Logger LOGGER = Logger.getLogger(DispatcherServlet.class.getName());

    private final Map<RoutePattern, HttpServlet> routes = new LinkedHashMap<>();
    private final List<Filter> filters = new ArrayList<>();
    private String staticResourceBase = "src/main/webapp";

    public void registerServlet(String pathPattern, HttpServlet servlet) {
        routes.put(new RoutePattern(pathPattern), servlet);
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public void setStaticResourceBase(String path) {
        this.staticResourceBase = path;
    }

    public void dispatch(HttpServletRequest req, HttpServletResponse resp) {
        String path = req.getPath();

        try {
            for (Map.Entry<RoutePattern, HttpServlet> entry : routes.entrySet()) {
                Map<String, String> params = new HashMap<>();
                if (entry.getKey().matches(path, params)) {
                    for (Map.Entry<String, String> p : params.entrySet()) {
                        req.setPathParameter(p.getKey(), p.getValue());
                    }

                    FilterChain chain = new FilterChain(new ArrayList<>(filters), entry.getValue());
                    chain.doFilter(req, resp);
                    return;
                }
            }

            if ("GET".equalsIgnoreCase(req.getMethod()) || "HEAD".equalsIgnoreCase(req.getMethod())) {
                serveStaticFile(path, resp);
                return;
            }

            resp.sendError(404, "Endpoint not found: " + path);
        } catch (AppException ae) {
            try {
                resp.sendError(ae.getHttpStatus(), ae.getMessage());
            } catch (IOException ignored) {}
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Internal dispatch error: " + path, e);
            try {
                resp.sendError(500, "Internal Server Error: " + e.getMessage());
            } catch (IOException ignored) {}
        }
    }

    private void serveStaticFile(String requestPath, HttpServletResponse resp) throws IOException {
        String cleanPath = requestPath.equals("/") ? "/index.html" : requestPath;
        cleanPath = URLDecoder.decode(cleanPath, StandardCharsets.UTF_8);

        File file = new File(staticResourceBase, cleanPath.startsWith("/") ? cleanPath.substring(1) : cleanPath);
        InputStream in = null;

        if (file.exists() && file.isFile()) {
            in = new FileInputStream(file);
        } else {
            String cpPath = "webapp" + (cleanPath.startsWith("/") ? cleanPath : "/" + cleanPath);
            in = getClass().getClassLoader().getResourceAsStream(cpPath);
            if (in == null && !requestPath.startsWith("/api/")) {
                File indexFile = new File(staticResourceBase, "index.html");
                if (indexFile.exists()) {
                    in = new FileInputStream(indexFile);
                    cleanPath = "/index.html";
                }
            }
        }

        if (in == null) {
            resp.sendError(404, "File not found: " + requestPath);
            return;
        }

        String mimeType = determineMimeType(cleanPath);
        resp.setContentType(mimeType);
        resp.setHeader("Cache-Control", "no-cache");
        resp.setStatus(200);

        try (InputStream stream = in; OutputStream out = resp.getOutputStream()) {
            stream.transferTo(out);
        }
    }

    private String determineMimeType(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
