package br.com.f2e.ovenplatform.fulfillment.infrastructure.kafka.payload;

import java.time.Instant;
import java.util.UUID;

public record KitchenTicketReadyPayload(
    UUID tenantId, UUID ticketId, UUID orderId, Instant readyAt) {}
