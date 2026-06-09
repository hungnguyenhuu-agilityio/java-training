package com.taskflow.domain;

import java.time.Instant;

/**
 * Domain model representing a Project — a named container that groups related Tasks.
 *
 * <p>{@code id} is 0 for a transient (not-yet-persisted) project.
 */
public class Project {

    private long id;
    private String name;
    private String description;
    private long ownerId;
    private Instant createdAt;

    public Project() {}

    public Project(long id, String name, String description, long ownerId, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
