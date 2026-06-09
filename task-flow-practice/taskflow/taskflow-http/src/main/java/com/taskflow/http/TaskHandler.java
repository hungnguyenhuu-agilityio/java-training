package com.taskflow.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.taskflow.domain.Priority;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;
import com.taskflow.domain.User;
import com.taskflow.service.TaskService;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP handler for {@code /tasks} and {@code /tasks/{id}}.
 *
 * <p>All routes require a valid Bearer token (enforced by {@link AuthFilter}).
 * The authenticated {@link User} is retrieved from the exchange attribute
 * {@link AuthFilter#ATTR_USER}.
 *
 * <p>Route table:
 * <ul>
 *   <li>POST   /tasks         → create task (201)</li>
 *   <li>GET    /tasks         → list / filter tasks (200)</li>
 *   <li>GET    /tasks/{id}    → get one task (200 / 404)</li>
 *   <li>PUT    /tasks/{id}    → update task (200 / 404)</li>
 *   <li>DELETE /tasks/{id}    → delete task (204 / 404)</li>
 * </ul>
 */
public class TaskHandler implements HttpHandler {

    private final TaskService taskService;
    private final ObjectMapper mapper;

    public TaskHandler(TaskService taskService, ObjectMapper mapper) {
        this.taskService = taskService;
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

        // Determine whether request is /tasks or /tasks/{id}
        String rawPath = exchange.getRequestURI().getPath(); // e.g. "/tasks" or "/tasks/42"
        String[] segments = rawPath.split("/");
        // segments[0]="" segments[1]="tasks" segments[2]=id (optional)
        boolean hasId = segments.length >= 3 && !segments[2].isBlank();

        if (hasId) {
            long id;
            try {
                id = Long.parseLong(segments[2]);
            } catch (NumberFormatException e) {
                AuthHandler.sendError(exchange, 400, "Task id must be numeric");
                return;
            }
            handleWithId(exchange, method, id);
        } else {
            handleCollection(exchange, method);
        }
    }

    // -------------------------------------------------------------------------
    // Collection: POST /tasks, GET /tasks
    // -------------------------------------------------------------------------

    private void handleCollection(HttpExchange exchange, String method) throws IOException {
        switch (method.toUpperCase()) {
            case "POST" -> handleCreate(exchange);
            case "GET"  -> handleList(exchange);
            default     -> AuthHandler.sendError(exchange, 405, "Method not allowed");
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        User user = authenticatedUser(exchange);

        JsonNode body = parseBody(exchange);
        if (body == null) return;

        String title = textOrNull(body, "title");
        if (title == null) {
            AuthHandler.sendError(exchange, 400, "title is required");
            return;
        }

        TaskStatus status = TaskStatus.TODO;
        String statusRaw = textOrNull(body, "status");
        if (statusRaw != null) {
            try {
                status = TaskStatus.valueOf(statusRaw.toUpperCase());
            } catch (IllegalArgumentException e) {
                AuthHandler.sendError(exchange, 400,
                        "Invalid status '" + statusRaw + "'; allowed: TODO, IN_PROGRESS, DONE");
                return;
            }
        }

        Priority priority = Priority.MEDIUM;
        String priorityRaw = textOrNull(body, "priority");
        if (priorityRaw != null) {
            try {
                priority = Priority.valueOf(priorityRaw.toUpperCase());
            } catch (IllegalArgumentException e) {
                AuthHandler.sendError(exchange, 400,
                        "Invalid priority '" + priorityRaw + "'; allowed: LOW, MEDIUM, HIGH");
                return;
            }
        }

        LocalDate dueDate = null;
        String dueDateRaw = textOrNull(body, "dueDate");
        if (dueDateRaw != null) {
            try {
                dueDate = LocalDate.parse(dueDateRaw);
            } catch (Exception e) {
                AuthHandler.sendError(exchange, 400, "Invalid dueDate format; expected ISO (yyyy-MM-dd)");
                return;
            }
        }

        Long projectId = null;
        JsonNode projectIdNode = body.get("projectId");
        if (projectIdNode != null && !projectIdNode.isNull()) {
            projectId = projectIdNode.asLong();
        }

        Task task = new Task();
        task.setTitle(title);
        task.setDescription(textOrNull(body, "description"));
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDate(dueDate);
        task.setProjectId(projectId);
        task.setUserId(user.getId());
        task.setCreatedAt(Instant.now());

        try {
            Task created = taskService.createTask(task);
            AuthHandler.sendResponse(exchange, 201, mapper.writeValueAsString(created));
        } catch (RuntimeException e) {
            // FK violation from DB (invalid projectId) → 400
            if (e.getCause() != null && e.getCause().getMessage() != null
                    && e.getCause().getMessage().toLowerCase().contains("foreign key")) {
                AuthHandler.sendError(exchange, 400, "Invalid projectId: referenced project does not exist");
            } else {
                AuthHandler.sendError(exchange, 500, "Internal server error");
            }
        }
    }

    private void handleList(HttpExchange exchange) throws IOException {
        // parse query params
        Map<String, String> filters = parseQueryParams(exchange);

        try {
            List<Task> tasks = taskService.getTasks(filters);
            AuthHandler.sendResponse(exchange, 200, mapper.writeValueAsString(tasks));
        } catch (IllegalArgumentException e) {
            AuthHandler.sendError(exchange, 400, e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Item: GET/PUT/DELETE /tasks/{id}
    // -------------------------------------------------------------------------

    private void handleWithId(HttpExchange exchange, String method, long id) throws IOException {
        switch (method.toUpperCase()) {
            case "GET"    -> handleGet(exchange, id);
            case "PUT"    -> handleUpdate(exchange, id);
            case "DELETE" -> handleDelete(exchange, id);
            default       -> AuthHandler.sendError(exchange, 405, "Method not allowed");
        }
    }

    private void handleGet(HttpExchange exchange, long id) throws IOException {
        Optional<Task> task = taskService.getTaskById(id);
        if (task.isEmpty()) {
            AuthHandler.sendError(exchange, 404, "Task not found: " + id);
            return;
        }
        AuthHandler.sendResponse(exchange, 200, mapper.writeValueAsString(task.get()));
    }

    private void handleUpdate(HttpExchange exchange, long id) throws IOException {
        User user = authenticatedUser(exchange);

        Optional<Task> existing = taskService.getTaskById(id);
        if (existing.isEmpty()) {
            AuthHandler.sendError(exchange, 404, "Task not found: " + id);
            return;
        }

        // Ownership check
        Task current = existing.get();
        if (current.getUserId() != null && !current.getUserId().equals(user.getId())) {
            AuthHandler.sendError(exchange, 403, "Forbidden: task belongs to another user");
            return;
        }

        JsonNode body = parseBody(exchange);
        if (body == null) return;

        String title = textOrNull(body, "title");
        if (title != null) current.setTitle(title);

        String description = textOrNull(body, "description");
        if (description != null) current.setDescription(description);

        String statusRaw = textOrNull(body, "status");
        if (statusRaw != null) {
            try {
                current.setStatus(TaskStatus.valueOf(statusRaw.toUpperCase()));
            } catch (IllegalArgumentException e) {
                AuthHandler.sendError(exchange, 400,
                        "Invalid status '" + statusRaw + "'; allowed: TODO, IN_PROGRESS, DONE");
                return;
            }
        }

        String priorityRaw = textOrNull(body, "priority");
        if (priorityRaw != null) {
            try {
                current.setPriority(Priority.valueOf(priorityRaw.toUpperCase()));
            } catch (IllegalArgumentException e) {
                AuthHandler.sendError(exchange, 400,
                        "Invalid priority '" + priorityRaw + "'; allowed: LOW, MEDIUM, HIGH");
                return;
            }
        }

        String dueDateRaw = textOrNull(body, "dueDate");
        if (dueDateRaw != null) {
            try {
                current.setDueDate(LocalDate.parse(dueDateRaw));
            } catch (Exception e) {
                AuthHandler.sendError(exchange, 400, "Invalid dueDate format; expected ISO (yyyy-MM-dd)");
                return;
            }
        }

        JsonNode projectIdNode = body.get("projectId");
        if (projectIdNode != null && !projectIdNode.isNull()) {
            current.setProjectId(projectIdNode.asLong());
        }

        try {
            Task updated = taskService.updateTask(current);
            AuthHandler.sendResponse(exchange, 200, mapper.writeValueAsString(updated));
        } catch (IllegalArgumentException e) {
            AuthHandler.sendError(exchange, 404, e.getMessage());
        } catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause().getMessage() != null
                    && e.getCause().getMessage().toLowerCase().contains("foreign key")) {
                AuthHandler.sendError(exchange, 400, "Invalid projectId: referenced project does not exist");
            } else {
                AuthHandler.sendError(exchange, 500, "Internal server error");
            }
        }
    }

    private void handleDelete(HttpExchange exchange, long id) throws IOException {
        User user = authenticatedUser(exchange);

        Optional<Task> existing = taskService.getTaskById(id);
        if (existing.isEmpty()) {
            AuthHandler.sendError(exchange, 404, "Task not found: " + id);
            return;
        }

        // Ownership check
        Task task = existing.get();
        if (task.getUserId() != null && !task.getUserId().equals(user.getId())) {
            AuthHandler.sendError(exchange, 403, "Forbidden: task belongs to another user");
            return;
        }

        taskService.deleteTask(id);
        AuthHandler.sendResponse(exchange, 204, "");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User authenticatedUser(HttpExchange exchange) {
        return (User) exchange.getAttribute(AuthFilter.ATTR_USER);
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

    /**
     * Parses {@code key=value&...} query string into a map.
     */
    private static Map<String, String> parseQueryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0].trim(), kv[1].trim());
            }
        }
        return params;
    }
}
