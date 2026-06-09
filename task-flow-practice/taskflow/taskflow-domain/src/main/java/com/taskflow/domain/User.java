package com.taskflow.domain;

import java.time.Instant;

/**
 * Domain model representing a registered user.
 *
 * <p>{@code passwordHash} stores the BCrypt hash — never a plain-text password.
 * {@code id} is 0 for a transient (not-yet-persisted) user.
 */
public class User {

    private long id;
    private String name;
    private String email;
    private String passwordHash;
    private Instant createdAt;

    public User() {}

    public User(long id, String name, String email, String passwordHash, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
