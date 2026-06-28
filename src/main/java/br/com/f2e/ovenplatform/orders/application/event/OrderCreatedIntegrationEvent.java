package br.com.f2e.ovenplatform.orders.application.event;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedIntegrationEvent(
    UUID tenantId,
    UUID orderId,
    BigDecimal totalAmount,
    OrderPaymentMethod paymentMethod,
    OrderPaymentStatus paymentStatus) {}
