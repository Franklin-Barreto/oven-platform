package br.com.f2e.ovenplatform.orders.application;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderableProduct(
    UUID productId, String productName, UUID variantId, String variantName, BigDecimal unitPrice) {

  public OrderableProduct(UUID productId, String productName, BigDecimal unitPrice) {
    this(productId, productName, null, null, unitPrice);
  }

  public OrderableProductSelection selection() {
    return new OrderableProductSelection(productId, variantId);
  }
}
