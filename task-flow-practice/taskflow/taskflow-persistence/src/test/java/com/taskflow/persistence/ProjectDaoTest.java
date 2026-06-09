package com.taskflow.persistence;

import com.taskflow.domain.Project;
import com.taskflow.domain.User;
import com.taskflow.domain.ProjectHasTasksException;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link ProjectDao}.
 *
 * <p>Requires Docker MySQL on localhost:3307 with V1__init.sql applied.
 * Each test cleans up its own rows via {@code deleteByOwnerForTest}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectDaoTest {

    private static final String TEST_EMAIL = "proj_dao_test@example.com";
    private static DbConnection db;
    private static UserDao userDao;
    private static ProjectDao projectDao;
    private static long ownerId;

    @BeforeAll
    static void setUpAll() throws SQLException {
        db = DbConnection.getInstance();
        userDao = new UserDao(db);
        projectDao = new ProjectDao(db);

        // Clean up any leftover test owner
        userDao.deleteByEmailForTest(TEST_EMAIL);

        // Insert a stable owner for all project tests
        User owner = new User(0, "ProjDaoOwner", TEST_EMAIL, "$2a$10$testhash", Instant.now());
        userDao.insert(owner);
        ownerId = userDao.findByEmail(TEST_EMAIL).orElseThrow().getId();
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        // Remove tasks first (FK), then projects, then owner
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM tasks WHERE project_id IN (SELECT id FROM projects WHERE owner_id = ?)")) {
            ps.setLong(1, ownerId);
            ps.executeUpdate();
        }
        projectDao.deleteAllByOwnerForTest(ownerId);
        userDao.deleteByEmailForTest(TEST_EMAIL);
    }

    @BeforeEach
    void cleanProjects() {
        projectDao.deleteAllByOwnerForTest(ownerId);
    }

    // ---------------------------------------------------------
    // insert + findById
    // ---------------------------------------------------------

    @Test
    @DisplayName("insert() persists a project and returns auto-generated id")
    void insert_persistsProject() {
        Project p = new Project(0, "Test Project", "A description", ownerId, Instant.now());
        Project saved = projectDao.insert(p);

        assertTrue(saved.getId() > 0, "auto-generated id must be > 0");
        assertEquals("Test Project", saved.getName());
    }

    @Test
    @DisplayName("findById() returns the project that was inserted")
    void findById_returnsInsertedProject() {
        Project p = new Project(0, "Find Me", "desc", ownerId, Instant.now());
        projectDao.insert(p);

        Optional<Project> found = projectDao.findById(p.getId());

        assertTrue(found.isPresent());
        assertEquals("Find Me", found.get().getName());
        assertEquals("desc", found.get().getDescription());
        assertEquals(ownerId, found.get().getOwnerId());
    }

    @Test
    @DisplayName("findById() returns empty for unknown id")
    void findById_unknownId_returnsEmpty() {
        Optional<Project> found = projectDao.findById(Long.MAX_VALUE);
        assertTrue(found.isEmpty());
    }

    // ---------------------------------------------------------
    // findAll
    // ---------------------------------------------------------

    @Test
    @DisplayName("findAll() returns all inserted projects")
    void findAll_returnsAllProjects() {
        projectDao.insert(new Project(0, "Alpha", null, ownerId, Instant.now()));
        projectDao.insert(new Project(0, "Beta", null, ownerId, Instant.now()));

        List<Project> all = projectDao.findAll();

        // At least our two projects are present (other tests may have added rows)
        long ours = all.stream().filter(pr -> pr.getOwnerId() == ownerId).count();
        assertEquals(2, ours);
    }

    // ---------------------------------------------------------
    // update
    // ---------------------------------------------------------

    @Test
    @DisplayName("update() changes name and description")
    void update_changesFields() {
        Project p = projectDao.insert(new Project(0, "Original", "old desc", ownerId, Instant.now()));

        p.setName("Updated");
        p.setDescription("new desc");
        projectDao.update(p);

        Project fetched = projectDao.findById(p.getId()).orElseThrow();
        assertEquals("Updated", fetched.getName());
        assertEquals("new desc", fetched.getDescription());
    }

    // ---------------------------------------------------------
    // delete (no tasks)
    // ---------------------------------------------------------

    @Test
    @DisplayName("delete() removes project when no tasks are linked")
    void delete_noTasks_removesProject() throws ProjectHasTasksException {
        Project p = projectDao.insert(new Project(0, "ToDelete", null, ownerId, Instant.now()));

        projectDao.delete(p.getId());

        assertTrue(projectDao.findById(p.getId()).isEmpty());
    }

    // ---------------------------------------------------------
    // delete (with tasks → ProjectHasTasksException)
    // ---------------------------------------------------------

    @Test
    @DisplayName("delete() with linked tasks throws ProjectHasTasksException")
    void delete_withTasks_throwsProjectHasTasksException() throws SQLException {
        Project p = projectDao.insert(new Project(0, "HasTasks", null, ownerId, Instant.now()));

        // Insert a task that references this project
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO tasks (title, project_id) VALUES (?, ?)")) {
            ps.setString(1, "Linked Task");
            ps.setLong(2, p.getId());
            ps.executeUpdate();
        }

        assertThrows(ProjectHasTasksException.class,
                () -> projectDao.delete(p.getId()),
                "delete with linked tasks must throw ProjectHasTasksException");

        // Cleanup: remove task then project
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM tasks WHERE project_id = ?")) {
            ps.setLong(1, p.getId());
            ps.executeUpdate();
        }
        try {
            projectDao.delete(p.getId());
        } catch (ProjectHasTasksException ignored) {}
    }
}
