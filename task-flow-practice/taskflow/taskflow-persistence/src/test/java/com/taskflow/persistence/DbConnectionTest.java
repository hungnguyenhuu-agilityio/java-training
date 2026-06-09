package com.taskflow.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link DbConnection}.
 *
 * <p>Requires a running MySQL container on localhost:3307 with database
 * {@code taskflow_db}, user {@code taskflow}, password {@code taskflow}.
 * Start with:
 * <pre>
 *   docker run -d --name taskflow-mysql \
 *     -e MYSQL_ROOT_PASSWORD=taskflow \
 *     -e MYSQL_DATABASE=taskflow_db \
 *     -e MYSQL_USER=taskflow \
 *     -e MYSQL_PASSWORD=taskflow \
 *     -p 3307:3306 mysql:8.4
 * </pre>
 */
class DbConnectionTest {

    /**
     * AC-4 (happy path): open a HikariCP connection and execute {@code SELECT 1}.
     * Must complete without any exception.
     */
    @Test
    @DisplayName("SELECT 1 succeeds over a HikariCP connection")
    void selectOne_succeeds() throws SQLException {
        DbConnection db = DbConnection.getInstance();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            assertTrue(rs.next(), "ResultSet must have at least one row");
            assertEquals(1, rs.getInt(1), "SELECT 1 must return 1");
        }
    }

    /**
     * AC-4 (negative path): connecting with bad credentials must throw
     * {@link SQLException}, not an NPE or a silent hang.
     */
    @Test
    @DisplayName("Bad credentials throw SQLException, not NPE")
    void badCredentials_throwsSQLException() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3307/taskflow_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername("wrong_user");
        config.setPassword("wrong_password");
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(5000);   // fail fast — 5 s max
        config.setInitializationFailTimeout(1);

        // HikariCP throws an exception during pool initialisation when
        // credentials are rejected by the server.
        assertThrows(Exception.class, () -> {
            try (HikariDataSource ds = new HikariDataSource(config)) {
                ds.getConnection();
            }
        }, "Expected an exception when connecting with bad credentials");
    }
}
