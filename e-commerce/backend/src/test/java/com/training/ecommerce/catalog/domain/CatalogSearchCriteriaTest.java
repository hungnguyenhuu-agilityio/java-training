package com.training.ecommerce.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CatalogSearchCriteriaTest {

	@Test
	void should_trim_the_search_text() {
		var criteria = new CatalogSearchCriteria("  desk lamp  ", null, CatalogSort.NAME_ASC, 0, 12);

		assertThat(criteria.query()).isEqualTo("desk lamp");
	}

	@Test
	void should_treat_blank_search_text_as_absent() {
		var criteria = new CatalogSearchCriteria("   ", null, CatalogSort.NAME_ASC, 0, 12);

		assertThat(criteria.query()).isNull();
	}

	@Test
	void should_default_to_name_ascending_when_sort_is_absent() {
		var criteria = new CatalogSearchCriteria(null, null, null, 0, 12);

		assertThat(criteria.sort()).isEqualTo(CatalogSort.NAME_ASC);
	}

	@Test
	void should_compute_the_row_offset_without_int_overflow() {
		var criteria = new CatalogSearchCriteria(null, null, CatalogSort.NAME_ASC, Integer.MAX_VALUE, 50);

		assertThat(criteria.offset()).isEqualTo(Integer.MAX_VALUE * 50L);
	}

	@Test
	void should_reject_a_negative_page() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CatalogSearchCriteria(null, null, CatalogSort.NAME_ASC, -1, 12));
	}

	@Test
	void should_reject_a_page_size_outside_the_allowed_bounds() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CatalogSearchCriteria(null, null, CatalogSort.NAME_ASC, 0, 0));
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CatalogSearchCriteria(null, null, CatalogSort.NAME_ASC, 0, 51));
	}

	@Test
	void should_reject_search_text_longer_than_the_maximum() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CatalogSearchCriteria("x".repeat(101), null, CatalogSort.NAME_ASC, 0, 12));
	}

	@Test
	void should_reject_a_non_positive_category_id() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CatalogSearchCriteria(null, 0L, CatalogSort.NAME_ASC, 0, 12));
	}

	@Test
	void should_resolve_sorts_by_their_public_value() {
		assertThat(CatalogSort.fromValue("price_desc")).contains(CatalogSort.PRICE_DESC);
		assertThat(CatalogSort.fromValue("PRICE_DESC")).isEmpty();
		assertThat(CatalogSort.fromValue(null)).isEmpty();
	}

	@Test
	void should_keep_the_allowed_values_pattern_in_sync_with_the_sort_values() {
		var values = Arrays.stream(CatalogSort.values()).map(CatalogSort::value).toList();

		assertThat(values).containsExactly("name_asc", "name_desc", "price_asc", "price_desc", "newest");
		assertThat(CatalogSort.ALLOWED_VALUES_PATTERN).isEqualTo(String.join("|", values));
	}
}
