package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.catalog.application.category.CategoryRepository;
import br.com.f2e.ovenplatform.catalog.application.product.CatalogService;
import br.com.f2e.ovenplatform.catalog.application.product.CreateProductCommand;
import br.com.f2e.ovenplatform.catalog.application.product.ProductRepository;
import br.com.f2e.ovenplatform.catalog.application.product.ProductResult;
import br.com.f2e.ovenplatform.catalog.application.product.ProductSummaryResult;
import br.com.f2e.ovenplatform.catalog.application.product.ProductVariantDetailResult;
import br.com.f2e.ovenplatform.catalog.application.product.UpdateProductCommand;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantRepository;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResultResolver;
import br.com.f2e.ovenplatform.catalog.domain.Category;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaCategoryRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductVariantRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.support.CatalogTestFixture;
import br.com.f2e.ovenplatform.media.application.api.AvailableImage;
import br.com.f2e.ovenplatform.media.application.api.AvailableImageLookup;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({
  CatalogService.class,
  ProductVariantResultResolver.class,
  JpaProductRepositoryAdapter.class,
  JpaCategoryRepositoryAdapter.class,
  JpaProductVariantRepositoryAdapter.class
})
class CatalogServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final String VALID_NAME = "Pizza Portuguesa";
  private static final String VALID_DESCRIPTION = "Pizza com queijo, presunto e ovos";
  private static final BigDecimal VALID_PRICE = new BigDecimal("35.40");
  private static final URI IMAGE_URL = URI.create("https://images.example/products/image.webp");
  private static final URI VARIANT_IMAGE_URL =
      URI.create("https://images.example/products/variant.webp");
  private static final String CATEGORY_NAME = "Pizza";
  private static final String LARGE_VARIANT_NAME = "Grande";
  private static final String CORLEONE_PIZZERIA = "Don Corleone Pizzeria";
  private static final String SOPRANO_PIZZERIA = "Soprano Pizzeria";
  private static final String PIZZA_MARGHERITA = "Pizza Margherita";

  @Autowired private CatalogService catalogService;
  @Autowired private ProductRepository productRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ProductVariantRepository variantRepository;

  @MockitoBean private AvailableImageLookup availableImageLookup;

  private CatalogTestFixture fixture;

  @BeforeEach
  void setUp() {
    fixture = new CatalogTestFixture(entityManager);
  }

  @Test
  void shouldCreateProduct() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);

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
    var tenant = fixture.createTenant(CORLEONE_PIZZERIA);
    var anotherTenant = fixture.createTenant(SOPRANO_PIZZERIA);
    var categoryFromAnotherTenant = fixture.createCategory(anotherTenant, CATEGORY_NAME);
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
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
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
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
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
    var ownerTenant = fixture.createTenant(CORLEONE_PIZZERIA);
    var anotherTenant = fixture.createTenant(SOPRANO_PIZZERIA);
    var category = fixture.createCategory(ownerTenant, CATEGORY_NAME);
    var product = createProduct(ownerTenant, category);

    var foundProduct = catalogService.findProduct(anotherTenant.getId(), product.id());

    assertThat(foundProduct).isEmpty();
  }

  @Test
  void shouldListOnlyActiveProductsByTenant() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var activeProduct = createProduct(tenant, category);
    createInactiveProduct(tenant);
    stubAvailableImages(tenant.getId(), activeProduct);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products).extracting(ProductSummaryResult::id).containsExactly(activeProduct.id());
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
    var tenant = fixture.createTenant(CORLEONE_PIZZERIA);
    var anotherTenant = fixture.createTenant(SOPRANO_PIZZERIA);
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var anotherCategory = fixture.createCategory(anotherTenant, CATEGORY_NAME);
    var productFromTenant = createProduct(tenant, category);
    createProduct(anotherTenant, anotherCategory, PIZZA_MARGHERITA, new BigDecimal("39.90"));
    stubAvailableImages(tenant.getId(), productFromTenant);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products)
        .extracting(ProductSummaryResult::id)
        .containsExactly(productFromTenant.id());
  }

  @Test
  void shouldListProductsWithTheirPublicImagesUsingBatchLookup() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var firstProduct = createProduct(tenant, category);
    var secondProduct = createProduct(tenant, category, PIZZA_MARGHERITA, new BigDecimal("39.90"));
    stubAvailableImages(tenant.getId(), firstProduct, secondProduct);
    clearInvocations(availableImageLookup);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products)
        .extracting(
            ProductSummaryResult::id, ProductSummaryResult::imageId, ProductSummaryResult::imageUrl)
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
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var sharedImageId = createAvailableImage(tenant.getId());
    var firstProduct =
        createProduct(tenant, category, sharedImageId, "Pizza Portuguesa", new BigDecimal("35.40"));
    var secondProduct =
        createProduct(tenant, category, sharedImageId, PIZZA_MARGHERITA, new BigDecimal("39.90"));
    stubAvailableImages(tenant.getId(), firstProduct);
    clearInvocations(availableImageLookup);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products)
        .extracting(ProductSummaryResult::id)
        .containsExactlyInAnyOrder(firstProduct.id(), secondProduct.id());
    assertThat(products).extracting(ProductSummaryResult::imageId).containsOnly(sharedImageId);
    verify(availableImageLookup).getAvailableImages(tenant.getId(), Set.of(sharedImageId));
    verify(availableImageLookup, never()).getAvailableImage(any(), any());
  }

  @Test
  void shouldGetProductByIdAndTenantId() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var product = createProduct(tenant, category);

    var foundProduct = catalogService.getProduct(tenant.getId(), product.id());

    assertThat(foundProduct.product().id()).isEqualTo(product.id());
    assertThat(foundProduct.product().imageUrl()).isEqualTo(IMAGE_URL);
    assertThat(foundProduct.product().displayPrice()).isEqualByComparingTo(product.price());
    assertThat(foundProduct.product().hasVariants()).isFalse();
    assertThat(foundProduct.product().available()).isTrue();
    assertThat(foundProduct.variants()).isEmpty();
  }

  @Test
  void shouldListOnlyActiveVariantsInDisplayOrderUsingProductImageAsFallback() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var product = createProduct(tenant, category);
    createVariant(tenant, product, "Grande", new BigDecimal("45.00"), 2, true);
    createVariant(tenant, product, "Pequena", new BigDecimal("39.00"), 0, true);
    createVariant(tenant, product, "Média", new BigDecimal("42.00"), 1, false);

    var detail = catalogService.getProduct(tenant.getId(), product.id());

    assertThat(detail.product().displayPrice()).isEqualByComparingTo("39.00");
    assertThat(detail.product().hasVariants()).isTrue();
    assertThat(detail.product().available()).isTrue();
    assertThat(detail.variants())
        .extracting(
            ProductVariantDetailResult::name,
            ProductVariantDetailResult::displayPosition,
            ProductVariantDetailResult::imageId,
            ProductVariantDetailResult::imageUrl)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Pequena", 0, product.imageId(), IMAGE_URL),
            org.assertj.core.groups.Tuple.tuple("Grande", 2, product.imageId(), IMAGE_URL));
  }

  @Test
  void shouldUseVariantImageWhenPresent() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var product = createProduct(tenant, category);
    var variantImageId = createAvailableImage(tenant.getId());
    var variant =
        createVariant(
            tenant, product, variantImageId, LARGE_VARIANT_NAME, new BigDecimal("45.00"), 0, true);
    when(availableImageLookup.getAvailableImages(tenant.getId(), Set.of(variantImageId)))
        .thenReturn(List.of(new AvailableImage(variantImageId, VARIANT_IMAGE_URL)));

    var detail = catalogService.getProduct(tenant.getId(), product.id());

    assertThat(detail.variants())
        .singleElement()
        .satisfies(
            result -> {
              assertThat(result.id()).isEqualTo(variant.getId());
              assertThat(result.imageId()).isEqualTo(variantImageId);
              assertThat(result.imageUrl()).isEqualTo(VARIANT_IMAGE_URL);
            });
  }

  @Test
  void shouldNotResolveImageFromInactiveVariantWhenGettingProductDetail() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var product = createProduct(tenant, category);
    var inactiveVariantImageId = createAvailableImage(tenant.getId());
    createVariant(tenant, product, null, "Pequena", new BigDecimal("39.00"), 0, true);
    createVariant(
        tenant,
        product,
        inactiveVariantImageId,
        LARGE_VARIANT_NAME,
        new BigDecimal("45.00"),
        1,
        false);

    var detail = catalogService.getProduct(tenant.getId(), product.id());

    assertThat(detail.product().hasVariants()).isTrue();
    assertThat(detail.product().available()).isTrue();
    assertThat(detail.variants())
        .singleElement()
        .satisfies(variant -> assertThat(variant.name()).isEqualTo("Pequena"));
    verify(availableImageLookup, never()).getAvailableImages(any(), any());
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
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var newCategory = fixture.createCategory(tenant, "Bebidas");
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
    var ownerTenant = fixture.createTenant(CORLEONE_PIZZERIA);
    var anotherTenant = fixture.createTenant(SOPRANO_PIZZERIA);
    var ownerCategory = fixture.createCategory(ownerTenant, CATEGORY_NAME);
    var anotherCategory = fixture.createCategory(anotherTenant, CATEGORY_NAME);
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
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var product = createProduct(tenant, category);

    catalogService.deactivate(tenant.getId(), product.id());

    assertThat(productRepository.findByIdAndTenantId(product.id(), tenant.getId()))
        .isPresent()
        .get()
        .extracting(Product::isActive)
        .isEqualTo(false);
  }

  @Test
  void shouldUseBasePriceWhenProductHasNoVariants() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var product = createProduct(tenant, category);
    stubAvailableImages(tenant.getId(), product);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products)
        .singleElement()
        .satisfies(
            summary -> {
              assertThat(summary.hasVariants()).isFalse();
              assertThat(summary.available()).isTrue();
              assertThat(summary.displayPrice()).isEqualByComparingTo(product.price());
            });
  }

  @Test
  void shouldUseMinimumActiveVariantPrice() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var product = createProduct(tenant, category);
    createVariant(tenant, product, LARGE_VARIANT_NAME, new BigDecimal("45.00"), 1, true);
    createVariant(tenant, product, "Pequena", new BigDecimal("39.00"), 0, true);
    stubAvailableImages(tenant.getId(), product);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products)
        .singleElement()
        .satisfies(
            summary -> {
              assertThat(summary.hasVariants()).isTrue();
              assertThat(summary.available()).isTrue();
              assertThat(summary.displayPrice()).isEqualByComparingTo("39.00");
            });
  }

  @Test
  void shouldIgnoreInactiveVariantsWhenCalculatingDisplayPrice() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var product = createProduct(tenant, category);
    createVariant(tenant, product, LARGE_VARIANT_NAME, new BigDecimal("45.00"), 0, true);
    createVariant(tenant, product, "Promocional", new BigDecimal("20.00"), 1, false);
    stubAvailableImages(tenant.getId(), product);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products)
        .singleElement()
        .satisfies(
            summary -> {
              assertThat(summary.hasVariants()).isTrue();
              assertThat(summary.available()).isTrue();
              assertThat(summary.displayPrice()).isEqualByComparingTo("45.00");
            });
  }

  @Test
  void shouldMarkProductUnavailableWhenAllVariantsAreInactive() {
    var tenant = createTenant();
    var category = fixture.createCategory(tenant, CATEGORY_NAME);
    var product = createProduct(tenant, category);
    createVariant(tenant, product, LARGE_VARIANT_NAME, new BigDecimal("45.00"), 0, false);
    stubAvailableImages(tenant.getId(), product);

    var products = catalogService.listActiveProducts(tenant.getId());

    assertThat(products)
        .singleElement()
        .satisfies(
            summary -> {
              assertThat(summary.hasVariants()).isTrue();
              assertThat(summary.available()).isFalse();
              assertThat(summary.displayPrice()).isNull();
            });
  }

  private Tenant createTenant() {
    return fixture.createTenant(CORLEONE_PIZZERIA);
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
    var image = fixture.createAvailableImage(tenantId);
    when(availableImageLookup.getAvailableImage(tenantId, image.getId()))
        .thenReturn(new AvailableImage(image.getId(), IMAGE_URL));
    return image.getId();
  }

  private void createInactiveProduct(Tenant tenant) {
    var category = fixture.createCategory(tenant, "Calzones");
    var result = createProduct(tenant, category, "Pizza Calabresa", new BigDecimal("42.00"));
    var product = productRepository.findByIdAndTenantId(result.id(), tenant.getId()).orElseThrow();
    product.deactivate();
    productRepository.save(product);
  }

  private void createVariant(
      Tenant tenant,
      ProductResult product,
      String name,
      BigDecimal price,
      int displayPosition,
      boolean active) {

    createVariant(tenant, product, null, name, price, displayPosition, active);
  }

  private ProductVariant createVariant(
      Tenant tenant,
      ProductResult product,
      UUID imageId,
      String name,
      BigDecimal price,
      int displayPosition,
      boolean active) {

    var variant =
        new ProductVariant(product.id(), tenant.getId(), imageId, name, price, displayPosition);

    variant.changeStatusTo(active);

    return variantRepository.save(variant);
  }
}
