package com.taskflow.domain;

import java.time.Instant;

/**
 * Domain model for a Comment attached to a Task.
 *
 * <p>Fields map 1-to-1 with the {@code comments} table in V1__init.sql.
 */
public class Comment {

    private long id;
    private String body;
    private long taskId;
    private long authorId;
    private Instant createdAt;

    public Comment() {}

    public Comment(long id, String body, long taskId, long authorId, Instant createdAt) {
        this.id = id;
        this.body = body;
        this.taskId = taskId;
        this.authorId = authorId;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public long getTaskId() { return taskId; }
    public void setTaskId(long taskId) { this.taskId = taskId; }

    public long getAuthorId() { return authorId; }
    public void setAuthorId(long authorId) { this.authorId = authorId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
