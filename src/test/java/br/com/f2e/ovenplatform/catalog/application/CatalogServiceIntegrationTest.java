package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.catalog.domain.Category;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaCategoryRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductRepositoryAdapter;
import br.com.f2e.ovenplatform.media.application.api.AvailableImage;
import br.com.f2e.ovenplatform.media.application.api.AvailableImageLookup;
import br.com.f2e.ovenplatform.media.domain.StoredImage;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({
  CatalogService.class,
  JpaProductRepositoryAdapter.class,
  JpaCategoryRepositoryAdapter.class
})
class CatalogServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final String VALID_NAME = "Pizza Portuguesa";
  private static final String VALID_DESCRIPTION = "Pizza com queijo, presunto e ovos";
  private static final BigDecimal VALID_PRICE = new BigDecimal("35.40");
  private static final URI IMAGE_URL = URI.create("https://images.example/products/image.webp");

  @Autowired private CatalogService catalogService;
  @Autowired private ProductRepository productRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private SpringDataTenantRepository tenantRepository;
  @MockitoBean private AvailableImageLookup availableImageLookup;

  @Test
  void shouldCreateProduct() {
    var tenant = createTenant();
    var category = createCategory(tenant);

    var product = createProduct(tenant, category);

    assertThat(product)
        .satisfies(
            prod -> {
              assertThat(prod.id()).isNotNull();
              assertThat(prod.active()).isTrue();
              assertThat(prod.tenantId()).isEqualTo(tenant.getId());
              assertThat(prod.categoryId()).isEqualTo(category.getId());
              assertThat(prod.imageId()).isNotNull();
              assertThat(prod.imageUrl()).isEqualTo(IMAGE_URL);
              assertThat(prod.name()).isEqualTo(VALID_NAME);
              assertThat(prod.description()).isEqualTo(VALID_DESCRIPTION);
              assertThat(prod.price()).isEqualByComparingTo(VALID_PRICE);
            });
  }

  @Test
  void shouldRejectProductWhenCategoryBelongsToAnotherTenant() {
    var tenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var categoryFromAnotherTenant = createCategory(anotherTenant);
    var tenantId = tenant.getId();
    var categoryId = categoryFromAnotherTenant.getId();
    var command =
        new CreateProductCommand(
            categoryId, UUID.randomUUID(), VALID_NAME, VALID_DESCRIPTION, VALID_PRICE);

    assertThatThrownBy(() -> catalogService.createProduct(tenantId, command))
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
    var command =
        new CreateProductCommand(
            categoryId, UUID.randomUUID(), VALID_NAME, VALID_DESCRIPTION, VALID_PRICE);

    assertThatThrownBy(() -> catalogService.createProduct(tenantId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Category id: %s not found".formatted(categoryId));
  }

  @Test
  void shouldFindProductByIdAndTenantId() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var product = createProduct(tenant, category);

    var foundProduct = catalogService.findProduct(tenant.getId(), product.id());

    assertThat(foundProduct)
        .isPresent()
        .get()
        .satisfies(
            found -> {
              assertThat(found.id()).isEqualTo(product.id());
              assertThat(found.tenantId()).isEqualTo(tenant.getId());
              assertThat(found.categoryId()).isEqualTo(category.getId());
              assertThat(found.imageId()).isEqualTo(product.imageId());
              assertThat(found.imageUrl()).isEqualTo(IMAGE_URL);
              assertThat(found.name()).isEqualTo(VALID_NAME);
              assertThat(found.description()).isEqualTo(VALID_DESCRIPTION);
              assertThat(found.price()).isEqualByComparingTo(VALID_PRICE);
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

    var foundProduct = catalogService.findProduct(anotherTenant.getId(), product.id());

    assertThat(foundProduct).isEmpty();
  }

  @Test
  void shouldListOnlyActiveProductsByTenant() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var activeProduct = createProduct(tenant, category);
    createInactiveProduct(tenant);
    stubAvailableImages(tenant.getId(), activeProduct);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products).extracting(ProductResult::id).containsExactly(activeProduct.id());
    verify(availableImageLookup)
        .getAvailableImages(tenant.getId(), Set.of(activeProduct.imageId()));
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
    stubAvailableImages(tenant.getId(), productFromTenant);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products).extracting(ProductResult::id).containsExactly(productFromTenant.id());
  }

  @Test
  void shouldListProductsWithTheirPublicImagesUsingBatchLookup() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var firstProduct = createProduct(tenant, category);
    var secondProduct =
        createProduct(tenant, category, "Pizza Margherita", new BigDecimal("39.90"));
    stubAvailableImages(tenant.getId(), firstProduct, secondProduct);
    clearInvocations(availableImageLookup);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products)
        .extracting(ProductResult::id, ProductResult::imageId, ProductResult::imageUrl)
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple(
                firstProduct.id(), firstProduct.imageId(), IMAGE_URL),
            org.assertj.core.groups.Tuple.tuple(
                secondProduct.id(), secondProduct.imageId(), IMAGE_URL));
    verify(availableImageLookup)
        .getAvailableImages(
            tenant.getId(), Set.of(firstProduct.imageId(), secondProduct.imageId()));
    verify(availableImageLookup, never()).getAvailableImage(any(), any());
  }

  @Test
  void shouldResolveSharedProductImageOnlyOnceWhenListingProducts() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var sharedImageId = createAvailableImage(tenant.getId());
    var firstProduct =
        createProduct(tenant, category, sharedImageId, "Pizza Portuguesa", new BigDecimal("35.40"));
    var secondProduct =
        createProduct(tenant, category, sharedImageId, "Pizza Margherita", new BigDecimal("39.90"));
    stubAvailableImages(tenant.getId(), firstProduct);
    clearInvocations(availableImageLookup);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products)
        .extracting(ProductResult::id)
        .containsExactly(firstProduct.id(), secondProduct.id());
    assertThat(products).extracting(ProductResult::imageId).containsOnly(sharedImageId);
    verify(availableImageLookup).getAvailableImages(tenant.getId(), Set.of(sharedImageId));
    verify(availableImageLookup, never()).getAvailableImage(any(), any());
  }

  @Test
  void shouldGetProductByIdAndTenantId() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var product = createProduct(tenant, category);

    var foundProduct = catalogService.getProduct(tenant.getId(), product.id());

    assertThat(foundProduct.id()).isEqualTo(product.id());
    assertThat(foundProduct.imageUrl()).isEqualTo(IMAGE_URL);
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
    var newImageId = createAvailableImage(tenant.getId());
    var command =
        new UpdateProductCommand(
            newCategory.getId(),
            newImageId,
            "Coca-cola lata",
            "Refrigerante gelado",
            new BigDecimal("8.00"),
            false);

    var updatedProduct = catalogService.update(tenant.getId(), product.id(), command);

    assertThat(updatedProduct.categoryId()).isEqualTo(newCategory.getId());
    assertThat(updatedProduct.imageId()).isEqualTo(newImageId);
    assertThat(updatedProduct.imageUrl()).isEqualTo(IMAGE_URL);
    assertThat(updatedProduct.name()).isEqualTo("Coca-cola lata");
    assertThat(updatedProduct.description()).isEqualTo("Refrigerante gelado");
    assertThat(updatedProduct.price()).isEqualByComparingTo("8.00");
    assertThat(updatedProduct.active()).isFalse();
  }

  @Test
  void shouldRejectProductUpdateWhenProductBelongsToAnotherTenant() {
    var ownerTenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var ownerCategory = createCategory(ownerTenant);
    var anotherCategory = createCategory(anotherTenant);
    var product = createProduct(ownerTenant, ownerCategory);
    var tenantId = anotherTenant.getId();
    var productId = product.id();
    var categoryId = anotherCategory.getId();
    var price = new BigDecimal("8.00");
    var command =
        new UpdateProductCommand(
            categoryId,
            createAvailableImage(anotherTenant.getId()),
            "Coca-cola lata",
            "Refrigerante gelado",
            price,
            true);

    assertThatThrownBy(() -> catalogService.update(tenantId, productId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  @Test
  void shouldDeactivateProduct() {
    var tenant = createTenant();
    var category = createCategory(tenant);
    var product = createProduct(tenant, category);

    catalogService.deactivate(tenant.getId(), product.id());

    assertThat(productRepository.findByIdAndTenantId(product.id(), tenant.getId()))
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

  private ProductResult createProduct(Tenant tenant, Category category) {
    return createProduct(tenant, category, VALID_NAME, VALID_PRICE);
  }

  private ProductResult createProduct(
      Tenant tenant, Category category, String name, BigDecimal price) {
    return createProduct(tenant, category, createAvailableImage(tenant.getId()), name, price);
  }

  private ProductResult createProduct(
      Tenant tenant, Category category, UUID imageId, String name, BigDecimal price) {
    var command =
        new CreateProductCommand(category.getId(), imageId, name, VALID_DESCRIPTION, price);
    return catalogService.createProduct(tenant.getId(), command);
  }

  private void stubAvailableImages(UUID tenantId, ProductResult... products) {
    var imageIds =
        Arrays.stream(products)
            .map(ProductResult::imageId)
            .collect(java.util.stream.Collectors.toSet());
    var images = imageIds.stream().map(imageId -> new AvailableImage(imageId, IMAGE_URL)).toList();
    when(availableImageLookup.getAvailableImages(tenantId, imageIds)).thenReturn(images);
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
    when(availableImageLookup.getAvailableImage(tenantId, image.getId()))
        .thenReturn(new AvailableImage(image.getId(), IMAGE_URL));
    return image.getId();
  }

  private void createInactiveProduct(Tenant tenant) {
    var category = createCategory(tenant, "Calzones");
    var result = createProduct(tenant, category, "Pizza Calabresa", new BigDecimal("42.00"));
    var product = productRepository.findByIdAndTenantId(result.id(), tenant.getId()).orElseThrow();
    product.deactivate();
    productRepository.save(product);
  }
}
