package com.training.ecommerce.catalog.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.training.ecommerce.catalog.CatalogMySqlTestSupport;
import com.training.ecommerce.catalog.application.CatalogReadRepository;
import com.training.ecommerce.catalog.application.CategoryReference;
import com.training.ecommerce.catalog.application.CategorySummary;
import com.training.ecommerce.catalog.application.ProductSummary;
import com.training.ecommerce.catalog.domain.CatalogSearchCriteria;
import com.training.ecommerce.catalog.domain.CatalogSort;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JdbcCatalogReadRepositoryTest extends CatalogMySqlTestSupport {

	@Autowired
	private CatalogReadRepository repository;

	private long lighting;
	private long furniture;

	@BeforeEach
	void seedMixedCatalog() {
		deleteCatalog();
		lighting = insertCategory("Lighting", true);
		furniture = insertCategory("Furniture", true);
	}

	@Test
	void should_list_only_active_products_in_active_categories() {
		long retired = insertCategory("Retired", false);
		long visible = insertProduct(lighting, "Desk Lamp", "19.99");
		insertProduct(lighting, "Hidden Lamp", "9.99", false, Instant.parse("2026-01-01T00:00:00Z"));
		insertProduct(retired, "Orphaned Chair", "49.00");

		var page = repository.searchPublicProducts(criteria(null, null, CatalogSort.NAME_ASC, 0, 12));

		assertThat(page.content()).extracting(ProductSummary::id).containsExactly(visible);
		assertThat(page.totalElements()).isEqualTo(1);
	}

	@Test
	void should_map_the_public_product_summary_fields() {
		long id = insertProduct(lighting, "Desk Lamp", "19.90");

		var summary = repository.searchPublicProducts(criteria(null, null, null, 0, 12)).content().getFirst();

		assertThat(summary.id()).isEqualTo(id);
		assertThat(summary.name()).isEqualTo("Desk Lamp");
		assertThat(summary.slug()).startsWith("product-");
		assertThat(summary.price()).isEqualByComparingTo("19.90");
		assertThat(summary.price().scale()).isEqualTo(2);
		assertThat(summary.currency()).isEqualTo("USD");
		assertThat(summary.category()).isEqualTo(new CategoryReference(lighting, "Lighting"));
	}

	@Test
	void should_match_search_text_as_a_case_insensitive_substring_of_the_name() {
		long lamp = insertProduct(lighting, "Brass DESK Lamp", "19.99");
		insertProduct(furniture, "Oak Chair", "49.00");

		var page = repository.searchPublicProducts(criteria("desk", null, null, 0, 12));

		assertThat(page.content()).extracting(ProductSummary::id).containsExactly(lamp);
	}

	@Test
	void should_follow_the_accent_insensitive_mysql_collation_for_search() {
		long cafe = insertProduct(furniture, "Café Table", "89.00");

		var page = repository.searchPublicProducts(criteria("cafe", null, null, 0, 12));

		assertThat(page.content()).extracting(ProductSummary::id).containsExactly(cafe);
	}

	@Test
	void should_treat_like_wildcards_in_search_text_literally() {
		long literal = insertProduct(lighting, "100% Cotton Shade", "5.00");
		insertProduct(lighting, "1000 Lumen Bulb", "5.00");
		insertProduct(lighting, "Shade_Pro", "5.00");
		insertProduct(lighting, "Shade Pro", "5.00");

		assertThat(repository.searchPublicProducts(criteria("100%", null, null, 0, 12)).content())
				.extracting(ProductSummary::id).containsExactly(literal);
		assertThat(repository.searchPublicProducts(criteria("e_P", null, null, 0, 12)).content())
				.extracting(ProductSummary::name).containsExactly("Shade_Pro");
		assertThat(repository.searchPublicProducts(criteria("!", null, null, 0, 12)).content()).isEmpty();
	}

	@Test
	void should_filter_by_category() {
		insertProduct(lighting, "Desk Lamp", "19.99");
		long chair = insertProduct(furniture, "Oak Chair", "49.00");

		var page = repository.searchPublicProducts(criteria(null, furniture, null, 0, 12));

		assertThat(page.content()).extracting(ProductSummary::id).containsExactly(chair);
	}

	@Test
	void should_order_by_each_approved_sort_with_a_stable_id_tie_break() {
		long cheapOld = insertProduct(lighting, "Bulb", "5.00", true, Instant.parse("2026-01-01T00:00:00Z"));
		long twinA = insertProduct(lighting, "Lamp", "20.00", true, Instant.parse("2026-03-01T00:00:00Z"));
		long twinB = insertProduct(lighting, "Lamp", "20.00", true, Instant.parse("2026-03-01T00:00:00Z"));
		long priceyNew = insertProduct(lighting, "Shade", "30.00", true, Instant.parse("2026-06-01T00:00:00Z"));

		assertThat(idsSortedBy(CatalogSort.NAME_ASC)).containsExactly(cheapOld, twinA, twinB, priceyNew);
		assertThat(idsSortedBy(CatalogSort.NAME_DESC)).containsExactly(priceyNew, twinA, twinB, cheapOld);
		assertThat(idsSortedBy(CatalogSort.PRICE_ASC)).containsExactly(cheapOld, twinA, twinB, priceyNew);
		assertThat(idsSortedBy(CatalogSort.PRICE_DESC)).containsExactly(priceyNew, twinA, twinB, cheapOld);
		assertThat(idsSortedBy(CatalogSort.NEWEST)).containsExactly(priceyNew, twinA, twinB, cheapOld);
	}

	@Test
	void should_paginate_with_totals_and_return_an_empty_page_beyond_the_end() {
		for (int index = 1; index <= 5; index++) {
			insertProduct(lighting, "Lamp " + index, "10.00");
		}

		var secondPage = repository.searchPublicProducts(criteria(null, null, null, 1, 2));
		var beyondEnd = repository.searchPublicProducts(criteria(null, null, null, 7, 2));

		assertThat(secondPage.content()).extracting(ProductSummary::name).containsExactly("Lamp 3", "Lamp 4");
		assertThat(secondPage.page()).isEqualTo(1);
		assertThat(secondPage.size()).isEqualTo(2);
		assertThat(secondPage.totalElements()).isEqualTo(5);
		assertThat(secondPage.totalPages()).isEqualTo(3);
		assertThat(beyondEnd.content()).isEmpty();
		assertThat(beyondEnd.totalElements()).isEqualTo(5);
		assertThat(beyondEnd.totalPages()).isEqualTo(3);
	}

	@Test
	void should_return_an_empty_page_when_filters_match_nothing() {
		insertProduct(lighting, "Desk Lamp", "19.99");

		var page = repository.searchPublicProducts(criteria("sofa", lighting, null, 0, 12));

		assertThat(page.content()).isEmpty();
		assertThat(page.totalElements()).isZero();
		assertThat(page.totalPages()).isZero();
	}

	@Test
	void should_find_an_active_product_with_its_description() {
		long id = insertProduct(lighting, "Desk Lamp", "19.99");

		var detail = repository.findPublicProduct(id);

		assertThat(detail).hasValueSatisfying(product -> {
			assertThat(product.name()).isEqualTo("Desk Lamp");
			assertThat(product.description()).isEqualTo("Description of Desk Lamp");
			assertThat(product.price()).isEqualTo(new BigDecimal("19.99"));
			assertThat(product.category()).isEqualTo(new CategoryReference(lighting, "Lighting"));
		});
	}

	@Test
	void should_hide_inactive_unknown_and_inactive_category_products_from_detail() {
		long retired = insertCategory("Retired", false);
		long inactive = insertProduct(lighting, "Hidden Lamp", "9.99", false, Instant.parse("2026-01-01T00:00:00Z"));
		long orphaned = insertProduct(retired, "Orphaned Chair", "49.00");

		assertThat(repository.findPublicProduct(inactive)).isEmpty();
		assertThat(repository.findPublicProduct(orphaned)).isEmpty();
		assertThat(repository.findPublicProduct(Long.MAX_VALUE)).isEmpty();
	}

	@Test
	void should_hide_a_product_deactivated_between_list_and_detail() {
		long id = insertProduct(lighting, "Desk Lamp", "19.99");
		assertThat(repository.searchPublicProducts(criteria(null, null, null, 0, 12)).content()).hasSize(1);

		jdbcClient.sql("UPDATE products SET is_active = FALSE WHERE id = :id").param("id", id).update();

		assertThat(repository.findPublicProduct(id)).isEmpty();
	}

	@Test
	void should_list_only_active_categories_ordered_by_name() {
		insertCategory("Archived", false);

		assertThat(repository.findActiveCategories())
				.extracting(CategorySummary::id, CategorySummary::name)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(furniture, "Furniture"),
						org.assertj.core.groups.Tuple.tuple(lighting, "Lighting"));
		assertThat(repository.findActiveCategories()).allSatisfy(category -> assertThat(category.slug()).isNotBlank());
	}

	private java.util.List<Long> idsSortedBy(CatalogSort sort) {
		return repository.searchPublicProducts(criteria(null, null, sort, 0, 12)).content().stream()
				.map(ProductSummary::id)
				.toList();
	}

	private static CatalogSearchCriteria criteria(String query, Long categoryId, CatalogSort sort, int page, int size) {
		return new CatalogSearchCriteria(query, categoryId, sort, page, size);
	}
}
