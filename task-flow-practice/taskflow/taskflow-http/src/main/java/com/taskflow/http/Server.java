package com.taskflow.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import com.taskflow.persistence.AttachmentDao;
import com.taskflow.persistence.CommentDao;
import com.taskflow.persistence.DbConnection;
import com.taskflow.persistence.ProjectDao;
import com.taskflow.persistence.TaskDao;
import com.taskflow.persistence.UserDao;
import com.taskflow.service.CommentService;
import com.taskflow.service.NotificationScheduler;
import com.taskflow.service.ProjectService;
import com.taskflow.service.StatsService;
import com.taskflow.service.TaskService;
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

        ProjectDao projectDao = new ProjectDao(db);
        ProjectService projectService = new ProjectService(projectDao);

        TaskDao taskDao = new TaskDao(db);
        TaskService taskService = new TaskService(taskDao);

        CommentDao commentDao = new CommentDao(db);
        CommentService commentService = new CommentService(commentDao, taskDao);

        AttachmentDao attachmentDao = new AttachmentDao(db);

        StatsService statsService = new StatsService(taskDao);

        NotificationScheduler notificationScheduler = new NotificationScheduler(taskDao, 60);
        notificationScheduler.start();

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        // 3. Create HttpServer — MUST use FixedThreadPool, not the default single-threaded executor
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(THREAD_POOL_SIZE));

        // 4. Register public auth endpoints (no filter)
        server.createContext("/auth", new AuthHandler(userService, mapper));

        // 4b. Register public API documentation (Swagger UI + OpenAPI spec, no filter)
        SwaggerHandler swaggerHandler = new SwaggerHandler();
        server.createContext("/swagger", swaggerHandler);
        server.createContext("/openapi.yaml", swaggerHandler);

        // 5. Register protected endpoints with AuthFilter
        AuthFilter authFilter = new AuthFilter(tokenStore);

        var projectsCtx = server.createContext("/projects", new ProjectHandler(projectService, mapper));
        projectsCtx.getFilters().add(authFilter);

        CommentHandler commentHandler = new CommentHandler(commentService, attachmentDao, mapper);
        var tasksCtx = server.createContext("/tasks", new TaskHandler(taskService, mapper, commentHandler));
        tasksCtx.getFilters().add(authFilter);

        var dashboardCtx = server.createContext("/dashboard", new DashboardHandler(statsService, mapper));
        dashboardCtx.getFilters().add(authFilter);

        // 6. JVM shutdown hook: stop server and close DB pool
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            notificationScheduler.shutdown();
            server.stop(1);
            db.close();
            log.info("Server stopped.");
        }, "server-shutdown"));

        server.start();
        log.info("TaskFlow server started on http://localhost:" + PORT);
        log.info("API docs (Swagger UI) at http://localhost:" + PORT + "/swagger");
    }
}
