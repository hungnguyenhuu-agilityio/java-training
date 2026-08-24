package com.taskflow.service;

import com.taskflow.domain.Priority;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;
import com.taskflow.domain.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NotificationScheduler}.
 *
 * <p>Uses an in-memory stub {@link TaskDao} — no MySQL required.
 * Log output is captured via a custom {@link Handler}.
 */
class NotificationSchedulerTest {

    // -------------------------------------------------------------------------
    // Stub DAO
    // -------------------------------------------------------------------------

    private static class StubTaskDao implements TaskRepository {
        private List<Task> dueSoonResult = new ArrayList<>();
        private boolean throwOnFindDueSoon = false;

        void setDueSoonResult(List<Task> tasks) {
            this.dueSoonResult = tasks;
        }

        void setThrowOnFindDueSoon(boolean value) {
            this.throwOnFindDueSoon = value;
        }

        @Override
        public List<Task> findDueSoon(LocalDate from, LocalDate to) {
            if (throwOnFindDueSoon) {
                throw new RuntimeException("DB unavailable");
            }
            return dueSoonResult;
        }

        // Stub out all other DAO methods to prevent NPE from null DbConnection
        @Override public Task insert(Task t) { throw new UnsupportedOperationException(); }
        @Override public List<Task> findAll() { throw new UnsupportedOperationException(); }
        @Override public Optional<Task> findById(long id) { throw new UnsupportedOperationException(); }
        @Override public void update(Task t) { throw new UnsupportedOperationException(); }
        @Override public void delete(long id) { throw new UnsupportedOperationException(); }
    }

    // -------------------------------------------------------------------------
    // Log capture
    // -------------------------------------------------------------------------

    private static class CapturingHandler extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override public void publish(LogRecord r) { records.add(r); }
        @Override public void flush() {}
        @Override public void close() {}

        boolean hasMessageContaining(String text) {
            return records.stream().anyMatch(r -> r.getMessage().contains(text));
        }
    }

    private CapturingHandler capturingHandler;
    private Logger schedulerLogger;

    @BeforeEach
    void attachLogCapture() {
        capturingHandler = new CapturingHandler();
        schedulerLogger = Logger.getLogger(NotificationScheduler.class.getName());
        schedulerLogger.addHandler(capturingHandler);
        schedulerLogger.setLevel(Level.ALL);
    }

    @AfterEach
    void detachLogCapture() {
        schedulerLogger.removeHandler(capturingHandler);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Task taskWithDueDate(long id, String title, LocalDate dueDate) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setDueDate(dueDate);
        t.setStatus(TaskStatus.TODO);
        t.setPriority(Priority.MEDIUM);
        return t;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("AC2: task due within 24 h produces a [NOTIFICATION] log line")
    void taskDueSoon_logsNotification() {
        StubTaskDao dao = new StubTaskDao();
        LocalDate today = LocalDate.now();
        dao.setDueSoonResult(List.of(taskWithDueDate(1L, "Fix login bug", today)));

        NotificationScheduler scheduler = new NotificationScheduler(dao, 60);
        scheduler.checkDueTasks(); // invoke directly — avoids waiting for schedule

        assertTrue(
                capturingHandler.hasMessageContaining("[NOTIFICATION] Task \"Fix login bug\" (id=1) is due within 24 hours."),
                "Expected [NOTIFICATION] log line for due task"
        );
    }

    @Test
    @DisplayName("AC3: task NOT due soon produces no [NOTIFICATION] log line")
    void taskNotDueSoon_noLog() {
        StubTaskDao dao = new StubTaskDao();
        // DAO returns empty list (task is not due soon)
        dao.setDueSoonResult(List.of());

        NotificationScheduler scheduler = new NotificationScheduler(dao, 60);
        scheduler.checkDueTasks();

        assertFalse(
                capturingHandler.hasMessageContaining("[NOTIFICATION]"),
                "Expected no [NOTIFICATION] log line for task not due soon"
        );
    }

    @Test
    @DisplayName("AC4: DB exception during check is caught — does not propagate")
    void dbException_caughtAndLogged() {
        StubTaskDao dao = new StubTaskDao();
        dao.setThrowOnFindDueSoon(true);

        NotificationScheduler scheduler = new NotificationScheduler(dao, 60);

        assertDoesNotThrow(scheduler::checkDueTasks,
                "checkDueTasks must not propagate exceptions");

        assertTrue(
                capturingHandler.hasMessageContaining("due-date check failed"),
                "Expected warning log when DB throws"
        );
    }

    @Test
    @DisplayName("AC5: shutdown() terminates the executor without exception")
    void shutdown_doesNotThrow() {
        StubTaskDao dao = new StubTaskDao();
        // Use a very large interval so the scheduled task doesn't fire during this test
        NotificationScheduler scheduler = new NotificationScheduler(dao, 999_999);
        scheduler.start();
        assertDoesNotThrow(scheduler::shutdown);
    }
}
