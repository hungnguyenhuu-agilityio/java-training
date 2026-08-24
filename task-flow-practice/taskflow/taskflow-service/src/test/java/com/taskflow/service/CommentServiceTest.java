package com.taskflow.service;

import com.taskflow.domain.Comment;
import com.taskflow.domain.Task;
import com.taskflow.domain.CommentRepository;
import com.taskflow.domain.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CommentService}.
 *
 * <p>Uses in-memory stubs — no MySQL required.
 */
class CommentServiceTest {

    // ---- Stubs ----

    private static class StubCommentDao implements CommentRepository {
        private final Map<Long, Comment> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public Comment insert(Comment c) {
            c.setId(nextId++);
            store.put(c.getId(), c);
            return c;
        }

        @Override
        public List<Comment> findByTaskId(long taskId) {
            return store.values().stream()
                    .filter(c -> c.getTaskId() == taskId)
                    .toList();
        }

        @Override
        public void delete(long id) {
            store.remove(id);
        }

        @Override
        public Optional<Comment> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }
    }

    private static class StubTaskDao implements TaskRepository {
        private final Map<Long, Task> store = new LinkedHashMap<>();

        void put(Task t) { store.put(t.getId(), t); }

        @Override public Task insert(Task t) { return t; }
        @Override public List<Task> findAll() { return List.of(); }
        @Override public Optional<Task> findById(long id) { return Optional.ofNullable(store.get(id)); }
        @Override public void update(Task t) {}
        @Override public void delete(long id) {}
        @Override public List<Task> findDueSoon(java.time.LocalDate f, java.time.LocalDate t) { return List.of(); }
    }

    // ---- Setup ----

    private StubCommentDao commentDao;
    private StubTaskDao taskDao;
    private CommentService service;

    @BeforeEach
    void setUp() {
        commentDao = new StubCommentDao();
        taskDao = new StubTaskDao();

        Task t = new Task();
        t.setId(1L);
        t.setTitle("A Task");
        taskDao.put(t);

        service = new CommentService(commentDao, taskDao);
    }

    // ---------------------------------------------------------
    // add — happy path
    // ---------------------------------------------------------

    @Test
    @DisplayName("add() creates comment and returns it with generated id")
    void add_happyPath() {
        Comment c = service.add("Looks good!", 1L, 42L);

        assertTrue(c.getId() > 0);
        assertEquals("Looks good!", c.getBody());
        assertEquals(1L, c.getTaskId());
        assertEquals(42L, c.getAuthorId());
        assertNotNull(c.getCreatedAt());
    }

    // ---------------------------------------------------------
    // add — non-existent task → IllegalArgumentException
    // ---------------------------------------------------------

    @Test
    @DisplayName("add() to non-existent task throws IllegalArgumentException")
    void add_nonExistentTask_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.add("Comment", 9999L, 42L),
                "must throw when task does not exist");
    }

    // ---------------------------------------------------------
    // add — empty body → IllegalArgumentException
    // ---------------------------------------------------------

    @Test
    @DisplayName("add() with blank body throws IllegalArgumentException")
    void add_blankBody_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.add("  ", 1L, 42L));
    }

    // ---------------------------------------------------------
    // list
    // ---------------------------------------------------------

    @Test
    @DisplayName("list() returns all comments for a task")
    void list_returnsComments() {
        service.add("One", 1L, 42L);
        service.add("Two", 1L, 42L);

        List<Comment> comments = service.list(1L);

        assertEquals(2, comments.size());
    }

    // ---------------------------------------------------------
    // delete — own comment → ok
    // ---------------------------------------------------------

    @Test
    @DisplayName("delete() own comment succeeds silently")
    void delete_ownComment_succeeds() {
        Comment c = service.add("Mine", 1L, 42L);

        assertDoesNotThrow(() -> service.delete(c.getId(), 42L));

        assertTrue(service.list(1L).isEmpty());
    }

    // ---------------------------------------------------------
    // delete — other user's comment → IllegalStateException("Forbidden")
    // ---------------------------------------------------------

    @Test
    @DisplayName("delete() other user's comment throws IllegalStateException with 'Forbidden'")
    void delete_otherUsersComment_throwsForbidden() {
        Comment c = service.add("Not mine", 1L, 42L);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.delete(c.getId(), 99L),
                "must throw Forbidden when non-author tries to delete");

        assertEquals("Forbidden", ex.getMessage());
    }

    // ---------------------------------------------------------
    // delete — non-existent comment → IllegalArgumentException
    // ---------------------------------------------------------

    @Test
    @DisplayName("delete() non-existent comment throws IllegalArgumentException")
    void delete_nonExistentComment_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> service.delete(9999L, 42L));
    }
}
