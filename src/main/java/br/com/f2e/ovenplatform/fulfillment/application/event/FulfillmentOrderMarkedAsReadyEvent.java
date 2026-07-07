package br.com.f2e.ovenplatform.fulfillment.application.event;

import java.time.Instant;
import java.util.UUID;

public record FulfillmentOrderMarkedAsReadyEvent(UUID tenantId, UUID orderId, Instant readyAt) {}
