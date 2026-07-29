package br.com.f2e.ovenplatform.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.catalog.domain.Category;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import br.com.f2e.ovenplatform.catalog.infrastructure.persistence.JpaProductVariantRepositoryAdapter;
import br.com.f2e.ovenplatform.media.domain.StoredImage;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(JpaProductVariantRepositoryAdapter.class)
class ProductVariantRepositoryIntegrationTest extends DataJpaIntegrationTest {

  private static final String CHECKSUM = "0t/CUcGnJF1Ot9leX4FUcsbbz37maQu9fBkS9He2wio=";
  private static final String LARGE_VARIANT_NAME = "Grande";
  private static final String MEDIUM_VARIANT_NAME = "Média";
  private static final String SMALL_VARIANT_NAME = "Pequena";

  @Autowired private ProductVariantRepository repository;

  @Test
  void shouldSaveAndRetrieveProductVariant() {
    var fixture = createFixture("Pizzeria Napoli");
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
    var fixture = createFixture("Pizzeria Roma");
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
    var fixture = createFixture("Pizzeria Milano");
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
    var fixture = createFixture("Pizzeria Firenze");
    var anotherFixture = createFixture("Pizzeria Torino");
    var variant = repository.save(variant(fixture, null, "Família", 0));
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndProductId(
                variant.getId(), anotherFixture.tenant().getId(), fixture.product().getId()))
        .isEmpty();
  }

  @Test
  void shouldNotFindVariantThroughAnotherProduct() {
    var fixture = createFixture("Pizzeria Bologna");
    var anotherProduct = createProduct(fixture);
    var variant = repository.save(variant(fixture, null, "Individual", 0));
    flushAndClear();

    assertThat(
            repository.findByIdAndTenantIdAndProductId(
                variant.getId(), fixture.tenant().getId(), anotherProduct.getId()))
        .isEmpty();
  }

  @Test
  void shouldRejectProductFromAnotherTenant() {
    var fixture = createFixture("Pizzeria Venezia");
    var anotherFixture = createFixture("Pizzeria Genova");
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
    var fixture = createFixture("Pizzeria Palermo");
    var anotherFixture = createFixture("Pizzeria Bari");
    var variant = variant(fixture, anotherFixture.image().getId(), LARGE_VARIANT_NAME, 0);
    repository.save(variant);

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_product_variants_tenant_image");
  }

  @Test
  void shouldRejectNonexistentProduct() {
    var fixture = createFixture("Pizzeria Verona");
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
    var fixture = createFixture("Pizzeria Catania");
    var variant = variant(fixture, UUID.randomUUID(), LARGE_VARIANT_NAME, 0);
    repository.save(variant);

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(PersistenceException.class)
        .rootCause()
        .hasMessageContaining("fk_product_variants_tenant_image");
  }

  private Fixture createFixture(String tenantName) {
    var tenant = new Tenant(tenantName, Plan.MVP);
    entityManager.persist(tenant);

    var category = new Category("Pizzas", tenant.getId());
    entityManager.persist(category);

    var image = createAvailableImage(tenant.getId());
    var product =
        new Product(
            tenant.getId(),
            category.getId(),
            image.getId(),
            "Pizza Calabresa",
            null,
            new BigDecimal("35.00"));
    entityManager.persist(product);
    entityManager.flush();

    return new Fixture(tenant, category, image, product);
  }

  private Product createProduct(Fixture fixture) {
    var product =
        new Product(
            fixture.tenant().getId(),
            fixture.category().getId(),
            fixture.image().getId(),
                "Pizza Margherita",
            null,
            new BigDecimal("39.00"));
    entityManager.persist(product);
    entityManager.flush();
    return product;
  }

  private StoredImage createAvailableImage(UUID tenantId) {
    var image =
        StoredImage.pending(
            tenantId,
            "tenants/%s/images/%s.webp".formatted(tenantId, UUID.randomUUID()),
            "image/webp",
            1_024L,
            CHECKSUM);
    image.confirm("image/webp", 1_024L, CHECKSUM);
    entityManager.persist(image);
    return image;
  }

  private ProductVariant variant(Fixture fixture, UUID imageId, String name, int displayPosition) {
    return new ProductVariant(
        fixture.product().getId(),
        fixture.tenant().getId(),
        imageId,
        name,
        new BigDecimal("41.00"),
        displayPosition);
  }

  private record Fixture(Tenant tenant, Category category, StoredImage image, Product product) {}
}
