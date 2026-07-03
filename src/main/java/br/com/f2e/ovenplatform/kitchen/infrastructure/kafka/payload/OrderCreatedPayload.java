package br.com.f2e.ovenplatform.kitchen.infrastructure.kafka.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedPayload(
    UUID tenantId, UUID orderId, List<OrderCreatedItemPayload> items) {

  public OrderCreatedPayload {
    items = List.copyOf(items);
  }

  @Override
  public List<OrderCreatedItemPayload> items() {
    return List.copyOf(items);
  }
}
