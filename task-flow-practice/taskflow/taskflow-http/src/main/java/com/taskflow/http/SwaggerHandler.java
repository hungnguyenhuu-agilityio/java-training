package com.taskflow.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves the OpenAPI specification and an interactive Swagger UI.
 *
 * <p>This is a public context (no {@link AuthFilter}) so the documentation is
 * reachable without a token. The UI itself supports the "Authorize" button so
 * protected endpoints can be exercised with a Bearer token.
 *
 * <p>Route table:
 * <ul>
 *   <li>GET /swagger          → Swagger UI HTML page</li>
 *   <li>GET /swagger/         → Swagger UI HTML page</li>
 *   <li>GET /openapi.yaml     → raw OpenAPI 3 spec (served from classpath)</li>
 * </ul>
 *
 * <p>The Swagger UI assets are loaded from a public CDN, so no extra Maven
 * dependency or bundled JavaScript is required.
 */
public class SwaggerHandler implements HttpHandler {

    private static final String SPEC_RESOURCE = "/openapi.yaml";
    private static final String SPEC_URL = "/openapi.yaml";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        AuthHandler.addCorsHeaders(exchange);

        String method = exchange.getRequestMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            AuthHandler.sendResponse(exchange, 204, "");
            return;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            AuthHandler.sendError(exchange, 405, "Method not allowed");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/openapi.yaml")) {
            serveSpec(exchange);
        } else {
            serveUi(exchange);
        }
    }

    private void serveSpec(HttpExchange exchange) throws IOException {
        try (InputStream in = SwaggerHandler.class.getResourceAsStream(SPEC_RESOURCE)) {
            if (in == null) {
                AuthHandler.sendError(exchange, 500, "OpenAPI spec not found on classpath");
                return;
            }
            byte[] bytes = in.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/yaml");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void serveUi(HttpExchange exchange) throws IOException {
        byte[] bytes = UI_HTML.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static final String UI_HTML = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>TaskFlow API — Swagger UI</title>
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
            </head>
            <body>
              <div id="swagger-ui"></div>
              <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
              <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-standalone-preset.js"></script>
              <script>
                window.onload = () => {
                  window.ui = SwaggerUIBundle({
                    url: '%s',
                    dom_id: '#swagger-ui',
                    deepLinking: true,
                    presets: [
                      SwaggerUIBundle.presets.apis,
                      SwaggerUIStandalonePreset
                    ],
                    layout: 'StandaloneLayout'
                  });
                };
              </script>
            </body>
            </html>
            """.formatted(SPEC_URL);
}
