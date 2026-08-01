package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.catalog.application.api.CatalogProductLookup;
import br.com.f2e.ovenplatform.catalog.application.api.ProductSelection;
import br.com.f2e.ovenplatform.catalog.application.api.SellableProduct;
import br.com.f2e.ovenplatform.catalog.application.product.CatalogProductLookupService;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantRepository;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaCategoryRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductVariantRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.support.CatalogTestFixture;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  CatalogProductLookupService.class,
  JpaProductRepositoryAdapter.class,
  JpaCategoryRepositoryAdapter.class,
  JpaProductVariantRepositoryAdapter.class
})
class CatalogProductLookupIntegrationTest extends DataJpaIntegrationTest {

  @Autowired private CatalogProductLookup catalogProductLookup;
  @Autowired private ProductVariantRepository variantRepository;

  private CatalogTestFixture fixture;

  @BeforeEach
  void setUp() {
    fixture = new CatalogTestFixture(entityManager);
  }

  @Test
  void shouldResolveSimpleProductWithCurrentBasePrice() {
    var productFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var product = productFixture.product();
    var selection = new ProductSelection(product.getId(), null);

    clearPersistenceContext();

    var result = find(product.getTenantId(), selection);

    assertThat(result).hasSize(1);
    assertSimpleProduct(result.getFirst(), product);
  }

  @Test
  void shouldResolveRequestedVariantsOfSameProductIndependently() {
    var productFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var product = productFixture.product();
    var medium = createVariant(product, "Pizza Calabresa Media", "38.50", true);
    var large = createVariant(product, "Pizza Calabresa Grande", "47.90", true);
    createVariant(product, "Pizza Calabresa Família", "59.90", true);
    var mediumSelection = selectionOf(product, medium);
    var largeSelection = selectionOf(product, large);

    clearPersistenceContext();

    var actualBySelection =
        bySelection(find(product.getTenantId(), mediumSelection, largeSelection));

    assertThat(actualBySelection.keySet())
        .containsExactlyInAnyOrder(mediumSelection, largeSelection);
    assertVariant(actualBySelection.get(mediumSelection), product, medium);
    assertVariant(actualBySelection.get(largeSelection), product, large);
  }

  @Test
  void shouldResolveSimpleAndVariantProductsInSameLookup() {
    var productFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var tenant = productFixture.tenant();
    var simpleProduct = productFixture.product();
    var variantProduct =
        fixture.createProduct(
            tenant, productFixture.category(), productFixture.image(), "Pizza Portuguesa");
    var large = createVariant(variantProduct, "Pizza Portuguesa Grande", "49.90", true);
    var simpleSelection = new ProductSelection(simpleProduct.getId(), null);
    var variantSelection = selectionOf(variantProduct, large);

    clearPersistenceContext();

    var actualBySelection = bySelection(find(tenant.getId(), simpleSelection, variantSelection));

    assertThat(actualBySelection.keySet())
        .containsExactlyInAnyOrder(simpleSelection, variantSelection);
    assertSimpleProduct(actualBySelection.get(simpleSelection), simpleProduct);
    assertVariant(actualBySelection.get(variantSelection), variantProduct, large);
  }

  @Test
  void shouldNotResolveProductConfiguredWithVariantsWithoutVariantSelection() {
    var productFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var product = productFixture.product();
    createVariant(product, "Pizza Calabresa Grande", "47.90", true);

    clearPersistenceContext();

    var result = find(product.getTenantId(), new ProductSelection(product.getId(), null));

    assertThat(result).isEmpty();
  }

  @Test
  void shouldNotResolveProductConfiguredOnlyWithInactiveVariantsAsSimpleProduct() {
    var productFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var product = productFixture.product();
    createVariant(product, "Pizza Calabresa Grande", "47.90", false);

    clearPersistenceContext();

    var result = find(product.getTenantId(), new ProductSelection(product.getId(), null));

    assertThat(result).isEmpty();
  }

  @Test
  void shouldNotResolveInactiveProductEvenWhenSelectedVariantIsActive() {
    var productFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var product = productFixture.product();
    var variant = createVariant(product, "Pizza Calabresa Grande", "47.90", true);
    product.deactivate();

    clearPersistenceContext();

    var result = find(product.getTenantId(), selectionOf(product, variant));

    assertThat(result).isEmpty();
  }

  @Test
  void shouldNotResolveInactiveVariant() {
    var productFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var product = productFixture.product();
    var variant = createVariant(product, "Pizza Calabresa Grande", "47.90", false);

    clearPersistenceContext();

    var result = find(product.getTenantId(), selectionOf(product, variant));

    assertThat(result).isEmpty();
  }

  @Test
  void shouldNotResolveVariantOwnedByAnotherProduct() {
    var productFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var tenant = productFixture.tenant();
    var calabresa = productFixture.product();
    var portuguesa =
        fixture.createProduct(
            tenant, productFixture.category(), productFixture.image(), "Pizza Portuguesa");
    var portuguesaLarge = createVariant(portuguesa, "Pizza Portuguesa Grande", "49.90", true);
    var invalidSelection = new ProductSelection(calabresa.getId(), portuguesaLarge.getId());

    clearPersistenceContext();

    var result = find(tenant.getId(), invalidSelection);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldNotResolveProductOrVariantFromAnotherTenant() {
    var currentTenantFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var currentProduct = currentTenantFixture.product();
    var currentVariant = createVariant(currentProduct, "Pizza Calabresa Grande", "47.90", true);
    var anotherTenantFixture = fixture.createProductFixture("Sicilia Pizzeria");
    var foreignProduct = anotherTenantFixture.product();
    var foreignVariant = createVariant(foreignProduct, "Pizza Margherita Grande", "45.90", true);
    var currentSelection = selectionOf(currentProduct, currentVariant);
    var foreignSelection = selectionOf(foreignProduct, foreignVariant);

    clearPersistenceContext();

    var result = find(currentTenantFixture.tenant().getId(), currentSelection, foreignSelection);

    assertThat(result).hasSize(1);
    assertVariant(result.getFirst(), currentProduct, currentVariant);
  }

  @Test
  void shouldNotResolveNonexistentVariantForExistingProduct() {
    var productFixture = fixture.createProductFixture("Don Corleone Pizzeria");
    var product = productFixture.product();
    createVariant(product, "Pizza Calabresa Grande", "47.90", true);

    clearPersistenceContext();

    var result =
        find(product.getTenantId(), new ProductSelection(product.getId(), UUID.randomUUID()));

    assertThat(result).isEmpty();
  }

  @Test
  void shouldNotResolveNonexistentProduct() {
    var tenant = fixture.createTenant("Don Corleone Pizzeria");

    clearPersistenceContext();

    var result = find(tenant.getId(), new ProductSelection(UUID.randomUUID(), null));

    assertThat(result).isEmpty();
  }

  private List<SellableProduct> find(UUID tenantId, ProductSelection... selections) {
    return catalogProductLookup.findSellableProducts(tenantId, List.of(selections));
  }

  private Map<ProductSelection, SellableProduct> bySelection(
      List<SellableProduct> sellableProducts) {
    return sellableProducts.stream()
        .collect(
            Collectors.toMap(
                product -> new ProductSelection(product.productId(), product.variantId()),
                Function.identity()));
  }

  private ProductVariant createVariant(Product product, String name, String price, boolean active) {
    var variant =
        new ProductVariant(
            product.getId(), product.getTenantId(), null, name, new BigDecimal(price), 0);
    variant.changeStatusTo(active);
    return variantRepository.save(variant);
  }

  private ProductSelection selectionOf(Product product, ProductVariant variant) {
    return new ProductSelection(product.getId(), variant.getId());
  }

  private void assertSimpleProduct(SellableProduct actual, Product expected) {
    assertThat(actual.productId()).isEqualTo(expected.getId());
    assertThat(actual.productName()).isEqualTo(expected.getName());
    assertThat(actual.variantId()).isNull();
    assertThat(actual.variantName()).isNull();
    assertThat(actual.price()).isEqualByComparingTo(expected.getPrice());
  }

  private void assertVariant(
      SellableProduct actual, Product expectedProduct, ProductVariant expectedVariant) {
    assertThat(actual.productId()).isEqualTo(expectedProduct.getId());
    assertThat(actual.productName()).isEqualTo(expectedProduct.getName());
    assertThat(actual.variantId()).isEqualTo(expectedVariant.getId());
    assertThat(actual.variantName()).isEqualTo(expectedVariant.getName());
    assertThat(actual.price()).isEqualByComparingTo(expectedVariant.getPrice());
  }

  private void clearPersistenceContext() {
    entityManager.flush();
    entityManager.clear();
  }
}
