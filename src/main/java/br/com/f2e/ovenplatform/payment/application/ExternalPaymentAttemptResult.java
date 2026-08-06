package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus;
import br.com.f2e.ovenplatform.payment.domain.PaymentProvider;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record ExternalPaymentAttemptResult(
    UUID attemptId,
    UUID paymentId,
    BigDecimal amount,
    String currency,
    PaymentProvider provider,
    ExternalPaymentAttemptStatus status,
    String providerReference,
    URI redirectUrl,
    Instant expiresAt) {

  public static ExternalPaymentAttemptResult from(ExternalPaymentAttempt attempt) {
    return new ExternalPaymentAttemptResult(
        attempt.getId(),
        attempt.getPaymentId(),
        attempt.getAmount(),
        attempt.getCurrency(),
        attempt.getProvider(),
        attempt.getStatus(),
        attempt.getProviderReference(),
        attempt.getRedirectUrl(),
        attempt.getExpiresAt());
  }
}
