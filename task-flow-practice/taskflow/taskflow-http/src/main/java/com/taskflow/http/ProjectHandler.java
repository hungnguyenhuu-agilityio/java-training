package com.taskflow.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.taskflow.domain.Project;
import com.taskflow.domain.ProjectHasTasksException;
import com.taskflow.domain.User;
import com.taskflow.service.ProjectService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * HTTP handler for {@code /projects} and {@code /projects/{id}}.
 *
 * <p>All routes are protected — {@link AuthFilter} must be attached to the context.
 * The authenticated {@link User} is retrieved from
 * {@code exchange.getAttribute(AuthFilter.ATTR_USER)}.
 *
 * <p>Route table:
 * <ul>
 *   <li>POST   /projects        → 201 + project JSON
 *   <li>GET    /projects        → 200 + JSON array
 *   <li>GET    /projects/{id}   → 200 / 404
 *   <li>PUT    /projects/{id}   → 200 / 404 / 403
 *   <li>DELETE /projects/{id}   → 204 / 404 / 403 / 409
 * </ul>
 */
public class ProjectHandler implements HttpHandler {

    private final ProjectService projectService;
    private final ObjectMapper mapper;

    public ProjectHandler(ProjectService projectService, ObjectMapper mapper) {
        this.projectService = projectService;
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        AuthHandler.addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // e.g. "/projects" or "/projects/42"

        if ("OPTIONS".equalsIgnoreCase(method)) {
            AuthHandler.sendResponse(exchange, 204, "");
            return;
        }

        // Extract optional {id} segment
        String idSegment = extractIdSegment(path); // null for collection routes

        if (idSegment == null) {
            // Collection routes: /projects
            switch (method.toUpperCase()) {
                case "POST" -> handleCreate(exchange);
                case "GET"  -> handleList(exchange);
                default     -> AuthHandler.sendError(exchange, 405, "Method not allowed");
            }
        } else {
            // Resource routes: /projects/{id}
            long id;
            try {
                id = Long.parseLong(idSegment);
            } catch (NumberFormatException e) {
                AuthHandler.sendError(exchange, 400, "Project id must be numeric");
                return;
            }

            switch (method.toUpperCase()) {
                case "GET"    -> handleGet(exchange, id);
                case "PUT"    -> handleUpdate(exchange, id);
                case "DELETE" -> handleDelete(exchange, id);
                default       -> AuthHandler.sendError(exchange, 405, "Method not allowed");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Route implementations
    // -------------------------------------------------------------------------

    private void handleCreate(HttpExchange exchange) throws IOException {
        User user = authenticatedUser(exchange);
        JsonNode body = parseBody(exchange);
        if (body == null) return;

        String name = textOrNull(body, "name");
        if (name == null) {
            AuthHandler.sendError(exchange, 400, "name is required and must not be blank");
            return;
        }

        String description = body.has("description") ? body.get("description").asText(null) : null;

        try {
            Project created = projectService.create(name, description, user.getId());
            AuthHandler.sendResponse(exchange, 201, mapper.writeValueAsString(toNode(created)));
        } catch (Exception e) {
            AuthHandler.sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        Pagination pagination = Pagination.from(exchange);
        List<ObjectNode> all = projectService.getAll().stream().map(this::toNode).toList();
        AuthHandler.sendResponse(exchange, 200, mapper.writeValueAsString(pagination.apply(all)));
    }

    private void handleGet(HttpExchange exchange, long id) throws IOException {
        Optional<Project> project = projectService.getById(id);
        if (project.isEmpty()) {
            AuthHandler.sendError(exchange, 404, "Project not found");
            return;
        }
        AuthHandler.sendResponse(exchange, 200, mapper.writeValueAsString(toNode(project.get())));
    }

    private void handleUpdate(HttpExchange exchange, long id) throws IOException {
        User user = authenticatedUser(exchange);
        Optional<Project> existing = projectService.getById(id);
        if (existing.isEmpty()) {
            AuthHandler.sendError(exchange, 404, "Project not found");
            return;
        }
        if (existing.get().getOwnerId() != user.getId()) {
            AuthHandler.sendError(exchange, 403, "Forbidden");
            return;
        }

        JsonNode body = parseBody(exchange);
        if (body == null) return;

        String name = textOrNull(body, "name");
        if (name == null) {
            AuthHandler.sendError(exchange, 400, "name is required and must not be blank");
            return;
        }

        String description = body.has("description") ? body.get("description").asText(null) : null;

        Project toUpdate = existing.get();
        toUpdate.setName(name);
        toUpdate.setDescription(description);
        projectService.update(toUpdate);

        AuthHandler.sendResponse(exchange, 200, mapper.writeValueAsString(toNode(toUpdate)));
    }

    private void handleDelete(HttpExchange exchange, long id) throws IOException {
        User user = authenticatedUser(exchange);
        Optional<Project> existing = projectService.getById(id);
        if (existing.isEmpty()) {
            AuthHandler.sendError(exchange, 404, "Project not found");
            return;
        }
        if (existing.get().getOwnerId() != user.getId()) {
            AuthHandler.sendError(exchange, 403, "Forbidden");
            return;
        }

        try {
            projectService.delete(id);
            AuthHandler.sendResponse(exchange, 204, "");
        } catch (ProjectHasTasksException e) {
            AuthHandler.sendError(exchange, 409, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the path segment after {@code /projects/}, or {@code null} for the
     * collection path {@code /projects} (with or without trailing slash).
     */
    private static String extractIdSegment(String path) {
        // Normalize trailing slash
        String normalised = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = normalised.lastIndexOf('/');
        String last = normalised.substring(lastSlash + 1);
        // "projects" is the collection; anything else is an id candidate
        return "projects".equals(last) ? null : last;
    }

    private JsonNode parseBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            if (bytes.length == 0) {
                AuthHandler.sendError(exchange, 400, "Request body is required");
                return null;
            }
            return mapper.readTree(bytes);
        } catch (Exception e) {
            AuthHandler.sendError(exchange, 400, "Invalid JSON body");
            return null;
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode f = node.get(field);
        if (f == null || f.isNull() || f.asText().isBlank()) return null;
        return f.asText().trim();
    }

    private User authenticatedUser(HttpExchange exchange) {
        return (User) exchange.getAttribute(AuthFilter.ATTR_USER);
    }

    private ObjectNode toNode(Project p) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", p.getId());
        node.put("name", p.getName());
        node.put("description", p.getDescription());
        node.put("ownerId", p.getOwnerId());
        if (p.getCreatedAt() != null) {
            node.put("createdAt", p.getCreatedAt().toString());
        }
        return node;
    }
}
