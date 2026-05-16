package br.com.f2e.ovenplatform.orders.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

public record PaymentInfo(OrderPaymentMethod method, OrderPaymentStatus status) {
  public PaymentInfo {
    requireNotNull(method, "payment method");
    requireNotNull(status, "payment status");
  }
}
