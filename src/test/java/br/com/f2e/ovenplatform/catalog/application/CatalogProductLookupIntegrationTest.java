package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.catalog.application.api.CatalogProductLookup;
import br.com.f2e.ovenplatform.catalog.application.api.SellableProduct;
import br.com.f2e.ovenplatform.catalog.domain.Category;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaCategoryRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductRepositoryAdapter;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({
  CatalogProductLookupService.class,
  JpaProductRepositoryAdapter.class,
  JpaCategoryRepositoryAdapter.class
})
@EnableJpaAuditing
class CatalogProductLookupIntegrationTest {

  @Autowired private CatalogProductLookup catalogProductLookup;
  @Autowired private ProductRepository productRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private SpringDataTenantRepository tenantRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void shouldFindSellableProductsWithCurrentPricesByTenantIdAndIds() {

    var tenant = createTenant();
    var products = createProducts(tenant, 4, true);

    entityManager.flush();
    entityManager.clear();

    var resultMap = products.stream().collect(Collectors.toMap(Product::getId, Product::getPrice));
    var sellableProducts =
        catalogProductLookup.findSellableProducts(tenant.getId(), resultMap.keySet());

    var actualPricesByProductId =
        sellableProducts.stream()
            .collect(Collectors.toMap(SellableProduct::productId, SellableProduct::price));

    assertThat(actualPricesByProductId.keySet())
        .containsExactlyInAnyOrderElementsOf(resultMap.keySet());

    resultMap.forEach(
        (productId, expectedPrice) ->
            assertThat(actualPricesByProductId.get(productId)).isEqualByComparingTo(expectedPrice));
  }

  @Test
  void shouldNotReturnInactiveProducts() {
    var tenant = createTenant();
    var activeProducts = createProducts(tenant, 2, true);
    var inactiveProducts = createProducts(tenant, 4, false);

    entityManager.flush();
    entityManager.clear();

    var requestedProductIds =
        Stream.concat(activeProducts.stream(), inactiveProducts.stream())
            .map(Product::getId)
            .collect(Collectors.toSet());

    var activeProductIds = activeProducts.stream().map(Product::getId).toList();

    var sellableProducts =
        catalogProductLookup.findSellableProducts(tenant.getId(), requestedProductIds);

    assertThat(sellableProducts).hasSize(activeProducts.size());

    assertThat(sellableProducts)
        .extracting(SellableProduct::productId)
        .containsExactlyInAnyOrderElementsOf(activeProductIds);
  }

  @Test
  void shouldNotReturnProductsFromAnotherTenant() {
    var tenant = createTenant();
    var productsFromTenant = createProducts(tenant, 1, true);

    var anotherTenant = createTenant("La bella pizza");
    var productsFromAnotherTenant = createProducts(anotherTenant, 4, true);

    entityManager.flush();
    entityManager.clear();

    var requestedProductIds =
        Stream.concat(productsFromTenant.stream(), productsFromAnotherTenant.stream())
            .map(Product::getId)
            .collect(Collectors.toSet());

    var expectedProductIds = productsFromTenant.stream().map(Product::getId).toList();
    var anotherTenantProductIds = productsFromAnotherTenant.stream().map(Product::getId).toList();

    var sellableProducts =
        catalogProductLookup.findSellableProducts(tenant.getId(), requestedProductIds);

    assertThat(sellableProducts).hasSize(productsFromTenant.size());

    assertThat(sellableProducts)
        .extracting(SellableProduct::productId)
        .containsExactlyInAnyOrderElementsOf(expectedProductIds);

    assertThat(sellableProducts)
        .extracting(SellableProduct::productId)
        .doesNotContainAnyElementsOf(anotherTenantProductIds);
  }

  @Test
  void shouldReturnEmptyWhenNoProductMatches() {

    var tenant = createTenant();
    createProducts(tenant, 3, true);

    entityManager.flush();
    entityManager.clear();

    assertThat(catalogProductLookup.findSellableProducts(tenant.getId(), Set.of(UUID.randomUUID())))
        .isEmpty();
  }

  private List<Product> createProducts(Tenant tenant, int quantity, boolean active) {
    List<Product> products = new ArrayList<>(quantity);
    var category = categoryRepository.save(new Category("Pizzas", tenant.getId()));

    for (int i = 1; i <= quantity; i++) {
      Product product =
          new Product(
              tenant.getId(), category.getId(), "Product %d".formatted(i), null, new BigDecimal(i));
      if (!active) {
        product.deactivate();
      }
      products.add(productRepository.save(product));
    }
    return products;
  }

  private Tenant createTenant(String name) {
    return tenantRepository.save(new Tenant(name, Plan.MVP));
  }

  private Tenant createTenant() {
    return createTenant("Pizarria da mama");
  }
}
