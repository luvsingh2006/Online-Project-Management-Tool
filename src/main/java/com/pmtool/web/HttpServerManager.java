package com.pmtool.web;

import com.pmtool.config.AppConfig;
import com.pmtool.web.servlet.DispatcherServlet;
import com.pmtool.web.servlet.StandardHttpServletRequest;
import com.pmtool.web.servlet.StandardHttpServletResponse;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class HttpServerManager {

    private static final Logger LOGGER = Logger.getLogger(HttpServerManager.class.getName());

    private final HttpServer server;
    private final DispatcherServlet dispatcherServlet;
    private final int port;

    public HttpServerManager(DispatcherServlet dispatcherServlet) throws IOException {
        this.dispatcherServlet = dispatcherServlet;
        this.port = AppConfig.getInt("server.port", 8080);
        String host = AppConfig.getString("server.host", "0.0.0.0");

        this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
        this.server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

        this.server.createContext("/", exchange -> {
            StandardHttpServletRequest req = new StandardHttpServletRequest(exchange);
            StandardHttpServletResponse resp = new StandardHttpServletResponse(exchange);

            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("X-Frame-Options", "DENY");
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");

            if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            this.dispatcherServlet.dispatch(req, resp);
        });

        LOGGER.info("HttpServerManager configured on port " + port);
    }

    public void start() {
        server.start();
        LOGGER.info("=================================================================");
        LOGGER.info("  Online Project Management Tool Server RUNNING on port " + port);
        LOGGER.info("  Access URL: http://localhost:" + port);
        LOGGER.info("=================================================================");
    }

    public void stop() {
        LOGGER.info("Stopping HTTP server...");
        server.stop(2);
    }

    public int getPort() { return port; }
}
