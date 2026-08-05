package br.com.f2e.ovenplatform.payment.infrastructure.persistence;

import br.com.f2e.ovenplatform.payment.application.ExternalPaymentAttemptRepository;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaExternalPaymentAttemptRepositoryAdapter
    implements ExternalPaymentAttemptRepository {

  private final SpringDataExternalPaymentAttemptRepository repository;

  public JpaExternalPaymentAttemptRepositoryAdapter(
      SpringDataExternalPaymentAttemptRepository repository) {
    this.repository = repository;
  }

  @Override
  public ExternalPaymentAttempt save(ExternalPaymentAttempt attempt) {
    return repository.save(attempt);
  }

  @Override
  public Optional<ExternalPaymentAttempt> findByIdAndTenantId(UUID attemptId, UUID tenantId) {
    return repository.findByIdAndTenantId(attemptId, tenantId);
  }

  @Override
  public List<ExternalPaymentAttempt> findByTenantIdAndPaymentId(UUID tenantId, UUID paymentId) {
    return repository.findByTenantIdAndPaymentIdOrderByCreatedAtDesc(tenantId, paymentId);
  }

  @Override
  public void saveAndFlush(ExternalPaymentAttempt attempt) {
    repository.saveAndFlush(attempt);
  }
}
