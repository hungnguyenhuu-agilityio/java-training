package com.taskflow.persistence;

import com.taskflow.domain.Project;
import com.taskflow.domain.ProjectHasTasksException;
import com.taskflow.domain.ProjectRepository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the {@code projects} table.
 *
 * <p>All SQL uses {@link PreparedStatement} — no string concatenation (NFR-006).
 *
 * <p>{@link #delete(long)} catches {@link SQLIntegrityConstraintViolationException} thrown
 * by MySQL when tasks still reference the project and re-throws it as the checked
 * {@link ProjectHasTasksException}.
 */
public class ProjectDao implements ProjectRepository {

    private final DbConnection db;

    public ProjectDao(DbConnection db) {
        this.db = db;
    }

    /**
     * Inserts a new project row and sets the generated {@code id} on the returned object.
     */
    public Project insert(Project project) {
        String sql = "INSERT INTO projects (name, description, owner_id, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setLong(3, project.getOwnerId());
            ps.setTimestamp(4, Timestamp.from(
                    project.getCreatedAt() != null ? project.getCreatedAt() : Instant.now()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    project.setId(keys.getLong(1));
                }
            }
            return project;
        } catch (SQLException e) {
            throw new RuntimeException("ProjectDao.insert failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all projects ordered by created_at descending.
     */
    public List<Project> findAll() {
        String sql = "SELECT id, name, description, owner_id, created_at FROM projects ORDER BY created_at DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Project> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("ProjectDao.findAll failed: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up a project by its primary key.
     */
    public Optional<Project> findById(long id) {
        String sql = "SELECT id, name, description, owner_id, created_at FROM projects WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("ProjectDao.findById failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates {@code name} and {@code description} for the given project.
     */
    public void update(Project project) {
        String sql = "UPDATE projects SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setLong(3, project.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ProjectDao.update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a project by id.
     *
     * <p>MySQL raises {@link SQLIntegrityConstraintViolationException} when the project
     * still has rows in {@code tasks} (FK constraint {@code fk_tasks_project}).
     * That exception is caught and re-thrown as the checked {@link ProjectHasTasksException}.
     *
     * @throws ProjectHasTasksException if tasks are still linked to the project
     */
    public void delete(long id) throws ProjectHasTasksException {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ProjectHasTasksException(id);
        } catch (SQLException e) {
            throw new RuntimeException("ProjectDao.delete failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    /**
     * Test-only: deletes all projects owned by the given user so tests can reset state.
     */
    public void deleteAllByOwnerForTest(long ownerId) {
        String sql = "DELETE FROM projects WHERE owner_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ownerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Best-effort cleanup
        }
    }

    // -------------------------------------------------------------------------

    private static Project mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Project(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getLong("owner_id"),
                ts != null ? ts.toInstant() : null);
    }
}
