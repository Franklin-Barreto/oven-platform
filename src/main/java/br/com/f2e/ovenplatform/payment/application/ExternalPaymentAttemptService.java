package br.com.f2e.ovenplatform.payment.application;

import br.com.f2e.ovenplatform.payment.application.exception.ActiveAttemptAlreadyExistsException;
import java.time.Clock;
import org.springframework.stereotype.Service;

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

    } catch (ActiveAttemptAlreadyExistsException ignored) {
      return ExternalPaymentAttemptResult.from(
          reservationService.findReusableAttempt(
              command.tenantId(), command.paymentId(), occurredAt));
    }
  }
}
