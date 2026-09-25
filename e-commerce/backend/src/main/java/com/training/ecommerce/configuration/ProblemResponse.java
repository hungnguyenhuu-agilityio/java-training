package com.training.ecommerce.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * OpenAPI description of the RFC 7807 bodies written by {@link ProblemDetailsExceptionHandler}. Runtime
 * responses are Spring {@code ProblemDetail}s; a contract test keeps both shapes aligned.
 */
@Schema(name = "ProblemDetail", description = "RFC 7807 Problem Details (application/problem+json)")
public record ProblemResponse(
		String type,
		String title,
		int status,
		String detail,
		String instance,
		@Schema(description = "Present only for request validation failures") List<FieldViolation> violations) {
}
