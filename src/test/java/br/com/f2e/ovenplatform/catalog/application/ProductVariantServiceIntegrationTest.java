package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.catalog.application.variant.CreateProductVariantCommand;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantRepository;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResult;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantService;
import br.com.f2e.ovenplatform.catalog.application.variant.ReorderProductVariantsCommand;
import br.com.f2e.ovenplatform.catalog.application.variant.UpdateProductVariantCommand;
import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import br.com.f2e.ovenplatform.catalog.support.CatalogTestFixture;
import br.com.f2e.ovenplatform.media.application.api.AvailableImage;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProductVariantServiceIntegrationTest extends CatalogDataJpaIntegrationTest {

  private static final String TENANT_NAME = "Pizzaria do Paulão";
  private static final String MEDIUM_VARIANT_NAME = "Média";
  private static final String LARGE_VARIANT_NAME = "Grande";
  private static final String LA_CASA_DO_PASTEL = "La casa do pastel";
  private static final BigDecimal VARIANT_PRICE = new BigDecimal("25.00");
  private static final URI IMAGE_URL = URI.create("https://images.example/variants/large.webp");

  @Autowired private ProductVariantService service;
  @Autowired private ProductVariantRepository variantRepository;

  private CatalogTestFixture fixture;

  @BeforeEach
  void setUp() {
    fixture = new CatalogTestFixture(entityManager);
  }

  @Test
  void shouldCreateFirstVariantAtPositionZero() {

    var productFixture = fixture.createProductFixture(LA_CASA_DO_PASTEL);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();

    var command =
        new CreateProductVariantCommand(
            productFixture.image().getId(), MEDIUM_VARIANT_NAME, VARIANT_PRICE);
    when(availableImageLookup.getAvailableImage(tenantId, productFixture.image().getId()))
        .thenReturn(new AvailableImage(productFixture.image().getId(), IMAGE_URL));

    var variant = service.create(tenantId, productId, command);

    var persistedVariant =
        variantRepository.findByIdAndTenantIdAndProductId(variant.id(), tenantId, productId);

    assertThat(persistedVariant)
        .isPresent()
        .get()
        .extracting(
            ProductVariant::getProductId,
            ProductVariant::getDisplayPosition,
            ProductVariant::isActive)
        .containsExactly(productId, 0, true);
  }

  @Test
  void shouldAppendVariantAfterExistingVariants() {

    var productFixture = fixture.createProductFixture(LA_CASA_DO_PASTEL);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var imageId = productFixture.image().getId();

    var firstVariant = new CreateProductVariantCommand(imageId, MEDIUM_VARIANT_NAME, VARIANT_PRICE);
    var secondVariant =
        new CreateProductVariantCommand(imageId, LARGE_VARIANT_NAME, new BigDecimal("35.00"));
    when(availableImageLookup.getAvailableImage(tenantId, imageId))
        .thenReturn(new AvailableImage(imageId, IMAGE_URL));

    var first = service.create(tenantId, productId, firstVariant);
    flushAndClear();
    var second = service.create(tenantId, productId, secondVariant);

    var persistedVariants = variantRepository.findByTenantIdAndProductId(tenantId, productId);

    assertThat(persistedVariants)
        .extracting(ProductVariant::getId, ProductVariant::getDisplayPosition)
        .containsExactly(tuple(first.id(), 0), tuple(second.id(), 1));
  }

  @Test
  void shouldCreateVariantWithoutImage() {

    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();

    var command = new CreateProductVariantCommand(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE);

    var variant = service.create(tenantId, productId, command);

    assertThat(variantRepository.findByIdAndTenantIdAndProductId(variant.id(), tenantId, productId))
        .isPresent()
        .get()
        .extracting(ProductVariant::getImageId)
        .isNull();
  }

  @Test
  void shouldRejectVariantWhenProductDoesNotExist() {

    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = UUID.randomUUID();

    var command = new CreateProductVariantCommand(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE);

    assertThatThrownBy(() -> service.create(tenantId, productId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  @Test
  void shouldRejectVariantWhenProductBelongsToAnotherTenant() {

    var ownerFixture = fixture.createProductFixture(TENANT_NAME);
    var anotherTenant = fixture.createTenant("Pizzaria do Silvio");
    var tenantId = anotherTenant.getId();
    var productId = ownerFixture.product().getId();

    var command = new CreateProductVariantCommand(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE);

    assertThatThrownBy(() -> service.create(tenantId, productId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  @Test
  void shouldRejectVariantWhenImageIsUnavailable() {

    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var imageId = UUID.randomUUID();

    var command = new CreateProductVariantCommand(imageId, MEDIUM_VARIANT_NAME, VARIANT_PRICE);
    assertThrow(tenantId, imageId);

    assertThatThrownBy(() -> service.create(tenantId, productId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("StoredImage id: %s not found".formatted(imageId));
  }

  @Test
  void shouldRejectVariantWhenImageBelongsToAnotherTenant() {

    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var anotherFixture = fixture.createProductFixture("Pizzaria do Silvio");
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var imageId = anotherFixture.image().getId();

    var command = new CreateProductVariantCommand(imageId, MEDIUM_VARIANT_NAME, VARIANT_PRICE);

    assertThrow(tenantId, imageId);

    assertThatThrownBy(() -> service.create(tenantId, productId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("StoredImage id: %s not found".formatted(imageId));
    verify(availableImageLookup).getAvailableImage(tenantId, imageId);
  }

  @Test
  void shouldNotValidateImageWhenVariantUsesProductImage() {

    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();

    var command = new CreateProductVariantCommand(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE);

    service.create(tenantId, productId, command);

    verifyNoInteractions(availableImageLookup);
  }

  @Test
  void shouldNotSaveVariantWhenProductValidationFails() {

    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = UUID.randomUUID();
    var imageId = productFixture.image().getId();

    var command = new CreateProductVariantCommand(imageId, MEDIUM_VARIANT_NAME, VARIANT_PRICE);

    assertThatThrownBy(() -> service.create(tenantId, productId, command))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThat(variantRepository.findByTenantIdAndProductId(tenantId, productId)).isEmpty();
    verify(availableImageLookup, never()).getAvailableImage(tenantId, imageId);
  }

  @Test
  void shouldNotSaveVariantWhenImageValidationFails() {

    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var imageId = UUID.randomUUID();

    var command = new CreateProductVariantCommand(imageId, MEDIUM_VARIANT_NAME, VARIANT_PRICE);
    assertThrow(tenantId, imageId);

    assertThatThrownBy(() -> service.create(tenantId, productId, command))
        .isInstanceOf(ResourceNotFoundException.class);

    assertThat(variantRepository.findByTenantIdAndProductId(tenantId, productId)).isEmpty();
  }

  @Test
  void shouldListVariantsInDisplayOrderWithTheirImageOverrides() {

    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var imageId = productFixture.image().getId();

    var large =
        variantRepository.save(
            new ProductVariant(
                productId, tenantId, imageId, LARGE_VARIANT_NAME, new BigDecimal("35.00"), 1));
    var medium =
        variantRepository.save(
            new ProductVariant(productId, tenantId, null, MEDIUM_VARIANT_NAME, VARIANT_PRICE, 0));

    flushAndClear();

    when(availableImageLookup.getAvailableImages(tenantId, Set.of(imageId)))
        .thenReturn(java.util.List.of(new AvailableImage(imageId, IMAGE_URL)));

    var variants = service.listVariants(tenantId, productId);

    assertThat(variants)
        .extracting(
            ProductVariantResult::id,
            ProductVariantResult::imageId,
            ProductVariantResult::imageUrl,
            ProductVariantResult::displayPosition)
        .containsExactly(
            tuple(medium.getId(), null, null, 0), tuple(large.getId(), imageId, IMAGE_URL, 1));
    verify(availableImageLookup).getAvailableImages(tenantId, Set.of(imageId));
  }

  @Test
  void shouldReturnEmptyListWithoutResolvingImagesWhenProductHasNoVariants() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    clearInvocations(availableImageLookup);

    var variants = service.listVariants(tenantId, productId);

    assertThat(variants).isEmpty();
    verifyNoInteractions(availableImageLookup);
  }

  @Test
  void shouldRejectListingWhenProductDoesNotExist() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = UUID.randomUUID();

    assertThatThrownBy(() -> service.listVariants(tenantId, productId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  @Test
  void shouldRejectListingWhenProductBelongsToAnotherTenant() {
    var ownerFixture = fixture.createProductFixture(TENANT_NAME);
    var anotherTenant = fixture.createTenant("Pizzaria do Silvio");
    var tenantId = anotherTenant.getId();
    var productId = ownerFixture.product().getId();

    assertThatThrownBy(() -> service.listVariants(tenantId, productId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  @Test
  void shouldUpdateVariantDetailsWithoutChangingDisplayPosition() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var imageId = productFixture.image().getId();
    var variant =
        variantRepository.save(
            new ProductVariant(productId, tenantId, null, MEDIUM_VARIANT_NAME, VARIANT_PRICE, 3));
    flushAndClear();

    var updatedPrice = new BigDecimal("37.50");
    var command = new UpdateProductVariantCommand(imageId, LARGE_VARIANT_NAME, updatedPrice);
    when(availableImageLookup.getAvailableImage(tenantId, imageId))
        .thenReturn(new AvailableImage(imageId, IMAGE_URL));

    var result = service.update(tenantId, productId, variant.getId(), command);
    flushAndClear();

    assertThat(result)
        .extracting(
            ProductVariantResult::id,
            ProductVariantResult::imageId,
            ProductVariantResult::imageUrl,
            ProductVariantResult::name,
            ProductVariantResult::price,
            ProductVariantResult::active,
            ProductVariantResult::displayPosition)
        .containsExactly(
            variant.getId(), imageId, IMAGE_URL, LARGE_VARIANT_NAME, updatedPrice, true, 3);
    assertThat(
            variantRepository.findByIdAndTenantIdAndProductId(variant.getId(), tenantId, productId))
        .isPresent()
        .get()
        .extracting(
            ProductVariant::getImageId,
            ProductVariant::getName,
            ProductVariant::getPrice,
            ProductVariant::isActive,
            ProductVariant::getDisplayPosition)
        .containsExactly(imageId, LARGE_VARIANT_NAME, updatedPrice, true, 3);
  }

  @Test
  void shouldRemoveVariantImageWithoutResolvingIt() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var variant =
        variantRepository.save(
            new ProductVariant(
                productId,
                tenantId,
                productFixture.image().getId(),
                MEDIUM_VARIANT_NAME,
                VARIANT_PRICE,
                0));
    flushAndClear();
    clearInvocations(availableImageLookup);

    var command = new UpdateProductVariantCommand(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE);

    var result = service.update(tenantId, productId, variant.getId(), command);
    flushAndClear();

    assertThat(result.imageId()).isNull();
    assertThat(result.imageUrl()).isNull();
    assertThat(
            variantRepository.findByIdAndTenantIdAndProductId(variant.getId(), tenantId, productId))
        .isPresent()
        .get()
        .extracting(ProductVariant::getImageId)
        .isNull();
    verifyNoInteractions(availableImageLookup);
  }

  @Test
  void shouldRejectUpdateWhenProductDoesNotExist() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = UUID.randomUUID();
    var variantId = UUID.randomUUID();
    var command = new UpdateProductVariantCommand(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE);

    assertThatThrownBy(() -> service.update(tenantId, productId, variantId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  @Test
  void shouldRejectUpdateWhenProductBelongsToAnotherTenant() {
    var ownerFixture = fixture.createProductFixture(TENANT_NAME);
    var anotherTenant = fixture.createTenant("Pizzaria do Silvio");
    var tenantId = anotherTenant.getId();
    var productId = ownerFixture.product().getId();
    var variantId = UUID.randomUUID();
    var command = new UpdateProductVariantCommand(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE);

    assertThatThrownBy(() -> service.update(tenantId, productId, variantId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  @Test
  void shouldRejectUpdateWhenVariantDoesNotBelongToProduct() {
    var firstProduct = fixture.createProductFixture(TENANT_NAME);
    var secondProduct =
        fixture.createProduct(
            firstProduct.tenant(),
            firstProduct.category(),
            firstProduct.image(),
            "Pizza Margherita");
    var tenantId = firstProduct.tenant().getId();
    var variant =
        variantRepository.save(
            new ProductVariant(
                firstProduct.product().getId(),
                tenantId,
                null,
                MEDIUM_VARIANT_NAME,
                VARIANT_PRICE,
                0));
    flushAndClear();
    var command = new UpdateProductVariantCommand(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE);
    var productId = secondProduct.getId();
    var variantId = variant.getId();

    assertThatThrownBy(() -> service.update(tenantId, productId, variantId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("ProductVariant id: %s not found".formatted(variantId));
  }

  @Test
  void shouldRejectUpdateWhenVariantBelongsToAnotherTenant() {
    var ownerFixture = fixture.createProductFixture(TENANT_NAME);
    var anotherFixture = fixture.createProductFixture("Pizzaria do Silvio");
    var variant =
        variantRepository.save(
            new ProductVariant(
                ownerFixture.product().getId(),
                ownerFixture.tenant().getId(),
                null,
                MEDIUM_VARIANT_NAME,
                VARIANT_PRICE,
                0));
    flushAndClear();
    var command = new UpdateProductVariantCommand(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE);
    var tenantId = anotherFixture.tenant().getId();
    var productId = anotherFixture.product().getId();
    var variantId = variant.getId();

    assertThatThrownBy(() -> service.update(tenantId, productId, variantId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("ProductVariant id: %s not found".formatted(variantId));
  }

  @Test
  void shouldNotPersistUpdateWhenImageValidationFails() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var variant =
        variantRepository.save(
            new ProductVariant(productId, tenantId, null, MEDIUM_VARIANT_NAME, VARIANT_PRICE, 0));
    flushAndClear();
    var imageId = UUID.randomUUID();
    var command =
        new UpdateProductVariantCommand(imageId, LARGE_VARIANT_NAME, new BigDecimal("35.00"));
    assertThrow(tenantId, imageId);
    var variantId = variant.getId();

    assertThatThrownBy(() -> service.update(tenantId, productId, variantId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("StoredImage id: %s not found".formatted(imageId));
    flushAndClear();

    assertThat(
            variantRepository.findByIdAndTenantIdAndProductId(variant.getId(), tenantId, productId))
        .isPresent()
        .get()
        .extracting(
            ProductVariant::getImageId,
            ProductVariant::getName,
            ProductVariant::getPrice,
            ProductVariant::isActive)
        .containsExactly(null, MEDIUM_VARIANT_NAME, VARIANT_PRICE, true);
  }

  @Test
  void shouldRejectUpdateWhenImageBelongsToAnotherTenant() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var anotherFixture = fixture.createProductFixture("Pizzaria do Silvio");
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var variant =
        variantRepository.save(
            new ProductVariant(productId, tenantId, null, MEDIUM_VARIANT_NAME, VARIANT_PRICE, 0));
    flushAndClear();
    var imageId = anotherFixture.image().getId();
    var command = new UpdateProductVariantCommand(imageId, MEDIUM_VARIANT_NAME, VARIANT_PRICE);
    assertThrow(tenantId, imageId);
    var variantId = variant.getId();

    assertThatThrownBy(() -> service.update(tenantId, productId, variantId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("StoredImage id: %s not found".formatted(imageId));
    verify(availableImageLookup).getAvailableImage(tenantId, imageId);
  }

  @Test
  void shouldDeactivateVariant() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var variant = variant(productId, tenantId, MEDIUM_VARIANT_NAME, 0);
    flushAndClear();

    service.changeStatus(tenantId, productId, variant.getId(), false);
    flushAndClear();

    assertThat(
            variantRepository.findByIdAndTenantIdAndProductId(variant.getId(), tenantId, productId))
        .isPresent()
        .get()
        .extracting(ProductVariant::isActive)
        .isEqualTo(false);
  }

  @Test
  void shouldActivateVariant() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var variant = variant(productId, tenantId, MEDIUM_VARIANT_NAME, 0);
    variant.changeStatusTo(false);
    flushAndClear();

    service.changeStatus(tenantId, productId, variant.getId(), true);
    flushAndClear();

    assertThat(
            variantRepository.findByIdAndTenantIdAndProductId(variant.getId(), tenantId, productId))
        .isPresent()
        .get()
        .extracting(ProductVariant::isActive)
        .isEqualTo(true);
  }

  @Test
  void shouldRejectStatusChangeWhenVariantDoesNotBelongToProduct() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var anotherProduct =
        fixture.createProduct(
            productFixture.tenant(),
            productFixture.category(),
            productFixture.image(),
            "Pizza Margherita");
    var tenantId = productFixture.tenant().getId();
    var productId = anotherProduct.getId();
    var variant = variant(productFixture.product().getId(), tenantId, MEDIUM_VARIANT_NAME, 0);
    var variantId = variant.getId();
    flushAndClear();

    assertThatThrownBy(() -> service.changeStatus(tenantId, productId, variantId, false))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("ProductVariant id: %s not found".formatted(variantId));
  }

  @Test
  void shouldReorderAllProductVariants() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var medium = variant(productId, tenantId, MEDIUM_VARIANT_NAME, 0);
    var large = variant(productId, tenantId, LARGE_VARIANT_NAME, 1);
    var family = variant(productId, tenantId, "Família", 2);
    flushAndClear();

    service.reorder(
        tenantId,
        productId,
        new ReorderProductVariantsCommand(List.of(family.getId(), medium.getId(), large.getId())));
    flushAndClear();

    assertThat(variantRepository.findByTenantIdAndProductId(tenantId, productId))
        .extracting(ProductVariant::getId, ProductVariant::getDisplayPosition)
        .containsExactly(
            tuple(family.getId(), 0), tuple(medium.getId(), 1), tuple(large.getId(), 2));
  }

  @Test
  void shouldAllowEmptyOrderWhenProductHasNoVariants() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();

    service.reorder(tenantId, productId, new ReorderProductVariantsCommand(List.of()));

    assertThat(variantRepository.findByTenantIdAndProductId(tenantId, productId)).isEmpty();
  }

  @Test
  void shouldRejectReorderWithMissingVariantWithoutChangingPositions() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var medium = variant(productId, tenantId, MEDIUM_VARIANT_NAME, 0);
    var large = variant(productId, tenantId, LARGE_VARIANT_NAME, 1);
    flushAndClear();
    var command = new ReorderProductVariantsCommand(List.of(large.getId()));

    assertThatThrownBy(() -> service.reorder(tenantId, productId, command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("variantIds must contain exactly all product variant ids");
    flushAndClear();

    assertThat(variantRepository.findByTenantIdAndProductId(tenantId, productId))
        .extracting(ProductVariant::getId, ProductVariant::getDisplayPosition)
        .containsExactly(tuple(medium.getId(), 0), tuple(large.getId(), 1));
  }

  @Test
  void shouldRejectReorderWithDuplicatedVariant() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var variant = variant(productId, tenantId, MEDIUM_VARIANT_NAME, 0);
    flushAndClear();
    var command = new ReorderProductVariantsCommand(List.of(variant.getId(), variant.getId()));

    assertThatThrownBy(() -> service.reorder(tenantId, productId, command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("variantIds must not contain duplicates");
  }

  @Test
  void shouldRejectReorderWithVariantFromAnotherProduct() {
    var firstFixture = fixture.createProductFixture(TENANT_NAME);
    var secondProduct =
        fixture.createProduct(
            firstFixture.tenant(),
            firstFixture.category(),
            firstFixture.image(),
            "Pizza Margherita");
    var tenantId = firstFixture.tenant().getId();
    var firstVariant = variant(firstFixture.product().getId(), tenantId, MEDIUM_VARIANT_NAME, 0);
    var secondVariant = variant(secondProduct.getId(), tenantId, LARGE_VARIANT_NAME, 0);
    flushAndClear();
    var productId = firstFixture.product().getId();
    var command = new ReorderProductVariantsCommand(List.of(secondVariant.getId()));

    assertThatThrownBy(() -> service.reorder(tenantId, productId, command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("variantIds must contain exactly all product variant ids");
    flushAndClear();

    assertThat(
            variantRepository.findByIdAndTenantIdAndProductId(
                firstVariant.getId(), tenantId, productId))
        .isPresent()
        .get()
        .extracting(ProductVariant::getDisplayPosition)
        .isEqualTo(0);
  }

  @Test
  void shouldRejectReorderWithVariantFromAnotherTenant() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var anotherFixture = fixture.createProductFixture("Pizzaria do Silvio");
    var tenantId = productFixture.tenant().getId();
    var productId = productFixture.product().getId();
    var variant = variant(productId, tenantId, MEDIUM_VARIANT_NAME, 0);
    var foreignVariant =
        variant(
            anotherFixture.product().getId(),
            anotherFixture.tenant().getId(),
            LARGE_VARIANT_NAME,
            0);
    flushAndClear();
    var command = new ReorderProductVariantsCommand(List.of(foreignVariant.getId()));

    assertThatThrownBy(() -> service.reorder(tenantId, productId, command))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("variantIds must contain exactly all product variant ids");
    flushAndClear();

    assertThat(
            variantRepository.findByIdAndTenantIdAndProductId(variant.getId(), tenantId, productId))
        .isPresent()
        .get()
        .extracting(ProductVariant::getDisplayPosition)
        .isEqualTo(0);
  }

  @Test
  void shouldRejectReorderWhenProductDoesNotExist() {
    var productFixture = fixture.createProductFixture(TENANT_NAME);
    var tenantId = productFixture.tenant().getId();
    var productId = UUID.randomUUID();
    var command = new ReorderProductVariantsCommand(List.of());

    assertThatThrownBy(() -> service.reorder(tenantId, productId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product id: %s not found".formatted(productId));
  }

  private ProductVariant variant(UUID productId, UUID tenantId, String name, int displayPosition) {
    return variantRepository.save(
        new ProductVariant(productId, tenantId, null, name, VARIANT_PRICE, displayPosition));
  }

  private void assertThrow(UUID tenantId, UUID imageId) {
    when(availableImageLookup.getAvailableImage(tenantId, imageId))
        .thenThrow(new ResourceNotFoundException("StoredImage", imageId));
  }
}
