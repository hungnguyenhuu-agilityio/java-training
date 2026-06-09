package com.taskflow.persistence;

import com.taskflow.domain.Comment;
import com.taskflow.domain.Task;
import com.taskflow.domain.User;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link CommentDao}.
 *
 * <p>Requires Docker MySQL on localhost:3307 with V1__init.sql applied.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommentDaoTest {

    private static final String TEST_EMAIL = "comment_dao_test@example.com";
    private static DbConnection db;
    private static UserDao userDao;
    private static TaskDao taskDao;
    private static CommentDao commentDao;
    private static long authorId;
    private static long taskId;

    @BeforeAll
    static void setUpAll() throws SQLException {
        db = DbConnection.getInstance();
        userDao = new UserDao(db);
        taskDao = new TaskDao(db);
        commentDao = new CommentDao(db);

        userDao.deleteByEmailForTest(TEST_EMAIL);

        User author = new User(0, "CommentDaoUser", TEST_EMAIL, "$2a$10$testhash", Instant.now());
        userDao.insert(author);
        authorId = userDao.findByEmail(TEST_EMAIL).orElseThrow().getId();

        Task t = new Task();
        t.setTitle("Comment DAO Test Task");
        t.setUserId(authorId);
        t.setCreatedAt(Instant.now());
        Task saved = taskDao.insert(t);
        taskId = saved.getId();
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        // delete comments then task then user
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM comments WHERE task_id = ?")) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM tasks WHERE id = ?")) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }
        userDao.deleteByEmailForTest(TEST_EMAIL);
    }

    @BeforeEach
    void cleanComments() throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM comments WHERE task_id = ?")) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------
    // insert
    // ---------------------------------------------------------

    @Test
    @DisplayName("insert() persists a comment and returns auto-generated id")
    void insert_persistsComment() {
        Comment c = new Comment(0, "Hello World", taskId, authorId, Instant.now());
        Comment saved = commentDao.insert(c);

        assertTrue(saved.getId() > 0, "auto-generated id must be > 0");
        assertEquals("Hello World", saved.getBody());
        assertEquals(taskId, saved.getTaskId());
        assertEquals(authorId, saved.getAuthorId());
    }

    // ---------------------------------------------------------
    // findByTaskId
    // ---------------------------------------------------------

    @Test
    @DisplayName("findByTaskId() returns all comments for a task")
    void findByTaskId_returnsComments() {
        commentDao.insert(new Comment(0, "First", taskId, authorId, Instant.now()));
        commentDao.insert(new Comment(0, "Second", taskId, authorId, Instant.now()));

        List<Comment> comments = commentDao.findByTaskId(taskId);

        assertEquals(2, comments.size());
    }

    @Test
    @DisplayName("findByTaskId() returns empty list when no comments exist")
    void findByTaskId_noComments_returnsEmpty() {
        List<Comment> comments = commentDao.findByTaskId(taskId);
        assertTrue(comments.isEmpty());
    }

    // ---------------------------------------------------------
    // delete
    // ---------------------------------------------------------

    @Test
    @DisplayName("delete() removes the comment by id")
    void delete_removesComment() {
        Comment saved = commentDao.insert(new Comment(0, "To Delete", taskId, authorId, Instant.now()));

        commentDao.delete(saved.getId());

        List<Comment> remaining = commentDao.findByTaskId(taskId);
        assertTrue(remaining.stream().noneMatch(c -> c.getId() == saved.getId()));
    }

    @Test
    @DisplayName("findById() returns the comment that was inserted")
    void findById_returnsComment() {
        Comment saved = commentDao.insert(new Comment(0, "Find Me", taskId, authorId, Instant.now()));

        java.util.Optional<Comment> found = commentDao.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Find Me", found.get().getBody());
        assertEquals(authorId, found.get().getAuthorId());
    }

    @Test
    @DisplayName("findById() returns empty for unknown id")
    void findById_unknownId_returnsEmpty() {
        assertTrue(commentDao.findById(Long.MAX_VALUE).isEmpty());
    }
}
