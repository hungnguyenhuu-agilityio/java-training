package com.training.architecturefixture.domain;

import com.training.architecturefixture.adapter.PersistenceAdapter;
import jakarta.persistence.EntityManager;

/**
 * Test-only fixture: deliberately violates the inward-dependency rule. It lives outside
 * {@code com.training.ecommerce}, so production architecture checks never import it.
 */
public class IllegalDomainPolicy {

	private PersistenceAdapter persistenceAdapter;

	private EntityManager entityManager;
}
