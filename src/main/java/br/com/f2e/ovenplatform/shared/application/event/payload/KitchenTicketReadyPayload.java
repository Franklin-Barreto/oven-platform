package br.com.f2e.ovenplatform.shared.application.event.payload;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.time.Instant;
import java.util.UUID;

public record KitchenTicketReadyPayload(
    UUID tenantId, UUID ticketId, UUID orderId, Instant readyAt) {

  public KitchenTicketReadyPayload {
    requireNotNull(tenantId, "tenantId");
    requireNotNull(ticketId, "ticketId");
    requireNotNull(orderId, "orderId");
    requireNotNull(readyAt, "readyAt");
  }
}
