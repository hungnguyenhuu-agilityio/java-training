package com.taskflow.domain;

import java.time.Instant;

/**
 * Domain model for a file attachment linked to a Task.
 *
 * <p>Fields map 1-to-1 with the {@code attachments} table in V1__init.sql.
 */
public class Attachment {

    private long id;
    private String originalName;
    private String storedName;
    private String mimeType;
    private long sizeBytes;
    private long taskId;
    private long uploadedBy;
    private Instant createdAt;

    public Attachment() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public long getTaskId() { return taskId; }
    public void setTaskId(long taskId) { this.taskId = taskId; }

    public long getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(long uploadedBy) { this.uploadedBy = uploadedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
