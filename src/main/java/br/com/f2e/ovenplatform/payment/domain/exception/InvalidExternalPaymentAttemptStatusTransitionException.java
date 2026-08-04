package br.com.f2e.ovenplatform.payment.domain.exception;

import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus;

public class InvalidExternalPaymentAttemptStatusTransitionException extends RuntimeException {

  public InvalidExternalPaymentAttemptStatusTransitionException(
      ExternalPaymentAttemptStatus currentStatus, ExternalPaymentAttemptStatus targetStatus) {
    super(
        "Cannot transition external payment attempt from %s to %s."
            .formatted(currentStatus, targetStatus));
  }
}
