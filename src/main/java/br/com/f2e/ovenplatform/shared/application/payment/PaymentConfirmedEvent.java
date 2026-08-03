package br.com.f2e.ovenplatform.shared.application.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmedEvent(UUID tenantId, UUID orderId, Instant paidAt) {}
