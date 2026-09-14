package com.training.ecommerce.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApiDocumentationConfiguration {

	@Bean
	OpenAPI ecommerceOpenApi() {
		return new OpenAPI()
				.components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")))
				.info(new Info()
				.title("E-Commerce API")
				.version("v1")
				.description("Generated contract for implemented E-Commerce HTTP endpoints."));
	}
}
