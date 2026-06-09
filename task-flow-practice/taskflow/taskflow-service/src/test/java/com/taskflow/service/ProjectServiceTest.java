package com.taskflow.service;

import com.taskflow.domain.Project;
import com.taskflow.domain.ProjectHasTasksException;
import com.taskflow.persistence.ProjectDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProjectService}.
 *
 * <p>Uses an in-memory stub {@link ProjectDao} — no MySQL required.
 */
class ProjectServiceTest {

    /** Minimal in-memory stub for ProjectDao. */
    private static class StubProjectDao extends ProjectDao {
        private final java.util.Map<Long, Project> store = new java.util.LinkedHashMap<>();
        private long nextId = 1;
        private boolean throwOnDelete = false;

        StubProjectDao() {
            super(null);
        }

        @Override
        public Project insert(Project p) {
            p.setId(nextId++);
            store.put(p.getId(), p);
            return p;
        }

        @Override
        public List<Project> findAll() {
            return List.copyOf(store.values());
        }

        @Override
        public Optional<Project> findById(long id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public void update(Project p) {
            store.put(p.getId(), p);
        }

        @Override
        public void delete(long id) throws ProjectHasTasksException {
            if (throwOnDelete) throw new ProjectHasTasksException(id);
            store.remove(id);
        }

        void configureThrowOnDelete(boolean value) {
            this.throwOnDelete = value;
        }
    }

    private StubProjectDao dao;
    private ProjectService service;

    @BeforeEach
    void setUp() {
        dao = new StubProjectDao();
        service = new ProjectService(dao);
    }

    @Test
    @DisplayName("create() persists project and returns it with generated id")
    void create_persistsProject() {
        Project p = service.create("Sprint 1", "First sprint", 42L);

        assertTrue(p.getId() > 0);
        assertEquals("Sprint 1", p.getName());
        assertEquals("First sprint", p.getDescription());
        assertEquals(42L, p.getOwnerId());
        assertNotNull(p.getCreatedAt());
    }

    @Test
    @DisplayName("getAll() returns all projects")
    void getAll_returnsAll() {
        service.create("P1", null, 1L);
        service.create("P2", null, 1L);

        List<Project> all = service.getAll();

        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("getById() returns existing project")
    void getById_returnsProject() {
        Project created = service.create("My Project", "desc", 1L);

        Optional<Project> found = service.getById(created.getId());

        assertTrue(found.isPresent());
        assertEquals("My Project", found.get().getName());
    }

    @Test
    @DisplayName("getById() returns empty for unknown id")
    void getById_unknownId_returnsEmpty() {
        assertTrue(service.getById(999L).isEmpty());
    }

    @Test
    @DisplayName("update() changes project fields")
    void update_changesFields() {
        Project created = service.create("Old Name", "old", 1L);
        created.setName("New Name");
        created.setDescription("new");

        service.update(created);

        assertEquals("New Name", service.getById(created.getId()).orElseThrow().getName());
    }

    @Test
    @DisplayName("delete() removes project when no tasks linked")
    void delete_noTasks_removesProject() throws ProjectHasTasksException {
        Project created = service.create("ToDelete", null, 1L);

        service.delete(created.getId());

        assertTrue(service.getById(created.getId()).isEmpty());
    }

    @Test
    @DisplayName("delete() with linked tasks propagates ProjectHasTasksException")
    void delete_withTasks_throwsProjectHasTasksException() {
        dao.configureThrowOnDelete(true);
        Project created = service.create("HasTasks", null, 1L);

        assertThrows(ProjectHasTasksException.class,
                () -> service.delete(created.getId()),
                "service must propagate ProjectHasTasksException from DAO");
    }
}
