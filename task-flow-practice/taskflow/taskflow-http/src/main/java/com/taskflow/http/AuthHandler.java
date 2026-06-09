package com.taskflow.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.taskflow.domain.User;
import com.taskflow.service.UserService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * HTTP handler for {@code POST /auth/register} and {@code POST /auth/login}.
 *
 * <p>JSON in/out via Jackson. All error responses include a JSON {@code {"error":"..."}} body.
 */
public class AuthHandler implements HttpHandler {

    private final UserService userService;
    private final ObjectMapper mapper;

    public AuthHandler(UserService userService, ObjectMapper mapper) {
        this.userService = userService;
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendResponse(exchange, 204, "");
            return;
        }

        if (!("POST".equalsIgnoreCase(method))) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        if (path.endsWith("/auth/register")) {
            handleRegister(exchange);
        } else if (path.endsWith("/auth/login")) {
            handleLogin(exchange);
        } else {
            sendError(exchange, 404, "Not found");
        }
    }

    // -------------------------------------------------------------------------

    private void handleRegister(HttpExchange exchange) throws IOException {
        JsonNode body = parseBody(exchange);
        if (body == null) return;

        String name = textOrNull(body, "name");
        String email = textOrNull(body, "email");
        String password = textOrNull(body, "password");

        if (name == null || email == null || password == null) {
            sendError(exchange, 400, "name, email, and password are required");
            return;
        }

        try {
            User user = userService.register(name, email, password);
            ObjectNode resp = mapper.createObjectNode();
            resp.put("id", user.getId());
            resp.put("name", user.getName());
            resp.put("email", user.getEmail());
            sendResponse(exchange, 201, mapper.writeValueAsString(resp));
        } catch (IllegalArgumentException e) {
            sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            sendError(exchange, 500, "Internal server error");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        JsonNode body = parseBody(exchange);
        if (body == null) return;

        String email = textOrNull(body, "email");
        String password = textOrNull(body, "password");

        if (email == null || password == null) {
            sendError(exchange, 400, "email and password are required");
            return;
        }

        Optional<String> token = userService.login(email, password);
        if (token.isEmpty()) {
            sendError(exchange, 401, "Invalid credentials");
            return;
        }

        ObjectNode resp = mapper.createObjectNode();
        resp.put("token", token.get());
        sendResponse(exchange, 200, mapper.writeValueAsString(resp));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private JsonNode parseBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] bytes = is.readAllBytes();
            if (bytes.length == 0) {
                sendError(exchange, 400, "Request body is required");
                return null;
            }
            return mapper.readTree(bytes);
        } catch (Exception e) {
            sendError(exchange, 400, "Invalid JSON body");
            return null;
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode f = node.get(field);
        if (f == null || f.isNull() || f.asText().isBlank()) return null;
        return f.asText().trim();
    }

    static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:4200");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
    }

    static void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } else {
            exchange.getResponseBody().close();
        }
    }

    private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();

    static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        ObjectNode err = ERROR_MAPPER.createObjectNode();
        err.put("error", message);
        sendResponse(exchange, status, ERROR_MAPPER.writeValueAsString(err));
    }
}
