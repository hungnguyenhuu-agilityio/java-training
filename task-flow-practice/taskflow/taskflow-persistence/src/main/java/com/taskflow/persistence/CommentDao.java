package com.taskflow.persistence;

import com.taskflow.domain.Comment;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data-access object for the {@code comments} table.
 *
 * <p>All SQL uses {@link PreparedStatement} — no string concatenation (NFR-006).
 */
public class CommentDao {

    private final DbConnection db;

    public CommentDao(DbConnection db) {
        this.db = db;
    }

    /**
     * Inserts a new comment row and sets the generated {@code id} on the returned object.
     */
    public Comment insert(Comment comment) {
        String sql = "INSERT INTO comments (body, task_id, author_id, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, comment.getBody());
            ps.setLong(2, comment.getTaskId());
            ps.setLong(3, comment.getAuthorId());
            ps.setTimestamp(4, Timestamp.from(
                    comment.getCreatedAt() != null ? comment.getCreatedAt() : Instant.now()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    comment.setId(keys.getLong(1));
                }
            }
            return comment;
        } catch (SQLException e) {
            throw new RuntimeException("CommentDao.insert failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all comments for the given task, ordered by creation time ascending.
     */
    public List<Comment> findByTaskId(long taskId) {
        String sql = "SELECT id, body, task_id, author_id, created_at FROM comments WHERE task_id = ? ORDER BY created_at ASC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Comment> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("CommentDao.findByTaskId failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a single comment by id, or empty if not found.
     */
    public Optional<Comment> findById(long id) {
        String sql = "SELECT id, body, task_id, author_id, created_at FROM comments WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("CommentDao.findById failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes the comment with the given id. No-op if not found.
     */
    public void delete(long commentId) {
        String sql = "DELETE FROM comments WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, commentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("CommentDao.delete failed: " + e.getMessage(), e);
        }
    }

    private Comment mapRow(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getLong("id"));
        c.setBody(rs.getString("body"));
        c.setTaskId(rs.getLong("task_id"));
        c.setAuthorId(rs.getLong("author_id"));
        Timestamp ts = rs.getTimestamp("created_at");
        c.setCreatedAt(ts != null ? ts.toInstant() : null);
        return c;
    }
}
