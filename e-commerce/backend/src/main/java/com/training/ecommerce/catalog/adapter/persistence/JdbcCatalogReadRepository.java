package com.training.ecommerce.catalog.adapter.persistence;

import com.training.ecommerce.catalog.application.CatalogReadRepository;
import com.training.ecommerce.catalog.application.CategoryReference;
import com.training.ecommerce.catalog.application.CategorySummary;
import com.training.ecommerce.catalog.application.ProductDetail;
import com.training.ecommerce.catalog.application.ProductPage;
import com.training.ecommerce.catalog.application.ProductSummary;
import com.training.ecommerce.catalog.domain.CatalogSearchCriteria;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Public catalog queries in plain SQL. Only bound parameters carry user input; the ORDER BY clause is
 * chosen from a closed enum. Search matching follows the {@code products.name} column collation
 * (MySQL default {@code utf8mb4_0900_ai_ci}: case- and accent-insensitive).
 */
@Repository
class JdbcCatalogReadRepository implements CatalogReadRepository {

	private static final String PUBLIC_PRODUCTS = """
			FROM products p
			JOIN categories c ON c.id = p.category_id
			WHERE p.is_active = TRUE AND c.is_active = TRUE""";

	private static final String PRODUCT_COLUMNS =
			"SELECT p.id, p.name, p.slug, p.price, p.currency, c.id AS category_id, c.name AS category_name";

	private final JdbcClient jdbcClient;

	JdbcCatalogReadRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public ProductPage searchPublicProducts(CatalogSearchCriteria criteria) {
		var filter = new StringBuilder(PUBLIC_PRODUCTS);
		Map<String, Object> parameters = new HashMap<>();
		if (criteria.categoryId() != null) {
			filter.append(" AND p.category_id = :categoryId");
			parameters.put("categoryId", criteria.categoryId());
		}
		if (criteria.query() != null) {
			filter.append(" AND p.name LIKE :namePattern ESCAPE '!'");
			parameters.put("namePattern", "%" + escapeLikeWildcards(criteria.query()) + "%");
		}

		long totalElements = jdbcClient.sql("SELECT COUNT(*) " + filter)
				.params(parameters)
				.query(Long.class)
				.single();
		List<ProductSummary> content = totalElements <= criteria.offset() ? List.of()
				: jdbcClient.sql(PRODUCT_COLUMNS + " " + filter + " ORDER BY " + orderBy(criteria)
						+ " LIMIT :limit OFFSET :offset")
						.params(parameters)
						.param("limit", criteria.size())
						.param("offset", criteria.offset())
						.query(JdbcCatalogReadRepository::mapSummary)
						.list();
		return ProductPage.of(content, criteria.page(), criteria.size(), totalElements);
	}

	@Override
	public Optional<ProductDetail> findPublicProduct(long productId) {
		return jdbcClient.sql(PRODUCT_COLUMNS + ", p.description " + PUBLIC_PRODUCTS + " AND p.id = :id")
				.param("id", productId)
				.query((row, rowNumber) -> {
					var summary = mapSummary(row, rowNumber);
					return new ProductDetail(summary.id(), summary.name(), summary.slug(), summary.price(),
							summary.currency(), summary.category(), row.getString("description"));
				})
				.optional();
	}

	@Override
	public List<CategorySummary> findActiveCategories() {
		return jdbcClient.sql("SELECT id, name, slug FROM categories WHERE is_active = TRUE ORDER BY name, id")
				.query((row, rowNumber) -> new CategorySummary(row.getLong("id"), row.getString("name"),
						row.getString("slug")))
				.list();
	}

	private static String orderBy(CatalogSearchCriteria criteria) {
		String primary = switch (criteria.sort()) {
			case NAME_ASC -> "p.name ASC";
			case NAME_DESC -> "p.name DESC";
			case PRICE_ASC -> "p.price ASC";
			case PRICE_DESC -> "p.price DESC";
			case NEWEST -> "p.created_at DESC";
		};
		return primary + ", p.id ASC";
	}

	private static String escapeLikeWildcards(String text) {
		return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}

	private static ProductSummary mapSummary(ResultSet row, int rowNumber) throws SQLException {
		return new ProductSummary(row.getLong("id"), row.getString("name"), row.getString("slug"),
				row.getBigDecimal("price"), row.getString("currency"),
				new CategoryReference(row.getLong("category_id"), row.getString("category_name")));
	}
}
