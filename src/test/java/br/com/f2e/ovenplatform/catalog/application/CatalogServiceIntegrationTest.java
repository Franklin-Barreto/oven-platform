package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.catalog.domain.Category;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaCategoryRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({
  CatalogService.class,
  JpaProductRepositoryAdapter.class,
  JpaCategoryRepositoryAdapter.class
})
class CatalogServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final String VALID_NAME = "Pizza Portuguesa";
  private static final String VALID_DESCRIPTION = "Pizza com queijo, presunto e ovos";
  private static final BigDecimal VALID_PRICE = new BigDecimal("35.40");

  @Autowired private CatalogService catalogService;
  @Autowired private ProductRepository productRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private SpringDataTenantRepository tenantRepository;

  @Test
  void shouldCreateProduct() {
    var tenant = createTenant();
    var category = createCategory(tenant);

    var product = createProduct(tenant, category);

    assertThat(product)
        .satisfies(
            prod -> {
              assertThat(prod.getId()).isNotNull();
              assertThat(prod.isActive()).isTrue();
              assertThat(prod.getTenantId()).isEqualTo(tenant.getId());
              assertThat(prod.getCategoryId()).isEqualTo(category.getId());
              assertThat(prod.getName()).isEqualTo(VALID_NAME);
              assertThat(prod.getDescription()).isEqualTo(VALID_DESCRIPTION);
              assertThat(prod.getPrice()).isEqualByComparingTo(VALID_PRICE);
            });
  }

  @Test
  void shouldRejectProductWhenCategoryBelongsToAnotherTenant() {
    var tenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var categoryFromAnotherTenant = createCategory(anotherTenant);
    var tenantId = tenant.getId();
    var categoryId = categoryFromAnotherTenant.getId();

    assertThatThrownBy(
            () ->
                catalogService.createProduct(
                    tenantId, categoryId, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Category id: %s not found".formatted(categoryId));
  }

  @Test
  void shouldRejectProductWhenCategoryIsInactive() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    category.deactivate();
    categoryRepository.save(category);
    var tenantId = tenant.getId();
    var categoryId = category.getId();

    assertThatThrownBy(
            () ->
                catalogService.createProduct(
                    tenantId, categoryId, VALID_NAME, VALID_DESCRIPTION, VALID_PRICE))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Category id: %s not found".formatted(categoryId));
  }

  @Test
  void shouldFindProductByIdAndTenantId() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var product = createProduct(tenant, category);

    var foundProduct = catalogService.findProduct(tenant.getId(), product.getId());

    assertThat(foundProduct)
        .isPresent()
        .get()
        .satisfies(
            found -> {
              assertThat(found.getId()).isEqualTo(product.getId());
              assertThat(found.getTenantId()).isEqualTo(tenant.getId());
              assertThat(found.getCategoryId()).isEqualTo(category.getId());
              assertThat(found.getName()).isEqualTo(VALID_NAME);
              assertThat(found.getDescription()).isEqualTo(VALID_DESCRIPTION);
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
    var category = createCategory(ownerTenant);
    var product = createProduct(ownerTenant, category);

    var foundProduct = catalogService.findProduct(anotherTenant.getId(), product.getId());

    assertThat(foundProduct).isEmpty();
  }

  @Test
  void shouldListOnlyActiveProductsByTenant() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var activeProduct = createProduct(tenant, category);
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
    var category = createCategory(tenant);
    var anotherCategory = createCategory(anotherTenant);
    var productFromTenant = createProduct(tenant, category);
    createProduct(anotherTenant, anotherCategory, "Pizza Margherita", new BigDecimal("39.90"));

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products).extracting(Product::getId).containsExactly(productFromTenant.getId());
  }

  @Test
  void shouldGetProductByIdAndTenantId() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var product = createProduct(tenant, category);

    var foundProduct = catalogService.getProduct(tenant.getId(), product.getId());

    assertThat(foundProduct.getId()).isEqualTo(product.getId());
  }

  @Test
  void shouldThrowWhenGettingUnknownProduct() {
    var tenant = createTenant();
    var tenantId = tenant.getId();
    var productId = UUID.randomUUID();

    assertThatThrownBy(() -> catalogService.getProduct(tenantId, productId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  @Test
  void shouldUpdateProduct() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var newCategory = createCategory(tenant, "Bebidas");
    var product = createProduct(tenant, category);

    var updatedProduct =
        catalogService.update(
            tenant.getId(),
            product.getId(),
            newCategory.getId(),
            "Coca-cola lata",
            "Refrigerante gelado",
            new BigDecimal("8.00"),
            false);

    assertThat(updatedProduct.getCategoryId()).isEqualTo(newCategory.getId());
    assertThat(updatedProduct.getName()).isEqualTo("Coca-cola lata");
    assertThat(updatedProduct.getDescription()).isEqualTo("Refrigerante gelado");
    assertThat(updatedProduct.getPrice()).isEqualByComparingTo("8.00");
    assertThat(updatedProduct.isActive()).isFalse();
  }

  @Test
  void shouldRejectProductUpdateWhenProductBelongsToAnotherTenant() {
    var ownerTenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var ownerCategory = createCategory(ownerTenant);
    var anotherCategory = createCategory(anotherTenant);
    var product = createProduct(ownerTenant, ownerCategory);
    var tenantId = anotherTenant.getId();
    var productId = product.getId();
    var categoryId = anotherCategory.getId();
    var price = new BigDecimal("8.00");

    assertThatThrownBy(
            () ->
                catalogService.update(
                    tenantId,
                    productId,
                    categoryId,
                    "Coca-cola lata",
                    "Refrigerante gelado",
                    price,
                    true))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  @Test
  void shouldDeactivateProduct() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var product = createProduct(tenant, category);

    catalogService.deactivate(tenant.getId(), product.getId());

    assertThat(productRepository.findByIdAndTenantId(product.getId(), tenant.getId()))
        .isPresent()
        .get()
        .extracting(Product::isActive)
        .isEqualTo(false);
  }

  private Tenant createTenant() {
    return createTenant("Don Corleone Pizzeria");
  }

  private Tenant createTenant(String name) {
    return tenantRepository.save(new Tenant(name, Plan.MVP));
  }

  private Category createCategory(Tenant tenant) {
    return createCategory(tenant, "Pizzas");
  }

  private Category createCategory(Tenant tenant, String name) {
    return categoryRepository.save(new Category(name, tenant.getId()));
  }

  private Product createProduct(Tenant tenant, Category category) {
    return createProduct(tenant, category, VALID_NAME, VALID_PRICE);
  }

  private Product createProduct(Tenant tenant, Category category, String name, BigDecimal price) {
    return catalogService.createProduct(
        tenant.getId(), category.getId(), name, VALID_DESCRIPTION, price);
  }

  private void createInactiveProduct(Tenant tenant) {
    var category = createCategory(tenant, "Calzones");
    var product = createProduct(tenant, category, "Pizza Calabresa", new BigDecimal("42.00"));
    product.deactivate();
    productRepository.save(product);
  }
}
