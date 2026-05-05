package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductRepositoryAdapter;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({CatalogService.class, JpaProductRepositoryAdapter.class})
@EnableJpaAuditing
class CatalogServiceIntegrationTest {

  private static final String VALID_NAME = "Pizza Portuguesa";
  private static final BigDecimal VALID_PRICE = new BigDecimal("35.40");

  @Autowired private CatalogService catalogService;
  @Autowired private ProductRepository productRepository;
  @Autowired private SpringDataTenantRepository tenantRepository;

  @Test
  void shouldCreateProduct() {
    var tenant = createTenant();

    var product = createProduct(tenant);

    assertThat(product)
        .satisfies(
            prod -> {
              assertThat(prod.getId()).isNotNull();
              assertThat(prod.isActive()).isTrue();
              assertThat(prod.getTenantId()).isEqualTo(tenant.getId());
              assertThat(prod.getName()).isEqualTo(VALID_NAME);
              assertThat(prod.getPrice()).isEqualByComparingTo(VALID_PRICE);
            });
  }

  @Test
  void shouldFindProductByIdAndTenantId() {
    var tenant = createTenant();
    var product = createProduct(tenant);

    var foundProduct = catalogService.findProduct(tenant.getId(), product.getId());

    assertThat(foundProduct)
        .isPresent()
        .get()
        .satisfies(
            found -> {
              assertThat(found.getId()).isEqualTo(product.getId());
              assertThat(found.getTenantId()).isEqualTo(tenant.getId());
              assertThat(found.getName()).isEqualTo(VALID_NAME);
              assertThat(found.getPrice()).isEqualByComparingTo(VALID_PRICE);
            });
  }

  @Test
  void shouldReturnEmptyWhenProductDoesNotExist() {
    var tenant = createTenant();

    var foundProduct = catalogService.findProduct(tenant.getId(), UUID.randomUUID());

    assertThat(foundProduct).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenProductBelongsToAnotherTenant() {
    var ownerTenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var product = createProduct(ownerTenant);

    var foundProduct = catalogService.findProduct(anotherTenant.getId(), product.getId());

    assertThat(foundProduct).isEmpty();
  }

  @Test
  void shouldListOnlyActiveProductsByTenant() {
    var tenant = createTenant();
    var activeProduct = createProduct(tenant);
    createInactiveProduct(tenant);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products).extracting(Product::getId).containsExactly(activeProduct.getId());
  }

  @Test
  void shouldNotListInactiveProducts() {
    var tenant = createTenant();
    createInactiveProduct(tenant);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products).isEmpty();
  }

  @Test
  void shouldNotListProductsFromAnotherTenant() {
    var tenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var productFromTenant = createProduct(tenant);
    createProduct(anotherTenant, "Pizza Margherita", new BigDecimal("39.90"));

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products).extracting(Product::getId).containsExactly(productFromTenant.getId());
  }

  private Tenant createTenant() {
    return createTenant("Don Corleone Pizzeria");
  }

  private Tenant createTenant(String name) {
    return tenantRepository.save(new Tenant(name, Plan.MVP));
  }

  private Product createProduct(Tenant tenant) {
    return createProduct(tenant, VALID_NAME, VALID_PRICE);
  }

  private Product createProduct(Tenant tenant, String name, BigDecimal price) {
    return catalogService.createProduct(tenant.getId(), name, price);
  }

  private void createInactiveProduct(Tenant tenant) {
    var product = createProduct(tenant, "Pizza Calabresa", new BigDecimal("42.00"));
    product.deactivate();
    productRepository.save(product);
  }
}
