package com.training.ecommerce.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * NFR-013 performance fixture for the catalog read APIs: 10,000 products and 100 categories on MySQL
 * 8.4.11, exercised over real HTTP by an in-JVM client on the same host (no network hop or TLS; the
 * client, server and MySQL share the CPU). The 100,000 historical orders are not seeded; no catalog
 * query reads them.
 *
 * <p>Load levels are selected with {@code -Dcatalog.load.level=smoke|light|target|stress|all}; without
 * the property, {@code smoke} and {@code light} run (the {@code verify} default). {@code target} is the
 * NFR-013 check (50 users, 1 s think time); {@code stress} (50 users, no think time) measures
 * saturation and only reports latency. See memory/decisions.md, 2026-09-25.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogPerformanceTest extends CatalogMySqlTestSupport {

	private static final String LOAD_LEVEL_PROPERTY = "catalog.load.level";
	private static final int REFERENCE_PRODUCTS = 10_000;
	private static final int REFERENCE_CATEGORIES = 100;
	private static final int OPERATION_KINDS = 6;
	private static final long P95_BUDGET_MS = 300;

	private static final String DIGITS = "(SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 "
			+ "UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 "
			+ "UNION ALL SELECT 9)";

	/** Named load levels; {@code isLatencyAsserted} false means p95 is printed but not enforced. */
	enum LoadLevel {
		SMOKE(1, OPERATION_KINDS * 20, Duration.ZERO, true, true),
		LIGHT(10, OPERATION_KINDS * 7, Duration.ZERO, true, true),
		TARGET(50, OPERATION_KINDS * 4, Duration.ofSeconds(1), true, false),
		STRESS(50, OPERATION_KINDS * 7, Duration.ZERO, false, false);

		private final int users;
		private final int requestsPerUser;
		private final Duration thinkTime;
		private final boolean isLatencyAsserted;
		private final boolean isDefault;

		LoadLevel(int users, int requestsPerUser, Duration thinkTime, boolean isLatencyAsserted, boolean isDefault) {
			this.users = users;
			this.requestsPerUser = requestsPerUser;
			this.thinkTime = thinkTime;
			this.isLatencyAsserted = isLatencyAsserted;
			this.isDefault = isDefault;
		}

		boolean isSelected() {
			String selection = System.getProperty(LOAD_LEVEL_PROPERTY);
			if (selection == null || selection.isBlank()) {
				return isDefault;
			}
			String normalized = selection.trim().toUpperCase(Locale.ROOT);
			if (normalized.equals("ALL")) {
				return true;
			}
			return valueOf(normalized) == this;
		}

		String label() {
			return "%s: %d users, think time %d ms".formatted(name().toLowerCase(Locale.ROOT), users,
					thinkTime.toMillis());
		}
	}

	private final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(5)).build();

	@LocalServerPort
	private int port;

	private List<Long> publicProductIds;
	private List<Long> categoryIds;

	@BeforeAll
	void seedReferenceCatalog() throws InterruptedException {
		deleteCatalog();
		jdbcClient.sql("INSERT INTO categories (name, slug, is_active) "
				+ "SELECT CONCAT('Category ', n), CONCAT('perf-category-', n), n NOT IN (7, 42) "
				+ "FROM (SELECT d1.d + d2.d * 10 AS n FROM " + DIGITS + " d1 CROSS JOIN " + DIGITS + " d2) numbers")
				.update();
		jdbcClient.sql("INSERT INTO products (category_id, name, slug, description, sku, price, currency, is_active, created_at) "
				+ "SELECT c.id, CONCAT(ELT(1 + n % 10, 'Desk Lamp', 'Oak Chair', 'Wool Rug', 'Glass Vase', 'Steel Shelf', "
				+ "'Linen Sheet', 'Cotton Towel', 'Brass Hook', 'Clay Pot', 'Pine Table'), ' ', n), "
				+ "CONCAT('perf-product-', n), CONCAT('Reference product ', n), CONCAT('PERF-', n), "
				+ "1 + (n * 37 % 100000) / 100, 'USD', n % 20 <> 0, TIMESTAMP('2026-01-01') + INTERVAL n MINUTE "
				+ "FROM (SELECT d1.d + d2.d * 10 + d3.d * 100 + d4.d * 1000 AS n FROM " + DIGITS + " d1 CROSS JOIN "
				+ DIGITS + " d2 CROSS JOIN " + DIGITS + " d3 CROSS JOIN " + DIGITS + " d4) numbers "
				+ "JOIN categories c ON c.slug = CONCAT('perf-category-', n % 100)")
				.update();
		jdbcClient.sql("ANALYZE TABLE products, categories").query().listOfRows();

		publicProductIds = jdbcClient.sql("SELECT p.id FROM products p JOIN categories c ON c.id = p.category_id "
				+ "WHERE p.is_active AND c.is_active ORDER BY p.id").query(Long.class).list();
		categoryIds = jdbcClient.sql("SELECT id FROM categories WHERE is_active ORDER BY id").query(Long.class).list();

		runLoad(10, 10, Duration.ZERO, "warm-up");
	}

	@AfterAll
	void removeReferenceCatalog() {
		deleteCatalog();
	}

	@Test
	void should_seed_the_reference_catalog_size() {
		assertThat(jdbcClient.sql("SELECT COUNT(*) FROM products").query(Long.class).single())
				.isEqualTo(REFERENCE_PRODUCTS);
		assertThat(jdbcClient.sql("SELECT COUNT(*) FROM categories").query(Long.class).single())
				.isEqualTo(REFERENCE_CATEGORIES);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(LoadLevel.class)
	void should_serve_the_catalog_at_the_selected_load_level(LoadLevel level) throws InterruptedException {
		assumeTrue(level.isSelected(), () -> level + " not selected by -D" + LOAD_LEVEL_PROPERTY);

		var result = runLoad(level.users, level.requestsPerUser, level.thinkTime, level.label());

		assertThat(result.requestCount()).isEqualTo(level.users * level.requestsPerUser);
		assertThat(result.failures()).as("non-200 responses").isZero();
		if (level.isLatencyAsserted) {
			result.latenciesByOperation().forEach((operation, latencies) -> assertThat(percentile(latencies, 95))
					.as("p95 of %s in ms at %s", operation, level.label()).isLessThanOrEqualTo(P95_BUDGET_MS));
		}
	}

	private LoadResult runLoad(int users, int requestsPerUser, Duration thinkTime, String label)
			throws InterruptedException {
		Map<String, List<Long>> latenciesByOperation = new ConcurrentHashMap<>();
		var failures = new AtomicInteger();
		var start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(users)) {
			for (int user = 0; user < users; user++) {
				int userIndex = user;
				executor.submit(() -> {
					start.await();
					for (int request = 0; request < requestsPerUser; request++) {
						if (request > 0 && !thinkTime.isZero()) {
							Thread.sleep(thinkTime);
						}
						var operation = operationFor(request * users + userIndex);
						long startedAt = System.nanoTime();
						var response = client.send(HttpRequest.newBuilder(URI.create(operation.uri())).GET().build(),
								HttpResponse.BodyHandlers.discarding());
						long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
						if (response.statusCode() != 200) {
							failures.incrementAndGet();
						}
						latenciesByOperation.computeIfAbsent(operation.name(), key -> new CopyOnWriteArrayList<>())
								.add(elapsedMs);
					}
					return null;
				});
			}
			start.countDown();
			executor.shutdown();
			assertThat(executor.awaitTermination(5, TimeUnit.MINUTES)).isTrue();
		}
		var result = new LoadResult(latenciesByOperation, failures.get());
		result.print(label);
		return result;
	}

	/**
	 * {@code sequence = request * users + userIndex}, so in any round concurrent users issue a mix of
	 * operation kinds instead of the same one in lockstep.
	 */
	private Operation operationFor(int sequence) {
		String base = "http://localhost:" + port + "/api/catalog";
		long productId = publicProductIds.get((sequence * 7919) % publicProductIds.size());
		long categoryId = categoryIds.get(sequence % categoryIds.size());
		return switch (sequence % OPERATION_KINDS) {
			case 0 -> new Operation("list-default", base + "/products");
			case 1 -> new Operation("list-search", base + "/products?q=lamp&sort=price_asc");
			case 2 -> new Operation("list-category", base + "/products?categoryId=" + categoryId + "&sort=newest");
			case 3 -> new Operation("list-deep-page", base + "/products?page=150&size=50&sort=name_desc");
			case 4 -> new Operation("detail", base + "/products/" + productId);
			default -> new Operation("categories", base + "/categories");
		};
	}

	private static long percentile(List<Long> latencies, int percentile) {
		var sorted = new ArrayList<>(latencies);
		Collections.sort(sorted);
		int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
		return sorted.get(Math.max(index, 0));
	}

	private record Operation(String name, String uri) {
	}

	private record LoadResult(Map<String, List<Long>> latenciesByOperation, int failures) {

		int requestCount() {
			return latenciesByOperation.values().stream().mapToInt(List::size).sum();
		}

		void print(String label) {
			System.out.printf("NFR-013 [%s] %d products, %d categories, requests=%d, failures=%d%n", label,
					REFERENCE_PRODUCTS, REFERENCE_CATEGORIES, requestCount(), failures);
			var all = new ArrayList<Long>();
			latenciesByOperation.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
				all.addAll(entry.getValue());
				printLine(label, entry.getKey(), entry.getValue());
			});
			printLine(label, "all", all);
		}

		private static void printLine(String label, String operation, List<Long> latencies) {
			System.out.printf("NFR-013 [%s] %-14s n=%4d p50=%4d ms p95=%4d ms max=%4d ms%n", label, operation,
					latencies.size(), percentile(latencies, 50), percentile(latencies, 95), percentile(latencies, 100));
		}
	}
}
