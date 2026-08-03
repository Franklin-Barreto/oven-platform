package br.com.f2e.ovenplatform.orders.application.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderReadyForPreparationEvent(
    UUID tenantId, UUID orderId, Instant releasedAt, List<OrderPlacedItem> items) {

  public OrderReadyForPreparationEvent {
    items = List.copyOf(items);
  }

  @Override
  public List<OrderPlacedItem> items() {
    return List.copyOf(items);
  }
}
