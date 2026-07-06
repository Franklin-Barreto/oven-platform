package br.com.f2e.ovenplatform.kitchen.application.event;

import java.time.Instant;
import java.util.UUID;

public record KitchenTicketMarkedAsReadyEvent(
    UUID tenantId, UUID ticketId, UUID orderId, Instant readyAt) {}
