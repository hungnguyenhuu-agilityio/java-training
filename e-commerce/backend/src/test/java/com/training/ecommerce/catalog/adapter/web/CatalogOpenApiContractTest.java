package com.training.ecommerce.catalog.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.training.ecommerce.catalog.application.CatalogQueryService;
import com.training.ecommerce.catalog.domain.CatalogSort;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Structural contract checks on the generated OpenAPI document (not a snapshot), plus checks that the
 * Problem Details bodies produced at runtime only use fields the document declares.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogOpenApiContractTest {

	private static final String PRODUCTS = "$.paths['/api/catalog/products'].get";
	private static final String PRODUCT = "$.paths['/api/catalog/products/{id}'].get";
	private static final String CATEGORIES = "$.paths['/api/catalog/categories'].get";
	private static final String PROBLEM_REF = "#/components/schemas/ProblemDetail";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CatalogQueryService catalogQueryService;

	private DocumentContext apiDocs;

	@BeforeEach
	void fetchApiDocs() throws Exception {
		apiDocs = JsonPath.parse(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString());
	}

	@Test
	void should_publish_only_read_operations_for_the_catalog_paths() {
		for (var path : List.of("/api/catalog/products", "/api/catalog/products/{id}", "/api/catalog/categories")) {
			Map<String, Object> operations = apiDocs.read("$.paths['" + path + "']");
			assertThat(operations.keySet()).as(path).containsExactly("get");
		}
	}

	@Test
	void should_document_catalog_operations_as_public() {
		assertThat(apiDocs.jsonString()).doesNotContain("\"security\"");
		for (var operation : List.of(PRODUCTS, PRODUCT, CATEGORIES)) {
			Map<String, Object> definition = apiDocs.read(operation);
			assertThat(definition).as(operation).doesNotContainKey("security");
		}
	}

	@Test
	void should_document_bounded_list_query_parameters() {
		List<String> names = apiDocs.read(PRODUCTS + ".parameters[*].name");
		assertThat(names).containsExactly("q", "categoryId", "sort", "page", "size");
		assertThat(apiDocs.read(PRODUCTS + ".parameters[*].in", List.class)).containsOnly("query");
		assertThat(apiDocs.read(PRODUCTS + ".parameters[*].required", List.class)).containsOnly(false);

		assertThat(parameterSchema("q")).containsEntry("type", "string").containsEntry("maxLength", 100);
		assertThat(parameterSchema("categoryId")).containsEntry("type", "integer").containsEntry("format", "int64")
				.containsEntry("exclusiveMinimum", 0);
		assertThat(parameterSchema("sort")).containsEntry("type", "string").containsEntry("default", "name_asc");
		assertThat(apiDocs.read(PRODUCTS + ".parameters[?(@.name == 'sort')].schema.enum[*]", List.class))
				.containsExactlyElementsOf(Arrays.stream(CatalogSort.values()).map(CatalogSort::value).toList());
		assertThat(parameterSchema("page")).containsEntry("type", "integer").containsEntry("minimum", 0)
				.containsEntry("default", 0);
		assertThat(parameterSchema("size")).containsEntry("type", "integer").containsEntry("minimum", 1)
				.containsEntry("maximum", 50).containsEntry("default", 12);
	}

	@Test
	void should_document_the_numeric_product_id_path_parameter() {
		assertThat(apiDocs.read(PRODUCT + ".parameters[*].name", List.class)).containsExactly("id");
		assertThat(apiDocs.read(PRODUCT + ".parameters[0].in", String.class)).isEqualTo("path");
		assertThat(apiDocs.read(PRODUCT + ".parameters[0].required", Boolean.class)).isTrue();
		assertThat(apiDocs.read(PRODUCT + ".parameters[0].schema", Map.class))
				.containsEntry("type", "integer").containsEntry("format", "int64");
	}

	@Test
	void should_document_concrete_success_schemas() {
		assertThat(successSchemaRef(PRODUCTS)).isEqualTo("#/components/schemas/ProductPage");
		assertThat(successSchemaRef(PRODUCT)).isEqualTo("#/components/schemas/ProductDetail");
		assertThat(apiDocs.read(CATEGORIES + ".responses['200'].content['application/json'].schema.items['$ref']",
				String.class)).isEqualTo("#/components/schemas/CategorySummary");

		assertThat(schemaProperties("ProductPage"))
				.containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages");
		assertThat(apiDocs.read("$.components.schemas.ProductPage.properties.content.items['$ref']", String.class))
				.isEqualTo("#/components/schemas/ProductSummary");
		assertThat(schemaProperties("ProductSummary"))
				.containsExactlyInAnyOrder("id", "name", "slug", "price", "currency", "category");
		assertThat(apiDocs.read("$.components.schemas.ProductSummary.properties.price.type", String.class))
				.isEqualTo("number");
		assertThat(schemaProperties("ProductDetail"))
				.containsExactlyInAnyOrder("id", "name", "slug", "price", "currency", "category", "description");
		assertThat(schemaProperties("CategoryReference")).containsExactlyInAnyOrder("id", "name");
		assertThat(schemaProperties("CategorySummary")).containsExactlyInAnyOrder("id", "name", "slug");
	}

	@Test
	void should_document_problem_details_failure_responses() {
		assertThat(problemResponseRef(PRODUCTS, "400")).isEqualTo(PROBLEM_REF);
		assertThat(problemResponseRef(PRODUCTS, "500")).isEqualTo(PROBLEM_REF);
		assertThat(problemResponseRef(PRODUCT, "400")).isEqualTo(PROBLEM_REF);
		assertThat(problemResponseRef(PRODUCT, "404")).isEqualTo(PROBLEM_REF);
		assertThat(problemResponseRef(PRODUCT, "500")).isEqualTo(PROBLEM_REF);
		assertThat(problemResponseRef(CATEGORIES, "500")).isEqualTo(PROBLEM_REF);
		assertThat(schemaProperties("ProblemDetail"))
				.containsExactlyInAnyOrder("type", "title", "status", "detail", "instance", "violations");
		assertThat(schemaProperties("FieldViolation")).containsExactlyInAnyOrder("field", "message");
	}

	@Test
	void should_emit_runtime_validation_problems_that_match_the_documented_schema() throws Exception {
		var body = JsonPath.parse(problemBody("/api/catalog/products?size=99", 400));

		assertThat(body.read("$", Map.class).keySet()).isEqualTo(schemaProperties("ProblemDetail"));
		assertThat(body.read("$.violations[0]", Map.class).keySet()).isEqualTo(schemaProperties("FieldViolation"));
	}

	@Test
	void should_emit_runtime_not_found_problems_that_match_the_documented_schema() throws Exception {
		given(catalogQueryService.findProduct(42L)).willReturn(Optional.empty());

		var body = JsonPath.parse(problemBody("/api/catalog/products/42", 404));

		assertThat(schemaProperties("ProblemDetail")).containsAll(body.read("$", Map.class).keySet());
		assertThat(body.read("$", Map.class).keySet()).containsExactlyInAnyOrder("type", "title", "status", "detail",
				"instance");
	}

	@Test
	void should_emit_safe_documented_problems_for_unexpected_failures() throws Exception {
		given(catalogQueryService.listCategories())
				.willThrow(new IllegalStateException("SELECT secret FROM categories failed"));

		var rawBody = problemBody("/api/catalog/categories", 500);

		assertThat(rawBody).doesNotContain("SELECT", "secret", "IllegalStateException", "java.");
		var body = JsonPath.parse(rawBody);
		assertThat(schemaProperties("ProblemDetail")).containsAll(body.read("$", Map.class).keySet());
		assertThat(body.read("$.detail", String.class)).isEqualTo("An unexpected error occurred.");
	}

	private String problemBody(String uri, int expectedStatus) throws Exception {
		var response = mockMvc.perform(get(uri)).andReturn().getResponse();
		assertThat(response.getStatus()).isEqualTo(expectedStatus);
		assertThat(response.getContentType()).isEqualTo("application/problem+json");
		return response.getContentAsString();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parameterSchema(String name) {
		List<Map<String, Object>> schemas = apiDocs.read(PRODUCTS + ".parameters[?(@.name == '" + name + "')].schema");
		return schemas.getFirst();
	}

	private String successSchemaRef(String operation) {
		return apiDocs.read(operation + ".responses['200'].content['application/json'].schema['$ref']");
	}

	private String problemResponseRef(String operation, String status) {
		return apiDocs.read(operation + ".responses['" + status + "'].content['application/problem+json'].schema['$ref']");
	}

	private java.util.Set<String> schemaProperties(String schemaName) {
		Map<String, Object> properties = apiDocs.read("$.components.schemas." + schemaName + ".properties");
		return properties.keySet();
	}
}
