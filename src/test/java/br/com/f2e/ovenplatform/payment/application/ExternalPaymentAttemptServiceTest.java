package br.com.f2e.ovenplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.application.checkout.RegisterExternalCheckoutCommand;
import br.com.f2e.ovenplatform.payment.application.exception.ActiveAttemptAlreadyExistsException;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import br.com.f2e.ovenplatform.payment.domain.PaymentProvider;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class ExternalPaymentAttemptServiceTest {

  private static final Instant EXPIRES_AT = Instant.parse("2026-08-04T18:00:00Z");

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PAYMENT_ID = UUID.fromString("b6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ATTEMPT_ID = UUID.fromString("c6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("120.00");
  private static final URI CHECKOUT_URL = URI.create("https://checkout.test/session");

  @Mock private ExternalPaymentAttemptReservationService reservationService;
  @Mock private Clock clock;

  @InjectMocks private ExternalPaymentAttemptService service;
  private ExternalPaymentAttempt attempt;
  private CreateExternalPaymentAttemptCommand command;

  @BeforeEach
  void setUp() {
    attempt =
        ExternalPaymentAttempt.createAttempt(
            TENANT_ID, PAYMENT_ID, PaymentProvider.STRIPE, PAYMENT_AMOUNT);
    EntityIdTestUtils.withId(attempt, ATTEMPT_ID);
    command =
        new CreateExternalPaymentAttemptCommand(TENANT_ID, PAYMENT_ID, PaymentProvider.STRIPE);
  }

  @Test
  void shouldReturnWinningAttemptWhenReservationConflicts() {

    when(clock.instant()).thenReturn(EXPIRES_AT);

    when(reservationService.createOrReuseInTransaction(
            TENANT_ID, PAYMENT_ID, PaymentProvider.STRIPE, EXPIRES_AT))
        .thenThrow(new ActiveAttemptAlreadyExistsException());
    when(reservationService.findReusableAttempt(TENANT_ID, PAYMENT_ID, EXPIRES_AT))
        .thenReturn(attempt);

    var result = service.createOrReuseAttempt(command);

    assertThat(result).isEqualTo(ExternalPaymentAttemptResult.from(attempt));
    verify(reservationService).findReusableAttempt(TENANT_ID, PAYMENT_ID, EXPIRES_AT);
  }

  @Test
  void shouldReturnReservedAttemptWithoutRecovery() {

    when(clock.instant()).thenReturn(EXPIRES_AT);

    when(reservationService.createOrReuseInTransaction(
            TENANT_ID, PAYMENT_ID, PaymentProvider.STRIPE, EXPIRES_AT))
        .thenReturn(attempt);

    var result = service.createOrReuseAttempt(command);

    assertThat(result).isEqualTo(ExternalPaymentAttemptResult.from(attempt));
    verify(reservationService, never()).findReusableAttempt(any(), any(), any());
  }

  @Test
  void shouldReturnWinningCheckoutWhenRegistrationConflicts() {
    var registerExternalCommand =
        new RegisterExternalCheckoutCommand(
            TENANT_ID, ATTEMPT_ID, "provider-reference", CHECKOUT_URL, EXPIRES_AT);

    when(reservationService.registerCheckoutInTransaction(registerExternalCommand))
        .thenThrow(
            new OptimisticLockingFailureException(
                "Failed", new IllegalStateException("Simulated persistence conflict")));

    attempt.registerCheckout(
        registerExternalCommand.providerReference(),
        registerExternalCommand.checkoutUrl(),
        registerExternalCommand.expiresAt());

    when(reservationService.getAttempt(TENANT_ID, ATTEMPT_ID)).thenReturn(attempt);

    var result = service.registerCheckout(registerExternalCommand);
    assertThat(result).isEqualTo(ExternalPaymentAttemptResult.from(attempt));
    verify(reservationService).getAttempt(TENANT_ID, ATTEMPT_ID);
  }

  @Test
  void shouldRejectRecoveredAttemptWithoutRegisteredCheckout() {
    var registerExternalCommand =
        new RegisterExternalCheckoutCommand(
            TENANT_ID, ATTEMPT_ID, "provider-reference", CHECKOUT_URL, EXPIRES_AT);

    when(reservationService.registerCheckoutInTransaction(registerExternalCommand))
        .thenThrow(
            new OptimisticLockingFailureException(
                "Failed", new IllegalStateException("Simulated persistence conflict")));
    when(reservationService.getAttempt(TENANT_ID, ATTEMPT_ID)).thenReturn(attempt);

    assertThatThrownBy(() -> service.registerCheckout(registerExternalCommand))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Recovered external payment attempt must be PENDING, but was CREATED.");
  }
}
