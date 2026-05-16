package br.com.f2e.ovenplatform.orders.application;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderPlacedEvent(
    UUID tenantId,
    UUID orderId,
    OrderPaymentMethod paymentMethod,
    OrderPaymentStatus paymentStatus,
    BigDecimal totalAmount) {}
