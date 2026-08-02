package br.com.f2e.ovenplatform.orders.application.event;

import br.com.f2e.ovenplatform.orders.domain.OrderItem;
import java.math.BigDecimal;
import java.util.UUID;

public record OrderPlacedItem(
    UUID productId,
    String productName,
    UUID variantId,
    String variantName,
    int quantity,
    BigDecimal unitPrice) {

  public OrderPlacedItem(UUID productId, String productName, int quantity, BigDecimal unitPrice) {
    this(productId, productName, null, null, quantity, unitPrice);
  }

  public static OrderPlacedItem from(OrderItem item) {
    return new OrderPlacedItem(
        item.getProductId(),
        item.getProductName(),
        item.getVariantId(),
        item.getVariantName(),
        item.getQuantity(),
        item.getUnitPrice());
  }
}
