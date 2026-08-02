package br.com.f2e.ovenplatform.catalog.application;

import br.com.f2e.ovenplatform.catalog.application.category.CategoryService;
import br.com.f2e.ovenplatform.catalog.application.product.CatalogProductLookupService;
import br.com.f2e.ovenplatform.catalog.application.product.CatalogService;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResultResolver;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantService;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaCategoryRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductVariantRepositoryAdapter;
import br.com.f2e.ovenplatform.media.application.api.AvailableImageLookup;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({
  CatalogService.class,
  CatalogProductLookupService.class,
  ProductVariantService.class,
  CategoryService.class,
  ProductVariantResultResolver.class,
  JpaProductRepositoryAdapter.class,
  JpaCategoryRepositoryAdapter.class,
  JpaProductVariantRepositoryAdapter.class
})
abstract class CatalogDataJpaIntegrationTest extends DataJpaIntegrationTest {

  @MockitoBean protected AvailableImageLookup availableImageLookup;
}
