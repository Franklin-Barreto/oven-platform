package br.com.f2e.ovenplatform.shared.application.event.payload.order;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotEmpty;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import br.com.f2e.ovenplatform.shared.application.event.payload.PaymentMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedPayload(
    UUID tenantId,
    UUID orderId,
    BigDecimal totalAmount,
    PaymentMethod paymentMethod,
    OrderPaymentStatus paymentStatus,
    List<OrderCreatedItemPayload> items) {

  public OrderCreatedPayload {
    requireNotNull(tenantId, "tenantId");
    requireNotNull(orderId, "orderId");
    requirePositive(totalAmount, "totalAmount");
    requireNotNull(paymentMethod, "paymentMethod");
    requireNotNull(paymentStatus, "paymentStatus");
    items = List.copyOf(requireNotEmpty(items, "items"));
  }

  @Override
  public List<OrderCreatedItemPayload> items() {
    return List.copyOf(items);
  }
}
