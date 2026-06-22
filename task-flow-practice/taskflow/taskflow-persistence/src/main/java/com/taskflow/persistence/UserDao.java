package com.taskflow.persistence;

import com.taskflow.domain.User;
import com.taskflow.domain.UserRepository;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;

/**
 * Data-access object for the {@code users} table.
 *
 * <p>All SQL uses {@link PreparedStatement} — no string concatenation (NFR-006).
 */
public class UserDao implements UserRepository {

    private final DbConnection db;

    public UserDao(DbConnection db) {
        this.db = db;
    }

    /**
     * Inserts a new user row and sets the generated {@code id} on the returned object.
     *
     * @param user user with {@code id == 0}; must have non-null name, email, passwordHash
     * @return the same object with its auto-generated {@code id} set
     * @throws RuntimeException wrapping {@link SQLException} on failure
     */
    public User insert(User user) {
        String sql = "INSERT INTO users (username, email, password, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setTimestamp(4, Timestamp.from(
                    user.getCreatedAt() != null ? user.getCreatedAt() : Instant.now()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.insert failed: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up a user by email address.
     *
     * @param email the email to search for (case-sensitive, as stored)
     * @return an {@link Optional} containing the user, or empty if not found
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, username, email, password, created_at FROM users WHERE email = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getTimestamp("created_at").toInstant());
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.findByEmail failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test-only helper: deletes a row by email so tests can reset state.
     * Never called in production code.
     */
    public void deleteByEmailForTest(String email) {
        String sql = "DELETE FROM users WHERE email = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Best-effort cleanup — swallow so test setup doesn't fail
        }
    }
}
