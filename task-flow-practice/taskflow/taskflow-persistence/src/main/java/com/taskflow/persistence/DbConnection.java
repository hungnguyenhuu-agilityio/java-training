package com.taskflow.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * HikariCP connection-pool singleton.
 *
 * <p>Configuration is loaded from {@code config.properties} on the classpath.
 * If the file is missing, an {@link IllegalStateException} is thrown with a
 * clear message — never an NPE.
 *
 * <p>Usage:
 * <pre>{@code
 *   try (Connection conn = DbConnection.getInstance().getConnection()) {
 *       // use conn
 *   }
 * }</pre>
 */
public final class DbConnection {

    private static volatile DbConnection instance;
    private final HikariDataSource dataSource;

    private DbConnection() {
        Properties props = loadProperties();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getRequired(props, "db.url"));
        config.setUsername(getRequired(props, "db.username"));
        config.setPassword(getRequired(props, "db.password"));
        config.setMaximumPoolSize(
                Integer.parseInt(props.getProperty("db.pool.maximumPoolSize", "10")));
        config.setConnectionTimeout(
                Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
        config.setPoolName("TaskFlowPool");

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Returns the singleton instance, creating it on first call (thread-safe).
     */
    public static DbConnection getInstance() {
        if (instance == null) {
            synchronized (DbConnection.class) {
                if (instance == null) {
                    instance = new DbConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Obtains a {@link Connection} from the pool. Callers must close the
     * connection (use try-with-resources) to return it to the pool.
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Shuts down the connection pool. Call this on JVM shutdown.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Properties loadProperties() {
        InputStream in = DbConnection.class
                .getClassLoader()
                .getResourceAsStream("config.properties");
        if (in == null) {
            throw new IllegalStateException(
                    "config.properties not found on classpath. "
                    + "Create taskflow-persistence/src/main/resources/config.properties.");
        }
        Properties props = new Properties();
        try (in) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config.properties: " + e.getMessage(), e);
        }
        return props;
    }

    private static String getRequired(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required property '" + key + "' is missing from config.properties.");
        }
        return value.trim();
    }
}
