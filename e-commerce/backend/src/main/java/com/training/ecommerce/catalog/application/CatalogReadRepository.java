package com.training.ecommerce.catalog.application;

import com.training.ecommerce.catalog.domain.CatalogSearchCriteria;
import java.util.List;
import java.util.Optional;

/**
 * Read-side port for public catalog queries. "Public" means the product is active and its category
 * is active; everything else is invisible, including to detail lookups.
 */
public interface CatalogReadRepository {

	ProductPage searchPublicProducts(CatalogSearchCriteria criteria);

	Optional<ProductDetail> findPublicProduct(long productId);

	List<CategorySummary> findActiveCategories();
}
