package com.taskflow.service;

import com.taskflow.domain.Priority;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;
import com.taskflow.domain.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link StatsService}.
 *
 * <p>Uses an in-memory stub {@link TaskDao} — no MySQL required.
 */
class StatsServiceTest {

    /** Minimal stub — overrides findAll() to return a fixed list. */
    private static class StubTaskDao implements TaskRepository {
        private final List<Task> store;

        StubTaskDao(List<Task> tasks) {
            this.store = tasks;
        }

        @Override public Task insert(Task t) { return t; }
        @Override public List<Task> findAll() { return store; }
        @Override public java.util.Optional<Task> findById(long id) { return java.util.Optional.empty(); }
        @Override public void update(Task t) {}
        @Override public void delete(long id) {}
        @Override public List<Task> findDueSoon(java.time.LocalDate f, java.time.LocalDate t) { return List.of(); }
    }

    private static Task task(TaskStatus status, Priority priority) {
        Task t = new Task();
        t.setStatus(status);
        t.setPriority(priority);
        return t;
    }

    // -------------------------------------------------------------------------
    // byStatus tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Empty task list → all status and priority counts are 0")
    void emptyList_allCountsZero() {
        StatsService service = new StatsService(new StubTaskDao(List.of()));
        Map<String, Long> byStatus = service.getStats().getByStatus();
        Map<String, Long> byPriority = service.getStats().getByPriority();

        assertEquals(0L, byStatus.get("TODO"));
        assertEquals(0L, byStatus.get("IN_PROGRESS"));
        assertEquals(0L, byStatus.get("DONE"));

        assertEquals(0L, byPriority.get("LOW"));
        assertEquals(0L, byPriority.get("MEDIUM"));
        assertEquals(0L, byPriority.get("HIGH"));
    }

    @Test
    @DisplayName("3 TODO + 1 DONE → correct byStatus counts, IN_PROGRESS = 0")
    void threeTodoOneDone_correctStatusCounts() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task(TaskStatus.TODO, Priority.LOW));
        tasks.add(task(TaskStatus.TODO, Priority.MEDIUM));
        tasks.add(task(TaskStatus.TODO, Priority.HIGH));
        tasks.add(task(TaskStatus.DONE, Priority.LOW));

        StatsService service = new StatsService(new StubTaskDao(tasks));
        Map<String, Long> byStatus = service.getStats().getByStatus();

        assertEquals(3L, byStatus.get("TODO"));
        assertEquals(0L, byStatus.get("IN_PROGRESS"));
        assertEquals(1L, byStatus.get("DONE"));
    }

    @Test
    @DisplayName("Mixed priorities → correct byPriority counts")
    void mixedPriorities_correctPriorityCounts() {
        List<Task> tasks = new ArrayList<>();
        tasks.add(task(TaskStatus.TODO, Priority.LOW));
        tasks.add(task(TaskStatus.TODO, Priority.HIGH));
        tasks.add(task(TaskStatus.IN_PROGRESS, Priority.HIGH));
        tasks.add(task(TaskStatus.DONE, Priority.MEDIUM));

        StatsService service = new StatsService(new StubTaskDao(tasks));
        Map<String, Long> byPriority = service.getStats().getByPriority();

        assertEquals(1L, byPriority.get("LOW"));
        assertEquals(1L, byPriority.get("MEDIUM"));
        assertEquals(2L, byPriority.get("HIGH"));
    }

    @Test
    @DisplayName("All 6 enum values always present in response maps")
    void allEnumValuesAlwaysPresent() {
        List<Task> tasks = List.of(task(TaskStatus.TODO, Priority.HIGH));
        StatsService service = new StatsService(new StubTaskDao(tasks));
        Map<String, Long> byStatus = service.getStats().getByStatus();
        Map<String, Long> byPriority = service.getStats().getByPriority();

        // All keys must exist
        assertEquals(3, byStatus.size());
        assertEquals(3, byPriority.size());
    }
}
