package com.taskflow.service;

import com.taskflow.domain.User;
import com.taskflow.persistence.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UserService}.
 *
 * <p>Uses an in-memory stub {@link UserDao} — no MySQL required.
 */
class UserServiceTest {

    /** Minimal stub: stores one user by email for lookup. */
    private static class StubUserDao extends UserDao {
        private User stored;

        StubUserDao() {
            super(null); // no DB connection needed
        }

        @Override
        public User insert(User user) {
            user.setId(1L);
            this.stored = user;
            return user;
        }

        @Override
        public Optional<User> findByEmail(String email) {
            if (stored != null && stored.getEmail().equals(email)) {
                return Optional.of(stored);
            }
            return Optional.empty();
        }
    }

    private StubUserDao dao;
    private TokenStore tokenStore;
    private UserService service;

    @BeforeEach
    void setUp() {
        dao = new StubUserDao();
        tokenStore = new TokenStore();
        service = new UserService(dao, tokenStore);
    }

    @Test
    @DisplayName("register() stores BCrypt hash — not plain-text password")
    void register_hashesPassword() {
        User user = service.register("Alice", "alice@example.com", "secret123");

        assertNotNull(user.getPasswordHash(), "password hash must not be null");
        assertTrue(user.getPasswordHash().startsWith("$2a$"),
                "BCrypt hash must start with $2a$");
        assertTrue(BCrypt.checkpw("secret123", user.getPasswordHash()),
                "BCrypt.checkpw must verify the original password against the stored hash");
        assertNotEquals("secret123", user.getPasswordHash(),
                "plain-text password must not be stored");
    }

    @Test
    @DisplayName("register() with duplicate email throws IllegalArgumentException")
    void register_duplicateEmail_throws() {
        service.register("Alice", "alice@example.com", "secret123");

        assertThrows(IllegalArgumentException.class,
                () -> service.register("Alice2", "alice@example.com", "other"),
                "Duplicate email must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("login() with correct password returns a non-empty Optional<String> token")
    void login_correctPassword_returnsToken() {
        service.register("Alice", "alice@example.com", "secret123");

        Optional<String> token = service.login("alice@example.com", "secret123");

        assertTrue(token.isPresent(), "login with correct credentials must return a token");
        assertFalse(token.get().isBlank(), "returned token must not be blank");
    }

    @Test
    @DisplayName("login() with wrong password returns empty Optional")
    void login_wrongPassword_returnsEmpty() {
        service.register("Alice", "alice@example.com", "secret123");

        Optional<String> token = service.login("alice@example.com", "wrong");

        assertTrue(token.isEmpty(), "login with wrong password must return empty Optional");
    }

    @Test
    @DisplayName("login() for non-existent email returns empty Optional (no NPE)")
    void login_unknownEmail_returnsEmpty() {
        Optional<String> token = service.login("ghost@example.com", "anything");

        assertTrue(token.isEmpty(), "login for unknown email must return empty Optional");
    }

    @Test
    @DisplayName("register() sets createdAt to a non-null Instant")
    void register_setsCreatedAt() {
        Instant before = Instant.now();
        User user = service.register("Bob", "bob@example.com", "pass");
        Instant after = Instant.now();

        assertNotNull(user.getCreatedAt());
        assertFalse(user.getCreatedAt().isBefore(before));
        assertFalse(user.getCreatedAt().isAfter(after));
    }
}
