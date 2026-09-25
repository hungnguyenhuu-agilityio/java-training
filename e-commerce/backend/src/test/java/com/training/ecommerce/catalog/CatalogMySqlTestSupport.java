package com.training.ecommerce.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Runs catalog tests against the real MySQL 8.4.11 engine (collation, LIKE and ordering semantics
 * differ from H2). One container is shared by every subclass for the whole test JVM.
 */
@ActiveProfiles("test")
public abstract class CatalogMySqlTestSupport {

	private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4.11")
			.withDatabaseName("ecommerce")
			.withUsername("ecommerce")
			.withPassword("ecommerce-test");

	private static final AtomicLong UNIQUE_SUFFIX = new AtomicLong();

	static {
		MYSQL.start();
	}

	@DynamicPropertySource
	static void registerMySqlDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Autowired
	protected JdbcClient jdbcClient;

	protected void deleteCatalog() {
		jdbcClient.sql("DELETE FROM products").update();
		jdbcClient.sql("DELETE FROM categories").update();
	}

	protected long insertCategory(String name, boolean isActive) {
		var keyHolder = new GeneratedKeyHolder();
		jdbcClient.sql("INSERT INTO categories (name, slug, is_active) VALUES (:name, :slug, :isActive)")
				.param("name", name)
				.param("slug", "category-" + UNIQUE_SUFFIX.incrementAndGet())
				.param("isActive", isActive)
				.update(keyHolder, "id");
		return keyHolder.getKey().longValue();
	}

	protected long insertProduct(long categoryId, String name, String price, boolean isActive, Instant createdAt) {
		var suffix = UNIQUE_SUFFIX.incrementAndGet();
		var keyHolder = new GeneratedKeyHolder();
		jdbcClient.sql("INSERT INTO products (category_id, name, slug, description, sku, price, currency, is_active, created_at) "
				+ "VALUES (:categoryId, :name, :slug, :description, :sku, :price, 'USD', :isActive, :createdAt)")
				.param("categoryId", categoryId)
				.param("name", name)
				.param("slug", "product-" + suffix)
				.param("description", "Description of " + name)
				.param("sku", "SKU-" + suffix)
				.param("price", new BigDecimal(price))
				.param("isActive", isActive)
				.param("createdAt", java.sql.Timestamp.from(createdAt))
				.update(keyHolder, "id");
		return keyHolder.getKey().longValue();
	}

	protected long insertProduct(long categoryId, String name, String price) {
		return insertProduct(categoryId, name, price, true, Instant.parse("2026-01-01T00:00:00Z"));
	}
}
