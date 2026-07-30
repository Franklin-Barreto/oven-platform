package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantRepository;
import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductVariantRepositoryAdapter;
import br.com.f2e.ovenplatform.catalog.support.CatalogTestFixture;
import br.com.f2e.ovenplatform.catalog.support.CatalogTestFixture.ProductFixture;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaProductVariantRepositoryAdapter.class)
class ProductVariantRepositoryIntegrationTest extends DataJpaIntegrationTest {

  private static final String LARGE_VARIANT_NAME = "Grande";
  private static final String MEDIUM_VARIANT_NAME = "Média";
  private static final String SMALL_VARIANT_NAME = "Pequena";

  @Autowired private ProductVariantRepository repository;

  private CatalogTestFixture catalogFixture;

  @BeforeEach
  void setUp() {
    catalogFixture = new CatalogTestFixture(entityManager);
  }

  @Test
  void shouldSaveAndRetrieveProductVariant() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Napoli");
    var variant = variant(fixture, fixture.image().getId(), LARGE_VARIANT_NAME, 1);

    repository.save(variant);
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndProductId(
                variant.getId(), fixture.tenant().getId(), fixture.product().getId()))
        .hasValueSatisfying(
            persisted -> {
              assertThat(persisted.getTenantId()).isEqualTo(fixture.tenant().getId());
              assertThat(persisted.getProductId()).isEqualTo(fixture.product().getId());
              assertThat(persisted.getImageId()).isEqualTo(fixture.image().getId());
              assertThat(persisted.getName()).isEqualTo(LARGE_VARIANT_NAME);
              assertThat(persisted.getPrice()).isEqualByComparingTo("41.00");
              assertThat(persisted.isActive()).isTrue();
              assertThat(persisted.getDisplayPosition()).isEqualTo(1);
            });
  }

  @Test
  void shouldSaveProductVariantWithoutImage() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Roma");
    var variant = variant(fixture, null, MEDIUM_VARIANT_NAME, 0);

    repository.save(variant);
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndProductId(
                variant.getId(), fixture.tenant().getId(), fixture.product().getId()))
        .hasValueSatisfying(persisted -> assertThat(persisted.getImageId()).isNull());
  }

  @Test
  void shouldReturnVariantsInDisplayOrder() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Milano");
    var large = variant(fixture, null, LARGE_VARIANT_NAME, 2);
    var small = variant(fixture, null, SMALL_VARIANT_NAME, 0);
    var medium = variant(fixture, null, MEDIUM_VARIANT_NAME, 1);

    repository.saveAll(List.of(large, small, medium));
    flushAndClear();

    assertThat(
            repository.findByTenantIdAndProductId(
                fixture.tenant().getId(), fixture.product().getId()))
        .extracting(ProductVariant::getName)
        .containsExactly(SMALL_VARIANT_NAME, MEDIUM_VARIANT_NAME, LARGE_VARIANT_NAME);
  }

  @Test
  void shouldNotFindVariantThroughAnotherTenant() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Firenze");
    var anotherFixture = catalogFixture.createProductFixture("Pizzeria Torino");
    var variant = repository.save(variant(fixture, null, "Família", 0));
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndProductId(
                variant.getId(), anotherFixture.tenant().getId(), fixture.product().getId()))
        .isEmpty();
  }

  @Test
  void shouldNotFindVariantThroughAnotherProduct() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Bologna");
    var anotherProduct =
        catalogFixture.createProduct(
            fixture.tenant(), fixture.category(), fixture.image(), "Pizza Margherita");
    entityManager.flush();
    var variant = repository.save(variant(fixture, null, "Individual", 0));
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndProductId(
                variant.getId(), fixture.tenant().getId(), anotherProduct.getId()))
        .isEmpty();
  }

  @Test
  void shouldRejectProductFromAnotherTenant() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Venezia");
    var anotherFixture = catalogFixture.createProductFixture("Pizzeria Genova");
    var variant =
        new ProductVariant(
            anotherFixture.product().getId(),
            fixture.tenant().getId(),
            null,
            LARGE_VARIANT_NAME,
            new BigDecimal("41.00"),
            0);
    repository.save(variant);

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_product_variants_tenant_product");
  }

  @Test
  void shouldRejectImageFromAnotherTenant() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Palermo");
    var anotherFixture = catalogFixture.createProductFixture("Pizzeria Bari");
    var variant = variant(fixture, anotherFixture.image().getId(), LARGE_VARIANT_NAME, 0);
    repository.save(variant);

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_product_variants_tenant_image");
  }

  @Test
  void shouldRejectNonexistentProduct() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Verona");
    var variant =
        new ProductVariant(
            UUID.randomUUID(),
            fixture.tenant().getId(),
            null,
            LARGE_VARIANT_NAME,
            new BigDecimal("41.00"),
            0);
    repository.save(variant);

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_product_variants_tenant_product");
  }

  @Test
  void shouldRejectNonexistentImage() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Catania");
    var variant = variant(fixture, UUID.randomUUID(), LARGE_VARIANT_NAME, 0);
    repository.save(variant);

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_product_variants_tenant_image");
  }

  @Test
  void shouldFindVariantsForMultipleProductsWithinTenantInDisplayOrder() {
    var fixture = catalogFixture.createProductFixture("Pizzeria Batch");
    var anotherProduct =
        catalogFixture.createProduct(
            fixture.tenant(), fixture.category(), fixture.image(), "Pizza Margherita");
    entityManager.flush();
    var anotherTenantFixture = catalogFixture.createProductFixture("Another Pizzeria");

    var large = variant(fixture, null, LARGE_VARIANT_NAME, 1);
    var small = variant(fixture, null, SMALL_VARIANT_NAME, 0);
    var anotherProductVariant =
        new ProductVariant(
            anotherProduct.getId(),
            fixture.tenant().getId(),
            null,
            MEDIUM_VARIANT_NAME,
            new BigDecimal("45.00"),
            0);
    var anotherTenantVariant = variant(anotherTenantFixture, null, "Família", 0);

    repository.saveAll(List.of(large, small, anotherProductVariant, anotherTenantVariant));
    flushAndClear();

    var variants =
        repository.findByTenantIdAndProductIds(
            fixture.tenant().getId(),
            Set.of(
                fixture.product().getId(),
                anotherProduct.getId(),
                anotherTenantFixture.product().getId()));

    assertThat(variants)
        .hasSize(3)
        .allMatch(variant -> variant.getTenantId().equals(fixture.tenant().getId()));
    assertThat(variants)
        .filteredOn(variant -> variant.getProductId().equals(fixture.product().getId()))
        .extracting(ProductVariant::getName, ProductVariant::getDisplayPosition)
        .containsExactly(
            tuple(SMALL_VARIANT_NAME, 0),
            tuple(LARGE_VARIANT_NAME, 1));
    assertThat(variants)
        .filteredOn(variant -> variant.getProductId().equals(anotherProduct.getId()))
        .extracting(ProductVariant::getName, ProductVariant::getDisplayPosition)
        .containsExactly(tuple(MEDIUM_VARIANT_NAME, 0));
  }

  private ProductVariant variant(
      ProductFixture fixture, UUID imageId, String name, int displayPosition) {
    return new ProductVariant(
        fixture.product().getId(),
        fixture.tenant().getId(),
        imageId,
        name,
        new BigDecimal("41.00"),
        displayPosition);
  }
}
