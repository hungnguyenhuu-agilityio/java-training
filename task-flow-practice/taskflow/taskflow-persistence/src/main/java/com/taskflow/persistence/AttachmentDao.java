package com.taskflow.persistence;

import com.taskflow.domain.Attachment;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code attachments} table.
 *
 * <p>All SQL uses {@link PreparedStatement} — no string concatenation (NFR-006).
 */
public class AttachmentDao {

    private final DbConnection db;

    public AttachmentDao(DbConnection db) {
        this.db = db;
    }

    /**
     * Inserts a new attachment row and sets the generated {@code id} on the returned object.
     */
    public Attachment insert(Attachment attachment) {
        String sql = "INSERT INTO attachments "
                + "(original_name, stored_name, mime_type, size_bytes, task_id, uploaded_by, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, attachment.getOriginalName());
            ps.setString(2, attachment.getStoredName());
            ps.setString(3, attachment.getMimeType());
            ps.setLong(4, attachment.getSizeBytes());
            ps.setLong(5, attachment.getTaskId());
            ps.setLong(6, attachment.getUploadedBy());
            ps.setTimestamp(7, Timestamp.from(
                    attachment.getCreatedAt() != null ? attachment.getCreatedAt() : Instant.now()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    attachment.setId(keys.getLong(1));
                }
            }
            return attachment;
        } catch (SQLException e) {
            throw new RuntimeException("AttachmentDao.insert failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns all attachments for the given task, ordered by creation time ascending.
     */
    public List<Attachment> findByTaskId(long taskId) {
        String sql = "SELECT id, original_name, stored_name, mime_type, size_bytes, task_id, "
                + "uploaded_by, created_at FROM attachments WHERE task_id = ? ORDER BY created_at ASC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Attachment> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException("AttachmentDao.findByTaskId failed: " + e.getMessage(), e);
        }
    }

    private Attachment mapRow(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getLong("id"));
        a.setOriginalName(rs.getString("original_name"));
        a.setStoredName(rs.getString("stored_name"));
        a.setMimeType(rs.getString("mime_type"));
        a.setSizeBytes(rs.getLong("size_bytes"));
        a.setTaskId(rs.getLong("task_id"));
        a.setUploadedBy(rs.getLong("uploaded_by"));
        Timestamp ts = rs.getTimestamp("created_at");
        a.setCreatedAt(ts != null ? ts.toInstant() : null);
        return a;
    }
}
