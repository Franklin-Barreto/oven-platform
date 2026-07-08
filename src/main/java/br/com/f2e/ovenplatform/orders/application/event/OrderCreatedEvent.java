package br.com.f2e.ovenplatform.orders.application.event;

import br.com.f2e.ovenplatform.shared.application.event.payload.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderPaymentStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
    UUID tenantId,
    UUID orderId,
    PaymentMethod paymentMethod,
    OrderPaymentStatus paymentStatus,
    BigDecimal totalAmount,
    List<OrderPlacedItem> items) {

  public OrderCreatedEvent {
    items = List.copyOf(items);
  }

  @Override
  public List<OrderPlacedItem> items() {
    return List.copyOf(items);
  }
}
