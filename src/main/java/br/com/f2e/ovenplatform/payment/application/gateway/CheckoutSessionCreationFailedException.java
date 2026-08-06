package br.com.f2e.ovenplatform.payment.application.gateway;

import java.util.UUID;

public class CheckoutSessionCreationFailedException extends RuntimeException {

  public CheckoutSessionCreationFailedException(UUID attemptId, Throwable cause) {
    super("Checkout session creation failed for attempt %s".formatted(attemptId), cause);
  }
}
