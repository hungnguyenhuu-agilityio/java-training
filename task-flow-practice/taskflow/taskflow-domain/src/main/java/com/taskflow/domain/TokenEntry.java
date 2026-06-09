package com.taskflow.domain;

import java.time.Instant;

/**
 * A value held in the in-memory token store.
 *
 * <p>Holds the authenticated {@link User} and an absolute expiry timestamp.
 * The token itself (UUID string) is the map key — not stored here.
 */
public class TokenEntry {

    private final User user;
    private Instant expiresAt;

    public TokenEntry(User user, Instant expiresAt) {
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public User getUser() { return user; }

    public Instant getExpiresAt() { return expiresAt; }

    /** Allows tests to manipulate expiry to simulate TTL passage. */
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    /** Returns {@code true} if the token has not yet expired. */
    public boolean isValid() {
        return Instant.now().isBefore(expiresAt);
    }
}
