package com.taskflow.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import com.taskflow.persistence.DbConnection;
import com.taskflow.persistence.UserDao;
import com.taskflow.service.TokenStore;
import com.taskflow.service.UserService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Bootstraps the TaskFlow HTTP server on port 8080.
 *
 * <p>Key constraints (PROJECT_SPEC.md):
 * <ul>
 *   <li>Uses {@link Executors#newFixedThreadPool} — default single-threaded executor
 *       causes request queuing under load.</li>
 *   <li>Creates the {@code uploads/} directory on startup.</li>
 *   <li>CORS headers are applied by handlers / filters.</li>
 * </ul>
 */
public class Server {

    private static final Logger log = Logger.getLogger(Server.class.getName());
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) throws IOException {
        // 1. Create uploads/ directory if absent
        Path uploads = Path.of("uploads");
        if (!Files.exists(uploads)) {
            Files.createDirectories(uploads);
            log.info("Created uploads/ directory");
        }

        // 2. Wire dependencies
        DbConnection db = DbConnection.getInstance();
        UserDao userDao = new UserDao(db);
        TokenStore tokenStore = new TokenStore();
        UserService userService = new UserService(userDao, tokenStore);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        // 3. Create HttpServer — MUST use FixedThreadPool, not the default single-threaded executor
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));

        // 4. Register public auth endpoints (no filter)
        server.createContext("/auth", new AuthHandler(userService, mapper));

        // 5. Register a sample protected context to demonstrate AuthFilter
        //    (later tasks will add TaskHandler, ProjectHandler, etc.)
        HttpContext tasksContext = server.createContext("/tasks", exchange -> {
            AuthHandler.addCorsHeaders(exchange);
            AuthHandler.sendResponse(exchange, 200, "{\"tasks\":[]}");
        });
        tasksContext.getFilters().add(new AuthFilter(tokenStore));

        // 6. JVM shutdown hook: close DB pool
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
            db.close();
            log.info("Server stopped.");
        }, "server-shutdown"));

        server.start();
        log.info("TaskFlow server started on http://localhost:" + PORT);
    }
}
