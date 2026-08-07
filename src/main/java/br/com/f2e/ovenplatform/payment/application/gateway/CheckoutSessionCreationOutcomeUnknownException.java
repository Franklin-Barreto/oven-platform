package br.com.f2e.ovenplatform.payment.application.gateway;

import java.util.UUID;

public class CheckoutSessionCreationOutcomeUnknownException extends RuntimeException {

  public CheckoutSessionCreationOutcomeUnknownException(UUID attemptId, Throwable cause) {
    super(
        "Checkout session creation outcome is unknown for attempt %s".formatted(attemptId), cause);
  }

  public CheckoutSessionCreationOutcomeUnknownException(
      UUID attemptId, String reason, Throwable cause) {
    super(
        "Checkout session creation outcome is unknown for attempt %s: %s"
            .formatted(attemptId, reason),
        cause);
  }
}
