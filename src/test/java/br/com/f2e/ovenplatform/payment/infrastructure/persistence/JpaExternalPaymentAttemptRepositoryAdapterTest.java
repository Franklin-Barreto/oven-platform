package br.com.f2e.ovenplatform.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.application.exception.ActiveAttemptAlreadyExistsException;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import br.com.f2e.ovenplatform.payment.domain.PaymentProvider;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class JpaExternalPaymentAttemptRepositoryAdapterTest {

  private static final String ACTIVE_ATTEMPT_CONSTRAINT = "uk_attempts_active_payment";

  @Mock private SpringDataExternalPaymentAttemptRepository repository;

  @InjectMocks private JpaExternalPaymentAttemptRepositoryAdapter adapter;

  @Test
  void shouldTranslateActiveAttemptConstraintViolation() {
    var attempt = attempt();
    var persistenceException = constraintViolation(ACTIVE_ATTEMPT_CONSTRAINT);

    when(repository.saveAndFlush(attempt)).thenThrow(persistenceException);

    assertThatThrownBy(() -> adapter.saveAndFlush(attempt))
        .isInstanceOf(ActiveAttemptAlreadyExistsException.class)
        .hasCause(persistenceException);
  }

  @Test
  void shouldTranslateNestedActiveAttemptConstraintViolation() {
    var attempt = attempt();
    var constraintViolation =
        new ConstraintViolationException("Constraint violation", null, ACTIVE_ATTEMPT_CONSTRAINT);
    var persistenceException =
        new DataIntegrityViolationException(
            "Nested data integrity violation",
            new RuntimeException("Wrapper", constraintViolation));

    when(repository.saveAndFlush(attempt)).thenThrow(persistenceException);

    assertThatThrownBy(() -> adapter.saveAndFlush(attempt))
        .isInstanceOf(ActiveAttemptAlreadyExistsException.class)
        .hasCause(persistenceException);
  }

  @Test
  void shouldRethrowDifferentConstraintViolation() {
    var attempt = attempt();
    var unexpectedException = constraintViolation("uk_attempts_provider_reference");

    when(repository.saveAndFlush(attempt)).thenThrow(unexpectedException);

    assertThatThrownBy(() -> adapter.saveAndFlush(attempt)).isSameAs(unexpectedException);
  }

  @Test
  void shouldRethrowDataIntegrityViolationWithoutConstraint() {
    var attempt = attempt();
    var unexpectedException =
        new DataIntegrityViolationException("Unexpected data integrity violation");

    when(repository.saveAndFlush(attempt)).thenThrow(unexpectedException);

    assertThatThrownBy(() -> adapter.saveAndFlush(attempt)).isSameAs(unexpectedException);
  }

  private ExternalPaymentAttempt attempt() {
    return ExternalPaymentAttempt.createAttempt(
        UUID.randomUUID(), UUID.randomUUID(), PaymentProvider.STRIPE, new BigDecimal("120.00"));
  }

  private DataIntegrityViolationException constraintViolation(String constraintName) {
    return new DataIntegrityViolationException(
        "Data integrity violation",
        new ConstraintViolationException("Constraint violation", null, constraintName));
  }
}
