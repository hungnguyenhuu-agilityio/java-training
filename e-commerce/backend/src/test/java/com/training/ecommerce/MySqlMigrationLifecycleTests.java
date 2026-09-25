package com.training.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.time.Duration;
import java.time.Instant;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@EnabledIfSystemProperty(named = "mysqlMigrationTest", matches = "true")
class MySqlMigrationLifecycleTests {

	@Container
	private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
			.withDatabaseName("ecommerce")
			.withUsername("ecommerce")
			.withPassword("ecommerce-test");

	@Test
	void should_apply_rollback_and_reapply_the_baseline_on_mysql() throws Exception {
		var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
		var database = DatabaseFactory.getInstance()
				.findCorrectDatabaseImplementation(new JdbcConnection(connection));
		try (var resources = new ClassLoaderResourceAccessor();
				var liquibase = new Liquibase("db/changelog/db.changelog-master.yaml", resources, database)) {
			Instant startedAt = Instant.now();

			liquibase.update();
			assertThat(countBusinessTables(connection)).isEqualTo(20);

			liquibase.rollback(1, "");
			assertThat(countBusinessTables(connection)).isZero();

			liquibase.update();
			assertThat(countBusinessTables(connection)).isEqualTo(20);

			System.out.printf("MySQL migration lifecycle: up->rollback->up completed in %d ms; table counts 20->0->20.%n",
					Duration.between(startedAt, Instant.now()).toMillis());
		}
	}

	private int countBusinessTables(java.sql.Connection connection) throws Exception {
		try (var statement = connection.createStatement();
				var result = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables "
						+ "WHERE table_schema = DATABASE() "
						+ "AND table_name NOT IN ('DATABASECHANGELOG', 'DATABASECHANGELOGLOCK')")) {
			result.next();
			return result.getInt(1);
		}
	}
}
