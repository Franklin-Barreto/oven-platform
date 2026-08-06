package br.com.f2e.ovenplatform.payment.application.checkout;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record PaymentCheckoutSessionResult(UUID attemptId, URI checkoutUrl, Instant expiresAt) {}
