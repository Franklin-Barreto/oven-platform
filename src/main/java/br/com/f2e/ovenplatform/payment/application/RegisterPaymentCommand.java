package br.com.f2e.ovenplatform.payment.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record RegisterPaymentCommand(
    UUID tenantId,
    UUID orderId,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    PaymentStatus paymentStatus) {

  public RegisterPaymentCommand {
    requireNotNull(tenantId, "tenantId");
    requireNotNull(orderId, "orderId");
    requirePositive(amount, "amount");
    requireNotNull(paymentMethod, "paymentMethod");
    requireNotNull(paymentStatus, "paymentStatus");
  }
}
