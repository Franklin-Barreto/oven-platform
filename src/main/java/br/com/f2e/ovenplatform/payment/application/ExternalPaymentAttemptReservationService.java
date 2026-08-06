package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.application.checkout.RegisterExternalCheckoutCommand;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import br.com.f2e.ovenplatform.payment.domain.PaymentProvider;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExternalPaymentAttemptReservationService {

  private static final String PAYMENT = "Payment";
  private static final String EXTERNAL_PAYMENT_ATTEMPT = "ExternalPaymentAttempt";

  private final PaymentRepository paymentRepository;
  private final ExternalPaymentAttemptRepository attemptRepository;

  public ExternalPaymentAttemptReservationService(
      PaymentRepository paymentRepository, ExternalPaymentAttemptRepository attemptRepository) {
    this.paymentRepository = paymentRepository;
    this.attemptRepository = attemptRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  protected ExternalPaymentAttempt createOrReuseInTransaction(
      UUID tenantId, UUID paymentId, PaymentProvider provider, Instant occurredAt) {

    var payment =
        paymentRepository
            .findByIdAndTenantId(paymentId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException(PAYMENT, paymentId));

    var attempts =
        attemptRepository.findByTenantIdAndPaymentId(payment.getTenantId(), payment.getId());

    expirePendingAttempt(attempts, occurredAt);

    var reusableAttempt =
        attempts.stream().filter(attempt -> attempt.isReusableAt(occurredAt)).findFirst();

    return reusableAttempt.orElseGet(
        () -> attemptRepository.saveAndFlush(payment.createExternalAttempt(provider)));
  }

  @Transactional(readOnly = true)
  protected ExternalPaymentAttempt findReusableAttempt(
      UUID tenantId, UUID paymentId, Instant occurredAt) {
    return attemptRepository.findByTenantIdAndPaymentId(tenantId, paymentId).stream()
        .filter(attempt -> attempt.isReusableAt(occurredAt))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(EXTERNAL_PAYMENT_ATTEMPT));
  }

  @Transactional
  public ExternalPaymentAttempt registerCheckoutInTransaction(
      RegisterExternalCheckoutCommand checkoutCommand) {
    var attempt =
        getExternalPaymentAttempt(checkoutCommand.tenantId(), checkoutCommand.attemptId());
    attempt.registerCheckout(
        checkoutCommand.providerReference(),
        checkoutCommand.checkoutUrl(),
        checkoutCommand.expiresAt());
    return attemptRepository.saveAndFlush(attempt);
  }

  @Transactional(readOnly = true)
  public ExternalPaymentAttempt getAttempt(UUID tenantId, UUID attemptId) {
    return getExternalPaymentAttempt(tenantId, attemptId);
  }

  @Transactional
  public void markAsFailedInTransaction(UUID tenantId, UUID attemptId, Instant occurredAt) {
    var attempt = getExternalPaymentAttempt(tenantId, attemptId);
    attempt.markAsFailed(occurredAt);
    attemptRepository.saveAndFlush(attempt);
  }

  private ExternalPaymentAttempt getExternalPaymentAttempt(UUID tenantId, UUID attemptId) {
    return attemptRepository
        .findByIdAndTenantId(attemptId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(EXTERNAL_PAYMENT_ATTEMPT, attemptId));
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
