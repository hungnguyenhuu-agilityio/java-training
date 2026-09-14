package com.training.ecommerce;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import com.training.architecturefixture.adapter.PersistenceAdapter;
import com.training.architecturefixture.domain.IllegalDomainPolicy;

class ArchitectureRulesTests {

	private static final ArchRule DOMAIN_AND_APPLICATION_LAYER_INDEPENDENCE = noClasses()
			.that().resideInAnyPackage("..domain..", "..application..")
			.should().dependOnClassesThat()
			.resideInAnyPackage(
					"..adapter..",
					"org.springframework.web..",
					"org.springframework.data..",
					"jakarta.persistence..",
					"com.stripe..")
				.allowEmptyShould(true);

	@Test
	void should_use_training_package_namespace() {
		assertThat(ECommerceApplication.class.getPackageName()).isEqualTo("com.training.ecommerce");
	}

	@Test
	void should_keep_application_modules_acyclic() {
		ApplicationModules.of(ECommerceApplication.class).verify();
	}

	@Test
	void should_detect_only_explicit_business_modules() {
		var moduleIdentifiers = ApplicationModules.of(ECommerceApplication.class).stream()
				.map(module -> module.getIdentifier().toString())
				.toList();

		assertThat(moduleIdentifiers).containsExactlyInAnyOrder(
				"cart",
				"catalog",
				"identity",
				"inventory",
				"notification",
				"ordering",
				"payment",
				"shareddomain");
	}

	@Test
	void should_keep_domain_and_application_layers_independent_from_adapters_and_frameworks() {
		var applicationClasses = new ClassFileImporter().importPackages("com.training.ecommerce");

		DOMAIN_AND_APPLICATION_LAYER_INDEPENDENCE.check(applicationClasses);
	}

	@Test
	void should_reject_domain_dependencies_on_adapters_and_frameworks() {
		var fixtureClasses = new ClassFileImporter().importPackages("com.training.architecturefixture");

		assertThatThrownBy(() -> DOMAIN_AND_APPLICATION_LAYER_INDEPENDENCE.check(fixtureClasses))
				.isInstanceOf(AssertionError.class)
				.hasMessageContaining(IllegalDomainPolicy.class.getName())
				.hasMessageContaining(PersistenceAdapter.class.getName())
				.hasMessageContaining("jakarta.persistence.EntityManager");
	}
}
