package br.com.f2e.ovenplatform.catalog.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNonNegative;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireSize;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false)
  private UUID tenantId;

  @Column private UUID imageId;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private boolean active;

  @Column(nullable = false)
  private int displayPosition;

  @SuppressWarnings("unused")
  protected ProductVariant() {}

  public ProductVariant(
      UUID productId,
      UUID tenantId,
      UUID imageId,
      String name,
      BigDecimal price,
      int displayPosition) {
    this.productId = requireNotNull(productId, "productId");
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.imageId = imageId;
    this.name = requireSize(name, "name", 1, 80);
    this.price = requirePositive(price, "price");
    this.active = true;
    this.displayPosition = requireNonNegative(displayPosition, "displayPosition");
  }

  public UUID getProductId() {
    return productId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UUID getImageId() {
    return imageId;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public boolean isActive() {
    return active;
  }

  public int getDisplayPosition() {
    return displayPosition;
  }

  public void updateDetails(UUID imageId, String name, BigDecimal price, boolean active) {
    var validatedName = requireSize(name, "name", 1, 80);
    var validatedPrice = requirePositive(price, "price");

    this.imageId = imageId;
    this.name = validatedName;
    this.price = validatedPrice;
    this.active = active;
  }

  public void changeDisplayPosition(int displayPosition) {
    this.displayPosition = requireNonNegative(displayPosition, "displayPosition");
  }

  public void activate() {
    this.active = true;
  }

  public void deactivate() {
    this.active = false;
  }
}
