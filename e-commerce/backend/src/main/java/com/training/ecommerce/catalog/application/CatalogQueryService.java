package com.training.ecommerce.catalog.application;

import com.training.ecommerce.catalog.domain.CatalogSearchCriteria;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public catalog use cases. Read-only transactions keep a page's count and rows consistent. */
@Service
@Transactional(readOnly = true)
public class CatalogQueryService {

	private final CatalogReadRepository catalogReadRepository;

	CatalogQueryService(CatalogReadRepository catalogReadRepository) {
		this.catalogReadRepository = catalogReadRepository;
	}

	public ProductPage searchProducts(CatalogSearchCriteria criteria) {
		return catalogReadRepository.searchPublicProducts(criteria);
	}

	public Optional<ProductDetail> findProduct(long productId) {
		return catalogReadRepository.findPublicProduct(productId);
	}

	public List<CategorySummary> listCategories() {
		return catalogReadRepository.findActiveCategories();
	}
}
