package com.training.ecommerce.configuration;

/** One rejected request field inside a Problem Details {@code violations} array. */
public record FieldViolation(String field, String message) {
}
