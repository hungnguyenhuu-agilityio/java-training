package com.taskflow.persistence;

import com.taskflow.domain.Attachment;
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
 * Integration tests for {@link AttachmentDao}.
 *
 * <p>Requires Docker MySQL on localhost:3307 with V1__init.sql applied.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AttachmentDaoTest {

    private static final String TEST_EMAIL = "attachment_dao_test@example.com";
    private static DbConnection db;
    private static UserDao userDao;
    private static TaskDao taskDao;
    private static AttachmentDao attachmentDao;
    private static long uploaderId;
    private static long taskId;

    @BeforeAll
    static void setUpAll() throws SQLException {
        db = DbConnection.getInstance();
        userDao = new UserDao(db);
        taskDao = new TaskDao(db);
        attachmentDao = new AttachmentDao(db);

        userDao.deleteByEmailForTest(TEST_EMAIL);

        User uploader = new User(0, "AttachDaoUser", TEST_EMAIL, "$2a$10$testhash", Instant.now());
        userDao.insert(uploader);
        uploaderId = userDao.findByEmail(TEST_EMAIL).orElseThrow().getId();

        Task t = new Task();
        t.setTitle("Attachment DAO Test Task");
        t.setUserId(uploaderId);
        t.setCreatedAt(Instant.now());
        taskId = taskDao.insert(t).getId();
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM attachments WHERE task_id = ?")) {
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
    void cleanAttachments() throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM attachments WHERE task_id = ?")) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------
    // insert
    // ---------------------------------------------------------

    @Test
    @DisplayName("insert() persists an attachment and returns auto-generated id")
    void insert_persistsAttachment() {
        Attachment a = new Attachment();
        a.setOriginalName("report.pdf");
        a.setStoredName("uuid123_report.pdf");
        a.setMimeType("application/pdf");
        a.setSizeBytes(1024L);
        a.setTaskId(taskId);
        a.setUploadedBy(uploaderId);
        a.setCreatedAt(Instant.now());

        Attachment saved = attachmentDao.insert(a);

        assertTrue(saved.getId() > 0, "auto-generated id must be > 0");
        assertEquals("report.pdf", saved.getOriginalName());
        assertEquals("uuid123_report.pdf", saved.getStoredName());
        assertEquals(taskId, saved.getTaskId());
        assertEquals(uploaderId, saved.getUploadedBy());
    }

    // ---------------------------------------------------------
    // findByTaskId
    // ---------------------------------------------------------

    @Test
    @DisplayName("findByTaskId() returns all attachments for a task")
    void findByTaskId_returnsAttachments() {
        insertAttachment("a.txt", "ua_a.txt");
        insertAttachment("b.txt", "ub_b.txt");

        List<Attachment> list = attachmentDao.findByTaskId(taskId);

        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("findByTaskId() returns empty list when no attachments exist")
    void findByTaskId_noAttachments_returnsEmpty() {
        List<Attachment> list = attachmentDao.findByTaskId(taskId);
        assertTrue(list.isEmpty());
    }

    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

    private void insertAttachment(String originalName, String storedName) {
        Attachment a = new Attachment();
        a.setOriginalName(originalName);
        a.setStoredName(storedName);
        a.setMimeType("text/plain");
        a.setSizeBytes(100L);
        a.setTaskId(taskId);
        a.setUploadedBy(uploaderId);
        a.setCreatedAt(Instant.now());
        attachmentDao.insert(a);
    }
}
