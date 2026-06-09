package com.taskflow.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.taskflow.domain.StatsDto;
import com.taskflow.domain.User;
import com.taskflow.service.StatsService;

import java.io.IOException;

/**
 * HTTP handler for {@code GET /dashboard/stats}.
 *
 * <p>Returns task count aggregations by status and priority as JSON.
 * Requires a valid Bearer token (enforced by {@link AuthFilter}).
 *
 * <p>Route table:
 * <ul>
 *   <li>GET /dashboard/stats → 200 + {@link StatsDto} JSON</li>
 * </ul>
 */
public class DashboardHandler implements HttpHandler {

    private final StatsService statsService;
    private final ObjectMapper mapper;

    public DashboardHandler(StatsService statsService, ObjectMapper mapper) {
        this.statsService = statsService;
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        AuthHandler.addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            AuthHandler.sendResponse(exchange, 204, "");
            return;
        }

        String path = exchange.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method) && path.equals("/dashboard/stats")) {
            // Authenticated user is available via attribute (set by AuthFilter)
            User user = (User) exchange.getAttribute(AuthFilter.ATTR_USER);
            if (user == null) {
                AuthHandler.sendError(exchange, 401, "Unauthorized");
                return;
            }

            StatsDto stats = statsService.getStats();
            String json = mapper.writeValueAsString(stats);
            AuthHandler.sendResponse(exchange, 200, json);
        } else {
            AuthHandler.sendError(exchange, 404, "Not Found");
        }
    }
}
