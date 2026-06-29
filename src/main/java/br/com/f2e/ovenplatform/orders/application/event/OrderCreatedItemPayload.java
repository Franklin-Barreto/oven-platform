package br.com.f2e.ovenplatform.orders.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedItemPayload(
    UUID productId, String productName, int quantity, BigDecimal unitPrice) {

  public static OrderCreatedItemPayload from(OrderPlacedItem item) {
    return new OrderCreatedItemPayload(
        item.productId(), item.productName(), item.quantity(), item.unitPrice());
  }
}
