package com.taskflow.persistence;

import com.taskflow.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link UserDao}.
 *
 * <p>Requires the Docker MySQL container on localhost:3307 with the V1__init.sql
 * schema already applied. Each test cleans up its own row so tests stay isolated.
 */
class UserDaoTest {

    private UserDao dao;

    @BeforeEach
    void setUp() {
        dao = new UserDao(DbConnection.getInstance());
        // Clean any leftover row from a previous run
        dao.deleteByEmailForTest("dao_test@example.com");
    }

    @Test
    @DisplayName("insert() persists a user and returns auto-generated id")
    void insert_persistsUser() {
        User user = new User(0, "DaoTest", "dao_test@example.com", "$2a$10$hash", Instant.now());

        User saved = dao.insert(user);

        assertTrue(saved.getId() > 0, "auto-generated id must be > 0");
        assertEquals("dao_test@example.com", saved.getEmail());
    }

    @Test
    @DisplayName("findByEmail() returns the user that was just inserted")
    void findByEmail_returnsInsertedUser() {
        User user = new User(0, "DaoTest", "dao_test@example.com", "$2a$10$hash", Instant.now());
        dao.insert(user);

        Optional<User> found = dao.findByEmail("dao_test@example.com");

        assertTrue(found.isPresent(), "user must be found after insert");
        assertEquals("DaoTest", found.get().getName());
        assertEquals("dao_test@example.com", found.get().getEmail());
        assertTrue(found.get().getPasswordHash().startsWith("$2a$"));
    }

    @Test
    @DisplayName("findByEmail() returns empty Optional for unknown email")
    void findByEmail_unknownEmail_returnsEmpty() {
        Optional<User> found = dao.findByEmail("nobody@example.com");

        assertTrue(found.isEmpty(), "unknown email must return empty Optional");
    }
}
