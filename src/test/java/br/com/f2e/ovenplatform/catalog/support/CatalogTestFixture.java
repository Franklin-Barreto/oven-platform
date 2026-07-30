package br.com.f2e.ovenplatform.catalog.support;

import br.com.f2e.ovenplatform.catalog.domain.Category;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.media.domain.StoredImage;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.UUID;

public final class CatalogTestFixture {

  private static final String CHECKSUM = "0t/CUcGnJF1Ot9leX4FUcsbbz37maQu9fBkS9He2wio=";

  private final EntityManager entityManager;

  public CatalogTestFixture(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public ProductFixture createProductFixture(String tenantName) {
    var tenant = createTenant(tenantName);
    var category = createCategory(tenant, "Pizzas");
    var image = createAvailableImage(tenant.getId());
    var product = createProduct(tenant, category, image, "Pizza Calabresa");
    entityManager.flush();
    return new ProductFixture(tenant, category, image, product);
  }

  public Tenant createTenant(String name) {
    var tenant = new Tenant(name, Plan.MVP);
    entityManager.persist(tenant);
    return tenant;
  }

  public Category createCategory(Tenant tenant, String name) {
    var category = new Category(name, tenant.getId());
    entityManager.persist(category);
    return category;
  }

  public StoredImage createAvailableImage(UUID tenantId) {
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

  public Product createProduct(Tenant tenant, Category category, StoredImage image, String name) {
    var product =
        new Product(
            tenant.getId(), category.getId(), image.getId(), name, null, new BigDecimal("35.00"));
    entityManager.persist(product);
    return product;
  }

  public record ProductFixture(
      Tenant tenant, Category category, StoredImage image, Product product) {}
}
