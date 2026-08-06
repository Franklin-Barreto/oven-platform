package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalPaymentAttemptRepository {

  ExternalPaymentAttempt save(ExternalPaymentAttempt attempt);

  Optional<ExternalPaymentAttempt> findByIdAndTenantId(UUID attemptId, UUID tenantId);

  List<ExternalPaymentAttempt> findByTenantIdAndPaymentId(UUID tenantId, UUID paymentId);

  ExternalPaymentAttempt saveAndFlush(ExternalPaymentAttempt attempt);
}
