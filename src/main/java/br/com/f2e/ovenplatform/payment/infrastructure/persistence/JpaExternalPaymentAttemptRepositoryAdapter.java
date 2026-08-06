package br.com.f2e.ovenplatform.payment.infrastructure.persistence;

import br.com.f2e.ovenplatform.payment.application.ExternalPaymentAttemptRepository;
import br.com.f2e.ovenplatform.payment.application.exception.ActiveAttemptAlreadyExistsException;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaExternalPaymentAttemptRepositoryAdapter
    implements ExternalPaymentAttemptRepository {

  private static final String ACTIVE_ATTEMPT_CONSTRAINT = "uk_attempts_active_payment";

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
  public ExternalPaymentAttempt saveAndFlush(ExternalPaymentAttempt attempt) {
    try {
      return repository.saveAndFlush(attempt);
    } catch (DataIntegrityViolationException exception) {
      if (hasConstraint(exception)) {
        throw new ActiveAttemptAlreadyExistsException(exception);
      }
      throw exception;
    }
  }

  private boolean hasConstraint(Throwable throwable) {
    var current = throwable;

    while (current != null) {
      if (current instanceof ConstraintViolationException constraintViolation
          && ACTIVE_ATTEMPT_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
        return true;
      }

      current = current.getCause();
    }
    return false;
  }
}
