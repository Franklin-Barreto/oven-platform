package br.com.f2e.ovenplatform.orders.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentProcessingMode;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;

public record PaymentInfo(
    PaymentMethod method, PaymentStatus status, PaymentProcessingMode processingMode) {

  public PaymentInfo(PaymentMethod method, PaymentStatus status) {
    this(method, status, PaymentProcessingMode.MANUAL);
  }

  public PaymentInfo {
    requireNotNull(method, "payment method");
    requireNotNull(status, "payment status");
    requireNotNull(processingMode, "payment processing mode");

    if (processingMode == PaymentProcessingMode.GATEWAY && method == PaymentMethod.CASH) {
      throw new IllegalArgumentException("gateway payment does not support cash");
    }
    if (processingMode == PaymentProcessingMode.GATEWAY && status == PaymentStatus.PAID) {
      throw new IllegalArgumentException("gateway payment must start pending");
    }
  }
}
