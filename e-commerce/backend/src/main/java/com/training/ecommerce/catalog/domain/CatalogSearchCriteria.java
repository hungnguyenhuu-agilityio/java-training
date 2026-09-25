package com.training.ecommerce.catalog.domain;

/**
 * A validated public catalog search. Search text is trimmed and blank text means "no text filter";
 * matching semantics (case and accent sensitivity) follow the product name column collation.
 */
public record CatalogSearchCriteria(String query, Long categoryId, CatalogSort sort, int page, int size) {

	public static final int MAX_QUERY_LENGTH = 100;
	public static final int MAX_PAGE_SIZE = 50;

	public CatalogSearchCriteria {
		query = query == null || query.isBlank() ? null : query.trim();
		sort = sort == null ? CatalogSort.NAME_ASC : sort;
		if (query != null && query.length() > MAX_QUERY_LENGTH) {
			throw new IllegalArgumentException("query must be at most " + MAX_QUERY_LENGTH + " characters");
		}
		if (categoryId != null && categoryId <= 0) {
			throw new IllegalArgumentException("categoryId must be positive");
		}
		if (page < 0) {
			throw new IllegalArgumentException("page must not be negative");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
	}

	public long offset() {
		return (long) page * size;
	}
}
