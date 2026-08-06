package br.com.f2e.ovenplatform.payment.application.checkout;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record RegisterExternalCheckoutCommand(
    UUID tenantId, UUID attemptId, String providerReference, URI checkoutUrl, Instant expiresAt) {
  public RegisterExternalCheckoutCommand {
    requireNotNull(tenantId, "tenantId");
    requireNotNull(attemptId, "attemptId");
    providerReference = requireNotBlank(providerReference, "providerReference");
    requireNotNull(checkoutUrl, "checkoutUrl");
    requireNotNull(expiresAt, "expiresAt");
  }
}
