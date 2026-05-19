package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record OrderPaymentResponse(
    UUID orderId, PaymentMethod method, PaymentStatus status, Instant paidAt) {}
