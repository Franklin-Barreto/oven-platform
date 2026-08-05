package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalPaymentAttemptService {

  private static final String RESOURCE = "Payment";

  private final PaymentRepository paymentRepository;
  private final ExternalPaymentAttemptRepository attemptRepository;
  private final Clock clock;

  public ExternalPaymentAttemptService(
      PaymentRepository paymentRepository,
      ExternalPaymentAttemptRepository attemptRepository,
      Clock clock) {
    this.paymentRepository = paymentRepository;
    this.attemptRepository = attemptRepository;
    this.clock = clock;
  }

  @Transactional
  public ExternalPaymentAttemptResult createOrReuseAttempt(
      CreateExternalPaymentAttemptCommand command) {

    var occurredAt = clock.instant();

    var payment =
        paymentRepository
            .findByIdAndTenantId(command.paymentId(), command.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, command.paymentId()));

    var attempts =
        attemptRepository.findByTenantIdAndPaymentId(payment.getTenantId(), payment.getId());

    expirePendingAttempt(attempts, occurredAt);

    var reusableAttempt =
        attempts.stream().filter(attempt -> attempt.isReusableAt(occurredAt)).findFirst();

    return reusableAttempt
        .map(ExternalPaymentAttemptResult::from)
        .orElseGet(
            () ->
                ExternalPaymentAttemptResult.from(
                    attemptRepository.save(payment.createExternalAttempt(command.provider()))));
  }

  private void expirePendingAttempt(List<ExternalPaymentAttempt> attempts, Instant occurredAt) {

    attempts.stream()
        .filter(attempt -> attempt.isExpiredAt(occurredAt))
        .findFirst()
        .ifPresent(
            attempt -> {
              attempt.markAsExpired(occurredAt);
              attemptRepository.saveAndFlush(attempt);
            });
  }
}
