package com.training.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LiquibaseBaselineMigrationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void should_create_the_complete_baseline_schema_with_liquibase() throws Exception {
		Set<String> tableNames = new HashSet<>();
		try (var connection = dataSource.getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			try (ResultSet tables = metadata.getTables(null, null, "%", new String[] { "TABLE" })) {
				while (tables.next()) {
					tableNames.add(tables.getString("TABLE_NAME").toLowerCase());
				}
			}
		}

		assertThat(tableNames).contains(
				"users",
				"refresh_sessions",
				"categories",
				"products",
				"warehouses",
				"warehouse_inventory",
				"carts",
				"cart_items",
				"orders",
				"order_items",
				"order_status_histories",
				"inventory_reservations",
				"inventory_reservation_items",
				"inventory_alert_states",
				"payments",
				"refunds",
				"processed_webhook_events",
				"notification_requests",
				"idempotency_keys");
	}

	@Test
	void should_scope_idempotency_keys_to_actor_and_operation() throws Exception {
		try (var connection = dataSource.getConnection()) {
			long firstUserId = insertUser(connection, "first@example.test");
			long secondUserId = insertUser(connection, "second@example.test");
			insertIdempotencyKey(connection, firstUserId, "fingerprint-a");
			insertIdempotencyKey(connection, secondUserId, "fingerprint-b");

			assertThatThrownBySql(() -> insertIdempotencyKey(connection, firstUserId, "fingerprint-a"));
		}
	}

	private long insertUser(java.sql.Connection connection, String email) throws SQLException {
		try (var statement = connection.prepareStatement(
				"INSERT INTO users (email, password_hash) VALUES (?, 'hash')", Statement.RETURN_GENERATED_KEYS)) {
			statement.setString(1, email);
			statement.executeUpdate();
			try (var generatedKeys = statement.getGeneratedKeys()) {
				generatedKeys.next();
				return generatedKeys.getLong(1);
			}
		}
	}

	private void insertIdempotencyKey(java.sql.Connection connection, long userId, String fingerprint)
			throws SQLException {
		try (var statement = connection.prepareStatement("INSERT INTO idempotency_keys "
				+ "(idempotency_key, user_id, request_path, request_fingerprint, expires_at) "
				+ "VALUES ('same-key', ?, '/orders', ?, CURRENT_TIMESTAMP)")) {
			statement.setLong(1, userId);
			statement.setString(2, fingerprint);
			statement.executeUpdate();
		}
	}

	@Test
	void should_persist_payment_expiry_and_webhook_object_identity() throws Exception {
		try (var connection = dataSource.getConnection()) {
			assertThat(columnNames(connection.getMetaData(), "PAYMENTS")).contains("expires_at");
			assertThat(columnNames(connection.getMetaData(), "PROCESSED_WEBHOOK_EVENTS"))
					.contains("provider_object_id");
		}
	}

	@Test
	void should_apply_rollback_and_reapply_the_baseline_on_a_scratch_database() throws Exception {
		var connection = DriverManager.getConnection("jdbc:h2:mem:rollback;MODE=MySQL", "sa", "");
		var database = DatabaseFactory.getInstance()
				.findCorrectDatabaseImplementation(new JdbcConnection(connection));
		try (var resources = new ClassLoaderResourceAccessor();
				var liquibase = new Liquibase("db/changelog/db.changelog-master.yaml", resources, database)) {
			liquibase.update();
			assertThat(countBusinessTables(connection, "PUBLIC")).isEqualTo(20);

			liquibase.rollback(1, "");
			assertThat(countBusinessTables(connection, "PUBLIC")).isZero();

			liquibase.update();
			assertThat(countBusinessTables(connection, "PUBLIC")).isEqualTo(20);
		}
	}

	private Set<String> columnNames(DatabaseMetaData metadata, String tableName) throws SQLException {
		Set<String> columnNames = new HashSet<>();
		try (ResultSet columns = metadata.getColumns(null, null, tableName, "%")) {
			while (columns.next()) {
				columnNames.add(columns.getString("COLUMN_NAME").toLowerCase());
			}
		}
		return columnNames;
	}

	private int countBusinessTables(java.sql.Connection connection, String schemaName) throws SQLException {
		try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM information_schema.tables "
				+ "WHERE table_schema = ? AND table_name NOT IN ('DATABASECHANGELOG', 'DATABASECHANGELOGLOCK')")) {
			statement.setString(1, schemaName);
			try (var result = statement.executeQuery()) {
				result.next();
				return result.getInt(1);
			}
		}
	}

	private void assertThatThrownBySql(SqlOperation operation) {
		org.assertj.core.api.Assertions.assertThatThrownBy(operation::execute)
				.isInstanceOf(SQLException.class);
	}

	@FunctionalInterface
	private interface SqlOperation {
		void execute() throws SQLException;
	}
}
