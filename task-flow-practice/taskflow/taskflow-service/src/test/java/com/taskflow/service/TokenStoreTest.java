package com.taskflow.service;

import com.taskflow.domain.TokenEntry;
import com.taskflow.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TokenStore}.
 */
class TokenStoreTest {

    private TokenStore store;
    private User alice;

    @BeforeEach
    void setUp() {
        store = new TokenStore();
        alice = new User(1L, "Alice", "alice@example.com", "$2a$hash", Instant.now());
    }

    @AfterEach
    void tearDown() {
        store.shutdown();
    }

    @Test
    @DisplayName("put() then validate() returns the user within TTL")
    void tokenValidWithinTTL() {
        String token = store.put(alice);

        Optional<User> result = store.validate(token);

        assertTrue(result.isPresent(), "token must be valid immediately after creation");
        assertEquals(alice.getEmail(), result.get().getEmail());
    }

    @Test
    @DisplayName("validate() returns empty for an unknown token")
    void unknownTokenReturnsEmpty() {
        Optional<User> result = store.validate("non-existent-token");

        assertTrue(result.isEmpty(), "unknown token must return empty Optional");
    }

    @Test
    @DisplayName("validate() returns empty when token is expired (manipulated expiresAt)")
    void expiredTokenReturnsEmpty() {
        String token = store.put(alice);

        // Force expiry by back-dating the entry's expiresAt
        store.expireForTest(token);

        Optional<User> result = store.validate(token);

        assertTrue(result.isEmpty(), "expired token must return empty Optional");
    }

    @Test
    @DisplayName("purgeExpired() removes tokens whose expiresAt is in the past")
    void purgeRemovesExpiredTokens() {
        String token = store.put(alice);
        store.expireForTest(token);

        store.purgeExpired();

        // After purge the map entry is gone; validate returns empty
        Optional<User> result = store.validate(token);
        assertTrue(result.isEmpty(), "purged token must not be retrievable");
    }

    @Test
    @DisplayName("put() generates a UUID-format token string")
    void tokenIsUUID() {
        String token = store.put(alice);

        // UUID format: 8-4-4-4-12 hex digits
        assertTrue(token.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "token must be a lowercase UUID string, got: " + token);
    }
}
