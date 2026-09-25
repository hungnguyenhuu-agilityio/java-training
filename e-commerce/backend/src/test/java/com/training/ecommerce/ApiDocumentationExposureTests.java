package com.training.ecommerce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:production-docs;MODE=MySQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.liquibase.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("production")
class ApiDocumentationExposureTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void should_not_publish_api_docs_or_interactive_ui_in_production() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
		mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
	}
}
