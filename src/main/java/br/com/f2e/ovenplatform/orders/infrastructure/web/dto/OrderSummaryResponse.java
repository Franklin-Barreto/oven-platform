package br.com.f2e.ovenplatform.orders.infrastructure.web.dto;

import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
    UUID id,
    UUID tenantId,
    OrderStatus status,
    OrderServiceType serviceType,
    BigDecimal totalAmount,
    Instant createdAt,
    Instant readyAt,
    Instant completedAt,
    Instant cancelledAt) {

  public static OrderSummaryResponse from(Order order) {
    return new OrderSummaryResponse(
        order.getId(),
        order.getTenantId(),
        order.getStatus(),
        order.getServiceType(),
        order.getTotalAmount(),
        order.getCreatedAt(),
        order.getReadyAt(),
        order.getCompletedAt(),
        order.getCancelledAt());
  }
}
