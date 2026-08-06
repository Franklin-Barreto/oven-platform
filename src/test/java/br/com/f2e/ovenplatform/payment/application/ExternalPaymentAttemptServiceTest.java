package br.com.f2e.ovenplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.application.exception.ActiveAttemptAlreadyExistsException;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import br.com.f2e.ovenplatform.payment.domain.PaymentProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExternalPaymentAttemptServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-04T18:00:00Z");

  @Mock private ExternalPaymentAttemptReservationService reservationService;
  @Mock private Clock clock;

  @InjectMocks private ExternalPaymentAttemptService service;

  @Test
  void shouldReturnWinningAttemptWhenReservationConflicts() {
    var tenantId = UUID.randomUUID();
    var paymentId = UUID.randomUUID();
    var attempt =
        ExternalPaymentAttempt.createAttempt(
            tenantId, paymentId, PaymentProvider.STRIPE, new BigDecimal("120.00"));
    var command =
        new CreateExternalPaymentAttemptCommand(tenantId, paymentId, PaymentProvider.STRIPE);

    when(clock.instant()).thenReturn(NOW);
    when(reservationService.createOrReuseInTransaction(
            tenantId, paymentId, PaymentProvider.STRIPE, NOW))
        .thenThrow(new ActiveAttemptAlreadyExistsException());
    when(reservationService.findReusableAttempt(tenantId, paymentId, NOW)).thenReturn(attempt);

    var result = service.createOrReuseAttempt(command);

    assertThat(result).isEqualTo(ExternalPaymentAttemptResult.from(attempt));
    verify(reservationService).findReusableAttempt(tenantId, paymentId, NOW);
  }

  @Test
  void shouldReturnReservedAttemptWithoutRecovery() {
    var tenantId = UUID.randomUUID();
    var paymentId = UUID.randomUUID();
    var attempt =
        ExternalPaymentAttempt.createAttempt(
            tenantId, paymentId, PaymentProvider.STRIPE, new BigDecimal("120.00"));
    var command =
        new CreateExternalPaymentAttemptCommand(tenantId, paymentId, PaymentProvider.STRIPE);

    when(clock.instant()).thenReturn(NOW);
    when(reservationService.createOrReuseInTransaction(
            tenantId, paymentId, PaymentProvider.STRIPE, NOW))
        .thenReturn(attempt);

    var result = service.createOrReuseAttempt(command);

    assertThat(result).isEqualTo(ExternalPaymentAttemptResult.from(attempt));
    verify(reservationService, never()).findReusableAttempt(any(), any(), any());
  }
}
