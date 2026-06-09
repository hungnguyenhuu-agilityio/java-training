package com.taskflow.service;

import com.taskflow.domain.Priority;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;
import com.taskflow.persistence.TaskDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TaskService}.
 *
 * <p>Uses an in-memory stub {@link TaskDao} — no MySQL required.
 */
class TaskServiceTest {

    /** Minimal stub backed by a simple list. */
    private static class StubTaskDao extends TaskDao {
        private final java.util.List<Task> store = new java.util.ArrayList<>();
        private long nextId = 1;

        StubTaskDao() {
            super(null);
        }

        @Override
        public Task insert(Task task) {
            task.setId(nextId++);
            store.add(task);
            return task;
        }

        @Override
        public List<Task> findAll() {
            return List.copyOf(store);
        }

        @Override
        public Optional<Task> findById(long id) {
            return store.stream().filter(t -> t.getId() == id).findFirst();
        }

        @Override
        public void update(Task task) {
            store.removeIf(t -> t.getId() == task.getId());
            store.add(task);
        }

        @Override
        public void delete(long id) {
            store.removeIf(t -> t.getId() == id);
        }
    }

    private StubTaskDao dao;
    private TaskService service;

    @BeforeEach
    void setUp() {
        dao = new StubTaskDao();
        service = new TaskService(dao);

        // Seed three tasks
        dao.insert(task("Task A", TaskStatus.TODO, Priority.HIGH, null));
        dao.insert(task("Task B", TaskStatus.IN_PROGRESS, Priority.MEDIUM, null));
        dao.insert(task("Task C", TaskStatus.DONE, Priority.LOW, null));
    }

    private Task task(String title, TaskStatus status, Priority priority, Long projectId) {
        Task t = new Task();
        t.setTitle(title);
        t.setStatus(status);
        t.setPriority(priority);
        t.setProjectId(projectId);
        t.setCreatedAt(Instant.now());
        return t;
    }

    @Test
    @DisplayName("getTasks() with no filters returns all tasks")
    void getTasks_noFilters_returnsAll() {
        List<Task> result = service.getTasks(Map.of());

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("getTasks() filtered by status=TODO returns only TODO tasks")
    void getTasks_filterByStatus_returnsTodo() {
        List<Task> result = service.getTasks(Map.of("status", "TODO"));

        assertEquals(1, result.size());
        assertEquals(TaskStatus.TODO, result.get(0).getStatus());
    }

    @Test
    @DisplayName("getTasks() filtered by priority=HIGH returns only HIGH tasks")
    void getTasks_filterByPriority_returnsHigh() {
        List<Task> result = service.getTasks(Map.of("priority", "HIGH"));

        assertEquals(1, result.size());
        assertEquals(Priority.HIGH, result.get(0).getPriority());
    }

    @Test
    @DisplayName("getTasks() filtered by projectId returns tasks matching that project")
    void getTasks_filterByProjectId_returnsMatching() {
        dao.insert(task("Task D", TaskStatus.TODO, Priority.LOW, 42L));

        List<Task> result = service.getTasks(Map.of("projectId", "42"));

        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).getProjectId());
    }

    @Test
    @DisplayName("getTasks() with combined filters applies all criteria")
    void getTasks_combinedFilters_appliesAll() {
        dao.insert(task("Task E", TaskStatus.TODO, Priority.HIGH, 10L));

        List<Task> result = service.getTasks(Map.of("status", "TODO", "priority", "HIGH"));

        // Task A (TODO, HIGH, no projectId) + Task E (TODO, HIGH, projectId=10)
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t ->
                t.getStatus() == TaskStatus.TODO && t.getPriority() == Priority.HIGH));
    }

    @Test
    @DisplayName("getTasks() with no matching results returns empty list, not exception")
    void getTasks_noMatch_returnsEmptyList() {
        List<Task> result = service.getTasks(Map.of("status", "DONE", "priority", "HIGH"));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
