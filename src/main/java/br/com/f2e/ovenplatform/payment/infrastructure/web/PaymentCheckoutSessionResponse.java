package br.com.f2e.ovenplatform.payment.infrastructure.web;

import br.com.f2e.ovenplatform.payment.application.checkout.PaymentCheckoutSessionResult;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record PaymentCheckoutSessionResponse(UUID attemptId, URI checkoutUrl, Instant expiresAt) {

  public static PaymentCheckoutSessionResponse from(PaymentCheckoutSessionResult result) {
    return new PaymentCheckoutSessionResponse(
        result.attemptId(), result.checkoutUrl(), result.expiresAt());
  }
}
