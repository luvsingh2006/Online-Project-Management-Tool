package com.pmtool.web.servlet;

import java.util.List;

public class FilterChain {
    private final List<Filter> filters;
    private final HttpServlet targetServlet;
    private int index = 0;

    public FilterChain(List<Filter> filters, HttpServlet targetServlet) {
        this.filters = filters;
        this.targetServlet = targetServlet;
    }

    public void doFilter(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (index < filters.size()) {
            Filter filter = filters.get(index++);
            filter.doFilter(req, resp, this);
        } else {
            targetServlet.service(req, resp);
        }
    }
}
