package com.training.ecommerce.catalog.application;

import java.math.BigDecimal;

/** A public product as shown in catalog listings; {@code currency} is an ISO 4217 code. */
public record ProductSummary(long id, String name, String slug, BigDecimal price, String currency,
		CategoryReference category) {
}
