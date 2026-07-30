package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.catalog.application.api.CatalogProductLookup;
import br.com.f2e.ovenplatform.catalog.application.api.SellableProduct;
import br.com.f2e.ovenplatform.catalog.application.category.CategoryRepository;
import br.com.f2e.ovenplatform.catalog.application.product.CatalogProductLookupService;
import br.com.f2e.ovenplatform.catalog.application.product.ProductRepository;
import br.com.f2e.ovenplatform.catalog.domain.Category;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaCategoryRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductRepositoryAdapter;
import br.com.f2e.ovenplatform.media.domain.StoredImage;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
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
import org.springframework.context.annotation.Import;

@Import({
  CatalogProductLookupService.class,
  JpaProductRepositoryAdapter.class,
  JpaCategoryRepositoryAdapter.class
})
class CatalogProductLookupIntegrationTest extends DataJpaIntegrationTest {

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

    var expectedProductsById =
        products.stream().collect(Collectors.toMap(Product::getId, product -> product));
    var sellableProducts =
        catalogProductLookup.findSellableProducts(tenant.getId(), expectedProductsById.keySet());

    var actualProductsById =
        sellableProducts.stream()
            .collect(Collectors.toMap(SellableProduct::productId, product -> product));

    assertThat(actualProductsById.keySet())
        .containsExactlyInAnyOrderElementsOf(expectedProductsById.keySet());

    expectedProductsById.forEach(
        (productId, expectedProduct) -> {
          var actualProduct = actualProductsById.get(productId);

          assertThat(actualProduct.productName()).isEqualTo(expectedProduct.getName());
          assertThat(actualProduct.price()).isEqualByComparingTo(expectedProduct.getPrice());
        });
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
    var imageId = createAvailableImage(tenant.getId());

    for (int i = 1; i <= quantity; i++) {
      Product product =
          new Product(
              tenant.getId(),
              category.getId(),
              imageId,
              "Product %d".formatted(i),
              null,
              new BigDecimal(i));
      if (!active) {
        product.deactivate();
      }
      products.add(productRepository.save(product));
    }
    return products;
  }

  private UUID createAvailableImage(UUID tenantId) {
    var checksum = "0t/CUcGnJF1Ot9leX4FUcsbbz37maQu9fBkS9He2wio=";
    var image =
        StoredImage.pending(
            tenantId,
            "tenants/%s/images/%s.webp".formatted(tenantId, UUID.randomUUID()),
            "image/webp",
            1_024L,
            checksum);
    image.confirm("image/webp", 1_024L, checksum);
    entityManager.persist(image);
    return image.getId();
  }

  private Tenant createTenant(String name) {
    return tenantRepository.save(new Tenant(name, Plan.MVP));
  }

  private Tenant createTenant() {
    return createTenant("Pizarria da mama");
  }
}
