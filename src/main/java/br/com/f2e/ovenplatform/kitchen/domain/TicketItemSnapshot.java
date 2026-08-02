package br.com.f2e.ovenplatform.kitchen.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireMinimumSize;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
class TicketItemSnapshot {

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false, length = 80)
  private String productName;

  @Column private UUID variantId;

  @Column(length = 80)
  private String variantName;

  protected TicketItemSnapshot() {}

  TicketItemSnapshot(UUID productId, String productName, UUID variantId, String variantName) {
    this.productId = requireNotNull(productId, "productId");
    this.productName = requireMinimumSize(productName, "productName", 5);
    this.variantId = variantId;
    this.variantName = variantId == null ? null : requireMinimumSize(variantName, "variantName", 1);
  }

  UUID productId() {
    return productId;
  }

  String productName() {
    return productName;
  }

  UUID variantId() {
    return variantId;
  }

  String variantName() {
    return variantName;
  }
}
