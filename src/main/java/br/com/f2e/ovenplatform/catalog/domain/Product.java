package br.com.f2e.ovenplatform.catalog.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireMinimumSize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
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

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(nullable = false)
  private boolean active;

  @SuppressWarnings("unused")
  protected Product() {}

  public Product(UUID tenantId, String name, BigDecimal price) {

    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.name = requireMinimumSize(name, "name", 5);
    this.price = requirePositive(price, "price");
    this.active = true;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public void rename(String name) {
    this.name = requireMinimumSize(name, "name", 5);
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
}
