package com.training.ecommerce.catalog.adapter.web;

import com.training.ecommerce.catalog.application.CatalogQueryService;
import com.training.ecommerce.catalog.application.CategorySummary;
import com.training.ecommerce.catalog.application.ProductDetail;
import com.training.ecommerce.catalog.application.ProductPage;
import com.training.ecommerce.catalog.domain.CatalogSearchCriteria;
import com.training.ecommerce.catalog.domain.CatalogSort;
import com.training.ecommerce.configuration.ProblemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Public, read-only catalog API; access rules live in {@code SecurityConfiguration}. */
@RestController
@RequestMapping(path = "/api/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Catalog", description = "Public product discovery")
@ApiResponse(responseCode = "500", description = "Unexpected failure",
		content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
				schema = @Schema(implementation = ProblemResponse.class)))
class CatalogController {

	private final CatalogQueryService catalogQueryService;

	CatalogController(CatalogQueryService catalogQueryService) {
		this.catalogQueryService = catalogQueryService;
	}

	@GetMapping("/products")
	@Operation(summary = "Search active products",
			description = "Case-insensitive name search, category filter and sorting; ties are ordered by id.")
	@ApiResponse(responseCode = "200", description = "A page of active products; empty content beyond the last page")
	@ApiResponse(responseCode = "400", description = "Invalid query parameters",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemResponse.class)))
	ProductPage searchProducts(
			@Parameter(description = "Name search text; trimmed, blank means no filter")
			@RequestParam(required = false) @Size(max = CatalogSearchCriteria.MAX_QUERY_LENGTH) String q,
			@RequestParam(required = false) @Positive Long categoryId,
			@Parameter(schema = @Schema(type = "string", defaultValue = "name_asc",
					allowableValues = { "name_asc", "name_desc", "price_asc", "price_desc", "newest" }))
			@RequestParam(defaultValue = "name_asc")
			@Pattern(regexp = "^(" + CatalogSort.ALLOWED_VALUES_PATTERN + ")$",
					message = "must be one of name_asc, name_desc, price_asc, price_desc, newest") String sort,
			@Parameter(description = "Zero-based page index")
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "12") @Min(1) @Max(CatalogSearchCriteria.MAX_PAGE_SIZE) int size) {
		var criteria = new CatalogSearchCriteria(q, categoryId, CatalogSort.fromValue(sort).orElseThrow(), page, size);
		return catalogQueryService.searchProducts(criteria);
	}

	@GetMapping("/products/{id}")
	@Operation(summary = "Get an active product")
	@ApiResponse(responseCode = "200", description = "The active product")
	@ApiResponse(responseCode = "400", description = "Non-numeric product id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemResponse.class)))
	@ApiResponse(responseCode = "404", description = "Unknown or inactive product",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemResponse.class)))
	ProductDetail getProduct(@PathVariable long id) {
		return catalogQueryService.findProduct(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
	}

	@GetMapping("/categories")
	@Operation(summary = "List active categories", description = "Ordered by name.")
	@ApiResponse(responseCode = "200", description = "Active categories")
	List<CategorySummary> listCategories() {
		return catalogQueryService.listCategories();
	}
}
