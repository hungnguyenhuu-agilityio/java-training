package com.training.ecommerce.catalog.adapter.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.training.ecommerce.catalog.CatalogMySqlTestSupport;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTest extends CatalogMySqlTestSupport {

	private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

	@Autowired
	private MockMvc mockMvc;

	private long lighting;
	private long furniture;
	private long deskLamp;
	private long oakChair;
	private long hiddenLamp;

	@BeforeEach
	void seedMixedCatalog() {
		deleteCatalog();
		lighting = insertCategory("Lighting", true);
		furniture = insertCategory("Furniture", true);
		long retired = insertCategory("Retired", false);
		deskLamp = insertProduct(lighting, "Desk Lamp", "19.90");
		oakChair = insertProduct(furniture, "Oak Chair", "49.00");
		hiddenLamp = insertProduct(lighting, "Hidden Lamp", "9.99", false, Instant.parse("2026-01-01T00:00:00Z"));
		insertProduct(retired, "Retired Stool", "15.00");
	}

	@Test
	void should_list_active_products_anonymously_with_default_paging_and_name_order() throws Exception {
		mockMvc.perform(get("/api/catalog/products"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[0].id").value(deskLamp))
				.andExpect(jsonPath("$.content[0].name").value("Desk Lamp"))
				.andExpect(jsonPath("$.content[0].slug").isString())
				.andExpect(jsonPath("$.content[0].price").value(19.90))
				.andExpect(jsonPath("$.content[0].currency").value("USD"))
				.andExpect(jsonPath("$.content[0].category.id").value(lighting))
				.andExpect(jsonPath("$.content[0].category.name").value("Lighting"))
				.andExpect(jsonPath("$.content[0].description").doesNotExist())
				.andExpect(jsonPath("$.content[1].id").value(oakChair))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(12))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(1));
	}

	@Test
	void should_serialize_price_with_two_decimal_places() throws Exception {
		mockMvc.perform(get("/api/catalog/products").param("q", "desk"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("\"price\":19.90")));
	}

	@Test
	void should_apply_search_category_sort_and_paging_parameters() throws Exception {
		mockMvc.perform(get("/api/catalog/products")
				.param("q", "  LAMP ")
				.param("categoryId", String.valueOf(lighting))
				.param("sort", "price_desc")
				.param("page", "0")
				.param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id").value(deskLamp))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(get("/api/catalog/products").param("sort", "price_desc"))
				.andExpect(jsonPath("$.content[0].id").value(oakChair));
	}

	@Test
	void should_treat_blank_search_text_as_absent() throws Exception {
		mockMvc.perform(get("/api/catalog/products").param("q", "   "))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void should_return_an_empty_page_beyond_the_end() throws Exception {
		mockMvc.perform(get("/api/catalog/products").param("page", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(0)))
				.andExpect(jsonPath("$.page").value(5))
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@ParameterizedTest(name = "{0}={1} is rejected")
	@CsvSource({
			"page, -1",
			"page, abc",
			"size, 0",
			"size, 51",
			"sort, cheapest",
			"categoryId, 0",
			"categoryId, x"
	})
	void should_reject_invalid_list_parameters_with_field_violations(String field, String value) throws Exception {
		expectValidationProblem(mockMvc.perform(get("/api/catalog/products").param(field, value)), field,
				"/api/catalog/products");
	}

	@Test
	void should_reject_search_text_longer_than_one_hundred_characters() throws Exception {
		expectValidationProblem(mockMvc.perform(get("/api/catalog/products").param("q", "x".repeat(101))), "q",
				"/api/catalog/products");
	}

	@Test
	void should_return_an_active_product_detail_anonymously() throws Exception {
		mockMvc.perform(get("/api/catalog/products/{id}", deskLamp))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(deskLamp))
				.andExpect(jsonPath("$.name").value("Desk Lamp"))
				.andExpect(jsonPath("$.price").value(19.90))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(jsonPath("$.category.id").value(lighting))
				.andExpect(jsonPath("$.description").value("Description of Desk Lamp"));
	}

	@Test
	void should_return_null_description_when_absent() throws Exception {
		jdbcClient.sql("UPDATE products SET description = NULL WHERE id = :id").param("id", deskLamp).update();

		mockMvc.perform(get("/api/catalog/products/{id}", deskLamp))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value(nullValue()));
	}

	@Test
	void should_answer_inactive_and_unknown_products_with_the_same_not_found_problem() throws Exception {
		var inactive = mockMvc.perform(get("/api/catalog/products/{id}", hiddenLamp));
		var unknown = mockMvc.perform(get("/api/catalog/products/{id}", 987654321L));

		for (var result : new ResultActions[] { inactive, unknown }) {
			result.andExpect(status().isNotFound())
					.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
					.andExpect(jsonPath("$.type").value("about:blank"))
					.andExpect(jsonPath("$.title").value("Not Found"))
					.andExpect(jsonPath("$.status").value(404))
					.andExpect(jsonPath("$.detail").value("Product not found."))
					.andExpect(jsonPath("$.violations").doesNotExist());
		}
		inactive.andExpect(jsonPath("$.instance").value("/api/catalog/products/" + hiddenLamp));
	}

	@Test
	void should_reject_a_non_numeric_product_id() throws Exception {
		expectValidationProblem(mockMvc.perform(get("/api/catalog/products/{id}", "lamp")), "id",
				"/api/catalog/products/lamp");
	}

	@Test
	void should_list_active_categories_by_name() throws Exception {
		mockMvc.perform(get("/api/catalog/categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id").value(furniture))
				.andExpect(jsonPath("$[0].name").value("Furniture"))
				.andExpect(jsonPath("$[0].slug").isString())
				.andExpect(jsonPath("$[1].name").value("Lighting"));
	}

	@Test
	void should_keep_non_read_catalog_requests_protected() throws Exception {
		mockMvc.perform(post("/api/catalog/products"))
				.andExpect(status().is4xxClientError())
				.andExpect(status().is(not(405)));
	}

	private static void expectValidationProblem(ResultActions result, String field, String instance) throws Exception {
		result.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("about:blank"))
				.andExpect(jsonPath("$.title").value("Bad Request"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isString())
				.andExpect(jsonPath("$.instance").value(instance))
				.andExpect(jsonPath("$.violations", hasSize(1)))
				.andExpect(jsonPath("$.violations[0].field").value(field))
				.andExpect(jsonPath("$.violations[0].message").isString())
				.andExpect(content().string(not(containsString("Exception"))))
				.andExpect(content().string(not(containsString("java."))));
	}
}
