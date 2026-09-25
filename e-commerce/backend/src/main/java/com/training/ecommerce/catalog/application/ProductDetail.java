package com.training.ecommerce.catalog.application;

import java.math.BigDecimal;

/** A public product with its description; {@code description} may be null. */
public record ProductDetail(long id, String name, String slug, BigDecimal price, String currency,
		CategoryReference category, String description) {
}
