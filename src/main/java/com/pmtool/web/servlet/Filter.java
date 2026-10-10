package com.pmtool.web.servlet;

public interface Filter {
    void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws Exception;
}
