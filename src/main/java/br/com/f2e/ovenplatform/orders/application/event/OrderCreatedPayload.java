package br.com.f2e.ovenplatform.orders.application.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedPayload(
    UUID tenantId,
    UUID orderId,
    BigDecimal totalAmount,
    OrderPaymentMethod paymentMethod,
    OrderPaymentStatus paymentStatus,
    List<OrderCreatedItemPayload> items) {

  public OrderCreatedPayload {
    items = List.copyOf(items);
  }

  @Override
  public List<OrderCreatedItemPayload> items() {
    return List.copyOf(items);
  }
}
