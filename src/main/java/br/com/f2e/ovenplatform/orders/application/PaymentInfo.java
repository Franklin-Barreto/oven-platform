package br.com.f2e.ovenplatform.orders.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;

public record PaymentInfo(PaymentMethod method, PaymentStatus status) {
  public PaymentInfo {
    requireNotNull(method, "payment method");
    requireNotNull(status, "payment status");
  }
}
