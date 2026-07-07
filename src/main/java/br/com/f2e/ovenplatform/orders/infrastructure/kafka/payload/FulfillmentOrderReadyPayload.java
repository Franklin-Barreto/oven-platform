package br.com.f2e.ovenplatform.orders.infrastructure.kafka.payload;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.time.Instant;
import java.util.UUID;

public record FulfillmentOrderReadyPayload(UUID tenantId, UUID orderId, Instant readyAt) {

  public FulfillmentOrderReadyPayload {
    requireNotNull(tenantId, "tenantId");
    requireNotNull(orderId, "orderId");
    requireNotNull(readyAt, "readyAt");
  }
}
