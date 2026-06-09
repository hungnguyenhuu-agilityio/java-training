package com.taskflow.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.taskflow.domain.Comment;
import com.taskflow.domain.User;
import com.taskflow.persistence.AttachmentDao;
import com.taskflow.service.CommentService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * HTTP handler for nested routes under {@code /tasks/{taskId}/...}.
 *
 * <p>Route table:
 * <ul>
 *   <li>POST   /tasks/{taskId}/comments              → 201 + comment JSON</li>
 *   <li>GET    /tasks/{taskId}/comments              → 200 + array JSON</li>
 *   <li>DELETE /tasks/{taskId}/comments/{commentId} → 204 (own) | 403 (other user)</li>
 *   <li>POST   /tasks/{taskId}/attachments           → 201 + attachment metadata JSON</li>
 *   <li>GET    /tasks/{taskId}/attachments           → 200 + attachment array JSON</li>
 * </ul>
 *
 * <p>Registered at {@code "/tasks/"} context (trailing slash) so {@link com.sun.net.httpserver.HttpServer}
 * longest-prefix matching routes it ahead of {@code "/tasks"} for sub-paths.
 * All routes require a valid Bearer token (enforced by {@link AuthFilter}).
 */
public class CommentHandler implements HttpHandler {

    private final CommentService commentService;
    private final AttachmentHandler attachmentHandler;
    private final ObjectMapper mapper;

    public CommentHandler(CommentService commentService, AttachmentDao attachmentDao,
                          ObjectMapper mapper) {
        this.commentService = commentService;
        this.attachmentHandler = new AttachmentHandler(attachmentDao, mapper);
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

        // Expected paths:
        //   /tasks/{taskId}/comments
        //   /tasks/{taskId}/comments/{commentId}
        // segments after split("/"):
        //   [0]="" [1]="tasks" [2]=taskId [3]="comments" [4]=commentId (optional)
        String rawPath = exchange.getRequestURI().getPath();
        String[] segments = rawPath.split("/");

        // Minimum: /tasks/{taskId}/comments or /tasks/{taskId}/attachments → 4 segments
        if (segments.length < 4) {
            AuthHandler.sendError(exchange, 404, "Not found");
            return;
        }

        long taskId;
        try {
            taskId = Long.parseLong(segments[2]);
        } catch (NumberFormatException e) {
            AuthHandler.sendError(exchange, 400, "Task id must be numeric");
            return;
        }

        // Dispatch attachment routes
        if ("attachments".equals(segments[3])) {
            attachmentHandler.handle(exchange, taskId);
            return;
        }

        if (!"comments".equals(segments[3])) {
            AuthHandler.sendError(exchange, 404, "Not found");
            return;
        }

        boolean hasCommentId = segments.length >= 5 && !segments[4].isBlank();

        if (hasCommentId) {
            long commentId;
            try {
                commentId = Long.parseLong(segments[4]);
            } catch (NumberFormatException e) {
                AuthHandler.sendError(exchange, 400, "Comment id must be numeric");
                return;
            }
            handleWithCommentId(exchange, method, taskId, commentId);
        } else {
            handleCollection(exchange, method, taskId);
        }
    }

    // -------------------------------------------------------------------------
    // Collection: POST /tasks/{taskId}/comments, GET /tasks/{taskId}/comments
    // -------------------------------------------------------------------------

    private void handleCollection(HttpExchange exchange, String method, long taskId)
            throws IOException {
        switch (method.toUpperCase()) {
            case "POST" -> handleCreate(exchange, taskId);
            case "GET"  -> handleList(exchange, taskId);
            default     -> AuthHandler.sendError(exchange, 405, "Method not allowed");
        }
    }

    private void handleCreate(HttpExchange exchange, long taskId) throws IOException {
        User user = authenticatedUser(exchange);

        JsonNode body = parseBody(exchange);
        if (body == null) return;

        String commentBody = textOrNull(body, "body");
        if (commentBody == null) {
            AuthHandler.sendError(exchange, 400, "body is required");
            return;
        }

        try {
            Comment created = commentService.add(commentBody, taskId, user.getId());
            AuthHandler.sendResponse(exchange, 201, mapper.writeValueAsString(created));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("Task not found")) {
                AuthHandler.sendError(exchange, 404, msg);
            } else {
                AuthHandler.sendError(exchange, 400, msg);
            }
        }
    }

    private void handleList(HttpExchange exchange, long taskId) throws IOException {
        List<Comment> comments = commentService.list(taskId);
        AuthHandler.sendResponse(exchange, 200, mapper.writeValueAsString(comments));
    }

    // -------------------------------------------------------------------------
    // Item: DELETE /tasks/{taskId}/comments/{commentId}
    // -------------------------------------------------------------------------

    private void handleWithCommentId(HttpExchange exchange, String method,
                                     long taskId, long commentId) throws IOException {
        switch (method.toUpperCase()) {
            case "DELETE" -> handleDelete(exchange, commentId);
            default       -> AuthHandler.sendError(exchange, 405, "Method not allowed");
        }
    }

    private void handleDelete(HttpExchange exchange, long commentId) throws IOException {
        User user = authenticatedUser(exchange);

        try {
            commentService.delete(commentId, user.getId());
            AuthHandler.sendResponse(exchange, 204, "");
        } catch (IllegalStateException e) {
            // "Forbidden" — non-author trying to delete
            AuthHandler.sendError(exchange, 403, e.getMessage());
        } catch (IllegalArgumentException e) {
            AuthHandler.sendError(exchange, 404, e.getMessage());
        }
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
}
