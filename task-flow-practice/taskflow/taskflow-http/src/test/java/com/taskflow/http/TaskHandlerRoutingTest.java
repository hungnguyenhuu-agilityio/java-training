package com.taskflow.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.taskflow.domain.Comment;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;
import com.taskflow.domain.User;
import com.taskflow.persistence.AttachmentDao;
import com.taskflow.service.CommentService;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Regression tests for TaskHandler routing.
 *
 * Before the fix, GET /tasks/42 was captured by the "/tasks/" context and
 * routed to CommentHandler, which returned 404 because segments.length < 4.
 * After the fix, TaskHandler owns the "/tasks" context exclusively and
 * delegates sub-paths to CommentHandler internally.
 */
class TaskHandlerRoutingTest {

    private StubTaskService taskService;
    private ObjectMapper mapper;
    private CommentHandler commentHandler;

    @BeforeEach
    void setUp() {
        taskService = new StubTaskService();
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        commentHandler = new CommentHandler(new StubCommentService(), new StubAttachmentDao(), mapper);
    }

    /**
     * Regression: GET /tasks/42 must reach TaskHandler and return 200, not the
     * spurious 404 that was produced when CommentHandler received the request and
     * rejected it (segments.length < 4).
     */
    @Test
    void getTaskById_routesToTaskHandler_returns200() throws IOException {
        Task task = new Task();
        task.setId(42L);
        task.setTitle("Test task");
        taskService.stubbedTask = task;

        TaskHandler handler = new TaskHandler(taskService, mapper, commentHandler);

        FakeExchange exchange = new FakeExchange("GET", "/tasks/42");
        setUser(exchange, 1L);

        handler.handle(exchange);

        assertEquals(200, exchange.responseCode,
                "GET /tasks/42 should return 200 from TaskHandler, not the spurious 404 from CommentHandler");
        assertFalse(exchange.responseBody.toString().isBlank());
    }

    /**
     * GET /tasks/{id} where the task does not exist → 404 from TaskHandler's own
     * "not found" logic — not the spurious 404 from CommentHandler.
     */
    @Test
    void getTaskById_notFound_returns404FromTaskHandler() throws IOException {
        taskService.stubbedTask = null;

        TaskHandler handler = new TaskHandler(taskService, mapper, commentHandler);

        FakeExchange exchange = new FakeExchange("GET", "/tasks/99");
        setUser(exchange, 1L);

        handler.handle(exchange);

        assertEquals(404, exchange.responseCode);
        // The body should contain "Task not found", confirming it came from TaskHandler
        assertFalse(exchange.responseBody.toString().contains("Not found") &&
                        !exchange.responseBody.toString().contains("Task not found"),
                "404 should originate from TaskHandler, not CommentHandler");
    }

    /**
     * PUT /tasks/{id} where the task is DONE must return 409 and must NOT call updateTask.
     */
    @Test
    void updateTask_whenStatusIsDone_returns409() throws IOException {
        Task done = new Task();
        done.setId(10L);
        done.setTitle("Finished task");
        done.setStatus(TaskStatus.DONE);
        done.setUserId(1L);
        taskService.stubbedTask = done;

        TaskHandler handler = new TaskHandler(taskService, mapper, commentHandler);

        FakeExchange exchange = new FakeExchange("PUT", "/tasks/10",
                "{\"title\":\"new title\"}");
        setUser(exchange, 1L);

        handler.handle(exchange);

        assertEquals(409, exchange.responseCode);
        assertFalse(taskService.updateTaskCalled,
                "updateTask must not be called for a DONE task");
    }

    /**
     * PUT /tasks/{id} where the task is IN_PROGRESS must succeed (200).
     */
    @Test
    void updateTask_whenStatusIsNotDone_returns200() throws IOException {
        Task inProgress = new Task();
        inProgress.setId(11L);
        inProgress.setTitle("Active task");
        inProgress.setStatus(TaskStatus.IN_PROGRESS);
        inProgress.setUserId(1L);
        taskService.stubbedTask = inProgress;

        TaskHandler handler = new TaskHandler(taskService, mapper, commentHandler);

        FakeExchange exchange = new FakeExchange("PUT", "/tasks/11",
                "{\"title\":\"updated title\"}");
        setUser(exchange, 1L);

        handler.handle(exchange);

        assertEquals(200, exchange.responseCode);
    }

    /**
     * GET /tasks/42/comments must be delegated to CommentHandler (returns 200 + empty array).
     * TaskService.getTaskById must NOT be consulted for sub-resource paths.
     */
    @Test
    void getTaskComments_delegatesToCommentHandler_returns200() throws IOException {
        TaskHandler handler = new TaskHandler(taskService, mapper, commentHandler);

        FakeExchange exchange = new FakeExchange("GET", "/tasks/42/comments");
        setUser(exchange, 1L);

        handler.handle(exchange);

        assertEquals(200, exchange.responseCode);
        assertFalse(taskService.getByIdCalled,
                "TaskService.getTaskById must not be called when routing /tasks/{id}/comments");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void setUser(FakeExchange exchange, long userId) {
        User user = new User();
        user.setId(userId);
        exchange.setAttribute(AuthFilter.ATTR_USER, user);
    }

    // -------------------------------------------------------------------------
    // Stubs
    // -------------------------------------------------------------------------

    static class StubTaskService extends TaskService {
        Task stubbedTask;
        boolean getByIdCalled = false;
        boolean updateTaskCalled = false;

        StubTaskService() { super(null); }

        @Override
        public Optional<Task> getTaskById(long id) {
            getByIdCalled = true;
            return Optional.ofNullable(stubbedTask);
        }

        @Override
        public Task updateTask(Task task) {
            updateTaskCalled = true;
            return task;
        }

        @Override
        public List<Task> getTasks(Map<String, String> filters) { return List.of(); }
    }

    static class StubCommentService extends CommentService {
        StubCommentService() { super(null, null); }

        @Override
        public List<Comment> list(long taskId) { return List.of(); }
    }

    static class StubAttachmentDao extends AttachmentDao {
        StubAttachmentDao() { super(null); }
    }

    // -------------------------------------------------------------------------
    // Minimal HttpExchange stub
    // -------------------------------------------------------------------------

    static class FakeExchange extends HttpExchange {
        private final String method;
        private final String path;
        private final byte[] requestBody;
        private final Map<String, Object> attrs = new HashMap<>();
        int responseCode = -1;
        final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        private final com.sun.net.httpserver.Headers reqHeaders = new com.sun.net.httpserver.Headers();
        private final com.sun.net.httpserver.Headers resHeaders = new com.sun.net.httpserver.Headers();

        FakeExchange(String method, String path) {
            this(method, path, "");
        }

        FakeExchange(String method, String path, String body) {
            this.method = method;
            this.path = path;
            this.requestBody = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override public String getRequestMethod() { return method; }
        @Override public URI getRequestURI() {
            try { return new URI(path); } catch (Exception e) { throw new RuntimeException(e); }
        }
        @Override public com.sun.net.httpserver.Headers getRequestHeaders() { return reqHeaders; }
        @Override public com.sun.net.httpserver.Headers getResponseHeaders() { return resHeaders; }
        @Override public java.io.InputStream getRequestBody() { return new ByteArrayInputStream(requestBody); }
        @Override public java.io.OutputStream getResponseBody() { return responseBody; }
        @Override public void sendResponseHeaders(int code, long length) { this.responseCode = code; }
        @Override public void close() {}
        @Override public Object getAttribute(String name) { return attrs.get(name); }
        @Override public void setAttribute(String name, Object value) { attrs.put(name, value); }
        @Override public com.sun.net.httpserver.HttpContext getHttpContext() { return null; }
        @Override public InetSocketAddress getRemoteAddress() { return null; }
        @Override public InetSocketAddress getLocalAddress() { return null; }
        @Override public String getProtocol() { return "HTTP/1.1"; }
        @Override public void setStreams(java.io.InputStream i, java.io.OutputStream o) {}
        @Override public HttpPrincipal getPrincipal() { return null; }
        @Override public int getResponseCode() { return responseCode; }
    }
}
