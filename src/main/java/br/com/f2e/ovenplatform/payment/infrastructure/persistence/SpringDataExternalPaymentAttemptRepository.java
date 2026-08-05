package br.com.f2e.ovenplatform.payment.infrastructure.persistence;

import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataExternalPaymentAttemptRepository
    extends JpaRepository<ExternalPaymentAttempt, UUID> {

  Optional<ExternalPaymentAttempt> findByIdAndTenantId(UUID attemptId, UUID tenantId);

  List<ExternalPaymentAttempt> findByTenantIdAndPaymentIdOrderByCreatedAtDesc(
      UUID tenantId, UUID paymentId);
}
