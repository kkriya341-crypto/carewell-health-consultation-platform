package com.example.health;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Minimal Servlet API example hosted by Spring Boot's embedded Tomcat server.
 * The endpoint is intentionally read-only and returns a small service status response.
 */
class PlatformStatusServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"service\":\"Carewell Health API\",\"status\":\"available\"}");
    }
}
