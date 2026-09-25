package com.training.ecommerce.catalog.domain;

import java.util.Arrays;
import java.util.Optional;

/** Public catalog orderings; every ordering is tie-broken by ascending product id. */
public enum CatalogSort {

	NAME_ASC("name_asc"),
	NAME_DESC("name_desc"),
	PRICE_ASC("price_asc"),
	PRICE_DESC("price_desc"),
	NEWEST("newest");

	/** Compile-time copy of the public values, for validation annotations; kept in sync by a unit test. */
	public static final String ALLOWED_VALUES_PATTERN = "name_asc|name_desc|price_asc|price_desc|newest";

	private final String value;

	CatalogSort(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static Optional<CatalogSort> fromValue(String value) {
		return Arrays.stream(values()).filter(sort -> sort.value.equals(value)).findFirst();
	}
}
