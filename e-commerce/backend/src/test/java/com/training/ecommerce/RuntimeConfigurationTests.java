package com.training.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;

class RuntimeConfigurationTests {

	@Test
	void should_bind_server_port_to_the_platform_port_variable() throws IOException {
		var environment = environmentWith("application.properties");
		environment.setProperty("PORT", "18081");

		assertThat(environment.getProperty("server.port")).isEqualTo("18081");
	}

	@Test
	void should_default_server_port_to_8080_without_a_platform_port() throws IOException {
		var environment = environmentWith("application.properties");

		assertThat(environment.getProperty("server.port")).isEqualTo("8080");
	}

	@Test
	void should_require_explicit_datasource_settings_outside_the_local_profile() throws IOException {
		var environment = environmentWith("application.properties");

		for (var key : new String[] { "spring.datasource.url", "spring.datasource.username", "spring.datasource.password" }) {
			assertThatThrownBy(() -> environment.getProperty(key))
					.as("%s must not fall back to a default", key)
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void should_provide_host_mode_datasource_defaults_in_the_local_profile() throws IOException {
		var environment = environmentWith("application-local.properties", "application.properties");

		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:mysql://localhost:13306/ecommerce?allowPublicKeyRetrieval=true&useSSL=false");
		assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("ecommerce");
		assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("ecommerce-local");
	}

	@Test
	void should_follow_the_compose_mysql_host_port_override_in_the_local_profile() throws IOException {
		var environment = environmentWith("application-local.properties", "application.properties");
		environment.setProperty("MYSQL_HOST_PORT", "23306");

		assertThat(environment.getProperty("spring.datasource.url"))
				.isEqualTo("jdbc:mysql://localhost:23306/ecommerce?allowPublicKeyRetrieval=true&useSSL=false");
	}

	private MockEnvironment environmentWith(String... resourceNames) throws IOException {
		var environment = new MockEnvironment();
		for (var resourceName : resourceNames) {
			environment.getPropertySources().addLast(new ResourcePropertySource(new ClassPathResource(resourceName)));
		}
		return environment;
	}
}
