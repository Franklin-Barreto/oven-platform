package br.com.f2e.ovenplatform.fulfillment.application;

import java.time.Instant;
import java.util.UUID;

public record PreparationReadyCommand(UUID tenantId, UUID orderId, Instant readyAt) {}
