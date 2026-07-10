package br.com.f2e.ovenplatform.orders.infrastructure.web.dto;

import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID tenantId,
    OrderStatus status,
    OrderServiceType serviceType,
    BigDecimal totalAmount,
    Instant createdAt,
    Instant readyAt,
    Instant deliveredAt,
    Instant cancelledAt,
    List<OrderItemResponse> items) {

  public OrderResponse {
    items = List.copyOf(items);
  }

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getTenantId(),
        order.getStatus(),
        order.getServiceType(),
        order.getTotalAmount(),
        order.getCreatedAt(),
        order.getReadyAt(),
        order.getDeliveredAt(),
        order.getCancelledAt(),
        OrderItemResponse.from(order.getItems()));
  }
}
