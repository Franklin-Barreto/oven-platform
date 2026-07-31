package br.com.f2e.ovenplatform.catalog.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNonNegative;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireSize;

import br.com.f2e.ovenplatform.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "options")
public class Option extends BaseEntity {

  @Column(nullable = false)
  private UUID optionGroupId;

  @Column(nullable = false)
  private UUID tenantId;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal priceAdjustment;

  @Column(nullable = false)
  private int displayPosition;

  @Column(nullable = false)
  private boolean active;

  @SuppressWarnings("unused")
  protected Option() {}

  public Option(
      UUID optionGroupId,
      UUID tenantId,
      String name,
      BigDecimal priceAdjustment,
      int displayPosition) {
    this.optionGroupId = requireNotNull(optionGroupId, "optionGroupId");
    this.tenantId = requireNotNull(tenantId, "tenantId");
    this.name = requireSize(name, "name", 1, 80);
    this.priceAdjustment = requireNonNegative(priceAdjustment, "priceAdjustment");
    this.displayPosition = requireNonNegative(displayPosition, "displayPosition");
    this.active = true;
  }

  public UUID getOptionGroupId() {
    return optionGroupId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getPriceAdjustment() {
    return priceAdjustment;
  }

  public int getDisplayPosition() {
    return displayPosition;
  }

  public boolean isActive() {
    return active;
  }

  public void updateDetails(String name, BigDecimal priceAdjustment) {
    var validatedName = requireSize(name, "name", 1, 80);
    var validatedPriceAdjustment = requireNonNegative(priceAdjustment, "priceAdjustment");

    this.name = validatedName;
    this.priceAdjustment = validatedPriceAdjustment;
  }

  public void changeDisplayPosition(int displayPosition) {
    this.displayPosition = requireNonNegative(displayPosition, "displayPosition");
  }

  public void changeStatusTo(boolean active) {
    this.active = active;
  }
}
