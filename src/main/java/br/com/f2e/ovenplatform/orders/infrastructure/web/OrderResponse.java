package br.com.f2e.ovenplatform.orders.infrastructure.web;

import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
    UUID id,
    UUID tenantId,
    OrderStatus status,
    BigDecimal totalAmount,
    List<OrderItemResponse> items) {

  public OrderResponse {
    items = List.copyOf(items);
  }

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getTenantId(),
        order.getStatus(),
        order.getTotalAmount(),
        OrderItemResponse.from(order.getItems()));
  }
}
