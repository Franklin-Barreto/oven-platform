package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus;
import br.com.f2e.ovenplatform.payment.domain.PaymentProvider;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record ExternalPaymentAttemptResult(
    UUID attemptId,
    UUID paymentId,
    PaymentProvider provider,
    ExternalPaymentAttemptStatus status,
    String providerReference,
    URI redirectUrl,
    Instant expiresAt) {

  public static ExternalPaymentAttemptResult from(ExternalPaymentAttempt attempt) {
    return new ExternalPaymentAttemptResult(
        attempt.getId(),
        attempt.getPaymentId(),
        attempt.getProvider(),
        attempt.getStatus(),
        attempt.getProviderReference(),
        attempt.getRedirectUrl(),
        attempt.getExpiresAt());
  }
}
