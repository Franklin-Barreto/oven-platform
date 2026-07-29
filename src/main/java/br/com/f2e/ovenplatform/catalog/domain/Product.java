package br.com.f2e.ovenplatform.catalog.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireMinimumSize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false)
  private UUID categoryId;

  @Column(nullable = false)
  private UUID imageId;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private boolean active;

  @SuppressWarnings("unused")
  protected Product() {}

  @SuppressFBWarnings(
      value = "CT_CONSTRUCTOR_THROW",
      justification = "Domain invariants are validated while constructing the aggregate.")
  public Product(
      UUID tenantId,
      UUID categoryId,
      UUID imageId,
      String name,
      String description,
      BigDecimal price) {

    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.categoryId = requireNotNull(categoryId, "categoryId");
    this.imageId = requireNotNull(imageId, "imageId");
    this.name = requireMinimumSize(name, "name", 5);
    this.description = normalizeDescription(description);
    this.price = requirePositive(price, "price");
    this.active = true;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getCategoryId() {
    return categoryId;
  }

  public UUID getImageId() {
    return imageId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public boolean isActive() {
    return active;
  }

  public void activate() {
    this.active = true;
  }

  public void deactivate() {
    this.active = false;
  }

  public void updateDetails(
      UUID categoryId,
      UUID imageId,
      String name,
      String description,
      BigDecimal price,
      boolean active) {

    var validatedCategoryId = requireNotNull(categoryId, "categoryId");
    var validatedImageId = requireNotNull(imageId, "imageId");
    var validatedName = requireMinimumSize(name, "name", 5);
    var validatedDescription = normalizeDescription(description);
    var validatedPrice = requirePositive(price, "price");

    this.categoryId = validatedCategoryId;
    this.imageId = validatedImageId;
    this.name = validatedName;
    this.description = validatedDescription;
    this.price = validatedPrice;
    this.active = active;
  }

  private static String normalizeDescription(String description) {
    if (description == null) {
      return null;
    }

    var trimmed = description.trim();
    if (trimmed.isBlank()) {
      return null;
    }

    if (trimmed.length() > 500) {
      throw new IllegalArgumentException("description must have at most 500 characters");
    }
    return trimmed;
  }
}
