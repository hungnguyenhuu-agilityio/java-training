package com.taskflow.persistence;

import com.taskflow.domain.Priority;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link TaskDao}.
 *
 * <p>Requires Docker MySQL on localhost:3307 with V1__init.sql applied.
 * Each test cleans up after itself via {@code deleteByIdForTest}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskDaoTest {

    private static TaskDao dao;
    private static long insertedId;

    @BeforeAll
    static void setUpAll() {
        dao = new TaskDao(DbConnection.getInstance());
        // Clean any leftover rows from previous runs
        dao.deleteAllForTest();
    }

    @AfterAll
    static void tearDownAll() {
        dao.deleteAllForTest();
    }

    @Test
    @Order(1)
    @DisplayName("insert() persists a task and returns auto-generated id")
    void insert_persistsTask() {
        Task task = new Task(0, "Test Task", "A description",
                TaskStatus.TODO, Priority.HIGH, LocalDate.of(2026, 12, 31),
                null, null, Instant.now());

        Task saved = dao.insert(task);

        assertTrue(saved.getId() > 0, "auto-generated id must be > 0");
        assertEquals("Test Task", saved.getTitle());
        assertEquals(TaskStatus.TODO, saved.getStatus());
        assertEquals(Priority.HIGH, saved.getPriority());

        insertedId = saved.getId();
    }

    @Test
    @Order(2)
    @DisplayName("findById() returns the task that was just inserted")
    void findById_returnsInsertedTask() {
        Optional<Task> found = dao.findById(insertedId);

        assertTrue(found.isPresent(), "task must be found after insert");
        assertEquals("Test Task", found.get().getTitle());
        assertEquals(TaskStatus.TODO, found.get().getStatus());
        assertEquals(Priority.HIGH, found.get().getPriority());
        assertEquals(LocalDate.of(2026, 12, 31), found.get().getDueDate());
    }

    @Test
    @Order(3)
    @DisplayName("findAll() returns a list containing the inserted task")
    void findAll_returnsInsertedTask() {
        List<Task> tasks = dao.findAll();

        assertFalse(tasks.isEmpty(), "findAll must return at least one task");
        assertTrue(tasks.stream().anyMatch(t -> t.getId() == insertedId),
                "inserted task must appear in findAll");
    }

    @Test
    @Order(4)
    @DisplayName("update() changes mutable fields and persists them")
    void update_changesMutableFields() {
        Optional<Task> opt = dao.findById(insertedId);
        assertTrue(opt.isPresent());
        Task task = opt.get();

        task.setTitle("Updated Title");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPriority(Priority.LOW);
        task.setDescription("Updated desc");

        dao.update(task);

        Optional<Task> updated = dao.findById(insertedId);
        assertTrue(updated.isPresent());
        assertEquals("Updated Title", updated.get().getTitle());
        assertEquals(TaskStatus.IN_PROGRESS, updated.get().getStatus());
        assertEquals(Priority.LOW, updated.get().getPriority());
        assertEquals("Updated desc", updated.get().getDescription());
    }

    @Test
    @Order(5)
    @DisplayName("delete() removes the task row")
    void delete_removesTask() {
        dao.delete(insertedId);

        Optional<Task> found = dao.findById(insertedId);
        assertTrue(found.isEmpty(), "task must not exist after delete");
    }

    @Test
    @Order(6)
    @DisplayName("findById() returns empty Optional for non-existent id")
    void findById_nonExistent_returnsEmpty() {
        Optional<Task> found = dao.findById(999999L);

        assertTrue(found.isEmpty(), "non-existent id must return empty Optional");
    }
}
