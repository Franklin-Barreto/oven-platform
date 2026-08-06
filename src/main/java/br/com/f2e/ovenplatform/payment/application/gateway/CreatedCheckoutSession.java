package br.com.f2e.ovenplatform.payment.application.gateway;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.net.URI;
import java.time.Instant;

public record CreatedCheckoutSession(String providerReference, URI checkoutUrl, Instant expiresAt) {

  public CreatedCheckoutSession {
    providerReference = requireNotBlank(providerReference, "providerReference");
    requireNotNull(checkoutUrl, "checkoutUrl");
    requireNotNull(expiresAt, "expiresAt");
  }
}
