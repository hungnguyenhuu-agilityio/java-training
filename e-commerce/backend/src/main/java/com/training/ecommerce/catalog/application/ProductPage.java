package com.training.ecommerce.catalog.application;

import java.util.List;

/** One page of public products; a page beyond the end has empty content but real totals. */
public record ProductPage(List<ProductSummary> content, int page, int size, long totalElements, int totalPages) {

	public static ProductPage of(List<ProductSummary> content, int page, int size, long totalElements) {
		return new ProductPage(content, page, size, totalElements, (int) ((totalElements + size - 1) / size));
	}
}
