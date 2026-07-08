package br.com.f2e.ovenplatform.orders.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.application.event.payload.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderPaymentStatus;

public record PaymentInfo(PaymentMethod method, OrderPaymentStatus status) {
  public PaymentInfo {
    requireNotNull(method, "payment method");
    requireNotNull(status, "payment status");
  }
}
