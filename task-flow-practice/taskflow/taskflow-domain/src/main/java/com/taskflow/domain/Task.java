package com.taskflow.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Domain model representing a unit of work.
 *
 * <p>Fields map to the {@code tasks} table columns in V1__init.sql:
 * {@code assignee_id} ↔ {@code userId}, {@code project_id} ↔ {@code projectId}.
 * {@code id} is 0 for a transient (not-yet-persisted) task.
 */
public class Task {

    private long id;
    private String title;
    private String description;
    private TaskStatus status;
    private Priority priority;
    private LocalDate dueDate;
    private Long userId;      // maps to assignee_id
    private Long projectId;   // maps to project_id
    private Instant createdAt;

    public Task() {}

    public Task(long id, String title, String description,
                TaskStatus status, Priority priority, LocalDate dueDate,
                Long userId, Long projectId, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.userId = userId;
        this.projectId = projectId;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
