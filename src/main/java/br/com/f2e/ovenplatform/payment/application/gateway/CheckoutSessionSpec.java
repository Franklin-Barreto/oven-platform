package br.com.f2e.ovenplatform.payment.application.gateway;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireSize;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutSessionSpec(UUID attemptId, BigDecimal amount, String currency) {

  public CheckoutSessionSpec {
    requireNotNull(attemptId, "attemptId");
    requirePositive(amount, "amount");
    currency = requireSize(currency, "currency", 3, 3);
  }
}
