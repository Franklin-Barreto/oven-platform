package br.com.f2e.ovenplatform.payment.application.checkout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.application.CreateExternalPaymentAttemptCommand;
import br.com.f2e.ovenplatform.payment.application.ExternalPaymentAttemptResult;
import br.com.f2e.ovenplatform.payment.application.ExternalPaymentAttemptService;
import br.com.f2e.ovenplatform.payment.application.PaymentRepository;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionSpec;
import br.com.f2e.ovenplatform.payment.application.gateway.CreatedCheckoutSession;
import br.com.f2e.ovenplatform.payment.application.gateway.PaymentGateway;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus;
import br.com.f2e.ovenplatform.payment.domain.Payment;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentProcessingMode;
import br.com.f2e.ovenplatform.payment.domain.PaymentProvider;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentCheckoutSessionServiceTest {
  private static final UUID PAYMENT_ID = UUID.randomUUID();
  private static final UUID ATTEMPT_ID = UUID.randomUUID();
  private static final UUID ORDER_ID = UUID.randomUUID();
  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final Instant EXPIRES_AT = Instant.parse("2026-08-04T18:00:00Z");
  private static final URI REDIRECT_URL = URI.create("https://checkout.test/session");
  private static final Instant WINNING_EXPIRES_AT = Instant.parse("2026-08-04T17:55:00Z");
  private static final URI WINNING_REDIRECT_URL =
      URI.create("https://checkout.test/session/winner");

  @Mock private PaymentRepository paymentRepository;
  @Mock private ExternalPaymentAttemptService attemptService;
  @Mock private PaymentGateway paymentGateway;

  @InjectMocks private PaymentCheckoutSessionService paymentCheckoutSessionService;

  private Payment payment;

  @BeforeEach
  void setUp() {
    var amount = new BigDecimal("120.00");
    payment =
        Payment.pending(
            TENANT_ID, ORDER_ID, amount, PaymentMethod.CARD, PaymentProcessingMode.GATEWAY);
    EntityIdTestUtils.withId(payment, PAYMENT_ID);

    when(paymentRepository.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID))
        .thenReturn(Optional.of(payment));
  }

  @Test
  void shouldReusePendingCheckoutSessionWithoutCallingGateway() {

    var command =
        new CreateExternalPaymentAttemptCommand(TENANT_ID, PAYMENT_ID, PaymentProvider.STRIPE);
    var attempt =
        createAttempt(
            payment,
            ExternalPaymentAttemptStatus.PENDING,
            "provider_reference",
            REDIRECT_URL,
            EXPIRES_AT);

    when(attemptService.createOrReuseAttempt(command)).thenReturn(attempt);

    var result = paymentCheckoutSessionService.createOrReuseCheckoutSession(TENANT_ID, ORDER_ID);

    assertThat(result.checkoutUrl()).isEqualTo(attempt.redirectUrl());
    assertThat(result.expiresAt()).isEqualTo(attempt.expiresAt());
    assertThat(result.attemptId()).isEqualTo(attempt.attemptId());
    verifyNoInteractions(paymentGateway);
  }

  @Test
  void shouldCreateAndRegisterCheckoutForCreatedAttempt() {

    var command =
        new CreateExternalPaymentAttemptCommand(TENANT_ID, PAYMENT_ID, PaymentProvider.STRIPE);
    var attempt = createAttempt(payment, ExternalPaymentAttemptStatus.CREATED, null, null, null);

    var spec = new CheckoutSessionSpec(attempt.attemptId(), attempt.amount(), attempt.currency());
    var createdCheckoutSession =
        new CreatedCheckoutSession("provider_reference", REDIRECT_URL, EXPIRES_AT);

    var registeredAttempt =
        createAttempt(
            payment,
            ExternalPaymentAttemptStatus.PENDING,
            createdCheckoutSession.providerReference(),
            WINNING_REDIRECT_URL,
            WINNING_EXPIRES_AT);

    when(attemptService.createOrReuseAttempt(command)).thenReturn(attempt);
    when(paymentGateway.createCheckoutSession(spec)).thenReturn(createdCheckoutSession);
    var registerCommand =
        new RegisterExternalCheckoutCommand(
            TENANT_ID,
            ATTEMPT_ID,
            createdCheckoutSession.providerReference(),
            createdCheckoutSession.checkoutUrl(),
            createdCheckoutSession.expiresAt());
    when(attemptService.registerCheckout(registerCommand)).thenReturn(registeredAttempt);

    var result = paymentCheckoutSessionService.createOrReuseCheckoutSession(TENANT_ID, ORDER_ID);

    assertThat(result.checkoutUrl()).isEqualTo(registeredAttempt.redirectUrl());
    assertThat(result.expiresAt()).isEqualTo(registeredAttempt.expiresAt());
    assertThat(result.attemptId()).isEqualTo(registeredAttempt.attemptId());
    verify(paymentGateway).createCheckoutSession(spec);
    verify(attemptService).registerCheckout(registerCommand);
  }

  @ParameterizedTest
  @MethodSource("invalidStatuses")
  void shouldThrowExceptionWhenStatusIsInvalid(
      ExternalPaymentAttemptStatus status, String message) {

    var command =
        new CreateExternalPaymentAttemptCommand(TENANT_ID, PAYMENT_ID, PaymentProvider.STRIPE);
    var attempt = createAttempt(payment, status, "provider_reference", REDIRECT_URL, EXPIRES_AT);

    when(attemptService.createOrReuseAttempt(command)).thenReturn(attempt);

    assertThatThrownBy(
            () -> paymentCheckoutSessionService.createOrReuseCheckoutSession(TENANT_ID, ORDER_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(message);

    verifyNoInteractions(paymentGateway);
    verify(attemptService, never()).registerCheckout(any());
  }

  private static Stream<Arguments> invalidStatuses() {
    return Stream.of(
        Arguments.of(
            ExternalPaymentAttemptStatus.SUCCEEDED,
            "Unexpected external payment attempt status for checkout creation: SUCCEEDED"),
        Arguments.of(
            ExternalPaymentAttemptStatus.FAILED,
            "Unexpected external payment attempt status for checkout creation: FAILED"),
        Arguments.of(
            ExternalPaymentAttemptStatus.EXPIRED,
            "Unexpected external payment attempt status for checkout creation: EXPIRED"));
  }

  private static ExternalPaymentAttemptResult createAttempt(
      Payment payment,
      ExternalPaymentAttemptStatus status,
      String providerReference,
      URI redirectUrl,
      Instant expiresAt) {
    return new ExternalPaymentAttemptResult(
        ATTEMPT_ID,
        payment.getId(),
        payment.getAmount(),
        "BRL",
        PaymentProvider.STRIPE,
        status,
        providerReference,
        redirectUrl,
        expiresAt);
  }
}
