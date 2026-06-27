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
      UUID tenantId, UUID categoryId, String name, String description, BigDecimal price) {

    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.categoryId = requireNotNull(categoryId, "categoryId");
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

  public void changeCategory(UUID categoryId) {
    this.categoryId = requireNotNull(categoryId, "categoryId");
  }

  public String getName() {
    return name;
  }

  public void rename(String name) {
    this.name = requireMinimumSize(name, "name", 5);
  }

  public String getDescription() {
    return description;
  }

  public void changeDescription(String description) {
    this.description = normalizeDescription(description);
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void changePrice(BigDecimal price) {
    this.price = requirePositive(price, "price");
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
