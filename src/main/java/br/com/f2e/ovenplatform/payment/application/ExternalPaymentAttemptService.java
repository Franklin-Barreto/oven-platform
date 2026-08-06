package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.application.checkout.RegisterExternalCheckoutCommand;
import br.com.f2e.ovenplatform.payment.application.exception.ActiveAttemptAlreadyExistsException;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class ExternalPaymentAttemptService {

  private final ExternalPaymentAttemptReservationService reservationService;
  private final Clock clock;

  public ExternalPaymentAttemptService(
      ExternalPaymentAttemptReservationService reservationService, Clock clock) {
    this.reservationService = reservationService;
    this.clock = clock;
  }

  public ExternalPaymentAttemptResult createOrReuseAttempt(
      CreateExternalPaymentAttemptCommand command) {

    var occurredAt = clock.instant();

    try {
      return ExternalPaymentAttemptResult.from(
          reservationService.createOrReuseInTransaction(
              command.tenantId(), command.paymentId(), command.provider(), occurredAt));

    } catch (ActiveAttemptAlreadyExistsException _) {
      return ExternalPaymentAttemptResult.from(
          reservationService.findReusableAttempt(
              command.tenantId(), command.paymentId(), occurredAt));
    }
  }

  public ExternalPaymentAttemptResult registerCheckout(RegisterExternalCheckoutCommand command) {
    try {
      return ExternalPaymentAttemptResult.from(
          reservationService.registerCheckoutInTransaction(command));
    } catch (OptimisticLockingFailureException _) {
      var attempt = reservationService.getAttempt(command.tenantId(), command.attemptId());

      if (attempt.getStatus() != ExternalPaymentAttemptStatus.PENDING) {
        throw new IllegalStateException(
            "Recovered external payment attempt must be PENDING, but was %s."
                .formatted(attempt.getStatus()));
      }

      return ExternalPaymentAttemptResult.from(attempt);
    }
  }
}
