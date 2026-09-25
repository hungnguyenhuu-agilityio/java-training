package com.training.ecommerce.catalog.application;

/** An active category offered as a public catalog filter. */
public record CategorySummary(long id, String name, String slug) {
}
