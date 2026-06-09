package com.taskflow.persistence;

import com.taskflow.domain.Priority;
import com.taskflow.domain.Task;
import com.taskflow.domain.TaskStatus;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the {@code tasks} table.
 *
 * <p>All SQL uses {@link PreparedStatement} — no string concatenation (NFR-006).
 * Column mapping: {@code assignee_id} ↔ {@link Task#getUserId()},
 * {@code project_id} ↔ {@link Task#getProjectId()}.
 */
public class TaskDao {

    private final DbConnection db;

    public TaskDao(DbConnection db) {
        this.db = db;
    }

    /**
     * Inserts a new task row and sets the generated {@code id} on the returned object.
     *
     * @param task task with {@code id == 0}; must have non-null title
     * @return the same object with its auto-generated {@code id} set
     */
    public Task insert(Task task) {
        String sql = """
                INSERT INTO tasks (title, description, status, priority, due_date,
                                   project_id, assignee_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus() != null
                    ? task.getStatus().name() : TaskStatus.TODO.name());
            ps.setString(4, task.getPriority() != null
                    ? task.getPriority().name() : Priority.MEDIUM.name());

            if (task.getDueDate() != null) {
                ps.setDate(5, Date.valueOf(task.getDueDate()));
            } else {
                ps.setNull(5, Types.DATE);
            }

            if (task.getProjectId() != null) {
                ps.setLong(6, task.getProjectId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }

            if (task.getUserId() != null) {
                ps.setLong(7, task.getUserId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }

            ps.setTimestamp(8, Timestamp.from(
                    task.getCreatedAt() != null ? task.getCreatedAt() : Instant.now()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setId(keys.getLong(1));
                }
            }
            return task;
        } catch (SQLException e) {
            throw new RuntimeException("TaskDao.insert failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all task rows.
     */
    public List<Task> findAll() {
        String sql = """
                SELECT id, title, description, status, priority, due_date,
                       project_id, assignee_id, created_at
                FROM tasks
                ORDER BY id
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Task> tasks = new ArrayList<>();
            while (rs.next()) {
                tasks.add(mapRow(rs));
            }
            return tasks;
        } catch (SQLException e) {
            throw new RuntimeException("TaskDao.findAll failed: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up a task by primary key.
     *
     * @return an {@link Optional} containing the task, or empty if not found
     */
    public Optional<Task> findById(long id) {
        String sql = """
                SELECT id, title, description, status, priority, due_date,
                       project_id, assignee_id, created_at
                FROM tasks WHERE id = ?
                """;
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
            throw new RuntimeException("TaskDao.findById failed: " + e.getMessage(), e);
        }
    }

    /**
     * Updates all mutable fields of an existing task row.
     */
    public void update(Task task) {
        String sql = """
                UPDATE tasks
                   SET title = ?, description = ?, status = ?, priority = ?,
                       due_date = ?, project_id = ?, assignee_id = ?
                 WHERE id = ?
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus() != null
                    ? task.getStatus().name() : TaskStatus.TODO.name());
            ps.setString(4, task.getPriority() != null
                    ? task.getPriority().name() : Priority.MEDIUM.name());

            if (task.getDueDate() != null) {
                ps.setDate(5, Date.valueOf(task.getDueDate()));
            } else {
                ps.setNull(5, Types.DATE);
            }

            if (task.getProjectId() != null) {
                ps.setLong(6, task.getProjectId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }

            if (task.getUserId() != null) {
                ps.setLong(7, task.getUserId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }

            ps.setLong(8, task.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("TaskDao.update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes a task row by primary key.
     */
    public void delete(long id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("TaskDao.delete failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all tasks whose {@code due_date} falls between {@code from} (inclusive)
     * and {@code to} (inclusive).
     *
     * <p>The {@code due_date} column is stored as MySQL {@code DATE} — use
     * {@link java.sql.Date} params, not {@link java.sql.Timestamp}.
     *
     * @param from start of the window (inclusive)
     * @param to   end of the window (inclusive)
     * @return tasks in the window, ordered by id
     */
    public List<Task> findDueSoon(java.time.LocalDate from, java.time.LocalDate to) {
        String sql = """
                SELECT id, title, description, status, priority, due_date,
                       project_id, assignee_id, created_at
                FROM tasks
                WHERE due_date BETWEEN ? AND ?
                ORDER BY id
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));

            try (ResultSet rs = ps.executeQuery()) {
                List<Task> tasks = new ArrayList<>();
                while (rs.next()) {
                    tasks.add(mapRow(rs));
                }
                return tasks;
            }
        } catch (SQLException e) {
            throw new RuntimeException("TaskDao.findDueSoon failed: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Test helpers
    // -------------------------------------------------------------------------

    /**
     * Test-only: removes all rows from the tasks table so tests can reset state.
     */
    public void deleteAllForTest() {
        String sql = "DELETE FROM tasks";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            // Best-effort cleanup
        }
    }

    // -------------------------------------------------------------------------

    private Task mapRow(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getLong("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setStatus(TaskStatus.valueOf(rs.getString("status")));
        task.setPriority(Priority.valueOf(rs.getString("priority")));

        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) {
            task.setDueDate(dueDate.toLocalDate());
        }

        long projectId = rs.getLong("project_id");
        task.setProjectId(rs.wasNull() ? null : projectId);

        long assigneeId = rs.getLong("assignee_id");
        task.setUserId(rs.wasNull() ? null : assigneeId);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            task.setCreatedAt(createdAt.toInstant());
        }

        return task;
    }
}
