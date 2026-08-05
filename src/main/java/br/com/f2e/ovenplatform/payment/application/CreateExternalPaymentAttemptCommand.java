package br.com.f2e.ovenplatform.payment.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.payment.domain.PaymentProvider;
import java.util.UUID;

public record CreateExternalPaymentAttemptCommand(
    UUID tenantId, UUID paymentId, PaymentProvider provider) {

  public CreateExternalPaymentAttemptCommand {
    requireNotNull(tenantId, "tenantId");
    requireNotNull(paymentId, "paymentId");
    requireNotNull(provider, "provider");
  }
}
