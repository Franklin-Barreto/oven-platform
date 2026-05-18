package br.com.f2e.ovenplatform.orders.application.event;

import java.util.UUID;

public record OrderPaymentMarkedAsPaidEvent(UUID tenantId, UUID orderId) {}
