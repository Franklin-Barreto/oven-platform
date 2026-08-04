package br.com.f2e.ovenplatform.payment.domain;

import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.CREATED;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.EXPIRED;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.FAILED;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.PENDING;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.SUCCEEDED;
import static br.com.f2e.ovenplatform.payment.domain.PaymentProvider.STRIPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.payment.domain.exception.InvalidExternalPaymentAttemptStatusTransitionException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ExternalPaymentAttemptTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");

  private static final UUID PAYMENT_ID = UUID.fromString("b6210129-f1d5-4942-8d0a-b144e518aecc");

  private static final BigDecimal AMOUNT = new BigDecimal("80.00");

  private static final String PROVIDER_REFERENCE = "cs_test_a11YYufWQzNY63zpQ6QSNRQhkUpVph4W";

  private static final URI REDIRECT_URL = URI.create("https://checkout.stripe.com/c/pay/test");

  private static final Instant EXPIRES_AT = Instant.parse("2026-08-05T12:00:00Z");

  private static final Instant COMPLETED_AT = Instant.parse("2026-08-04T18:00:00Z");

  @Test
  void shouldCreateAttemptBeforeCallingGateway() {
    var attempt = createAttempt();

    assertThat(attempt.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(attempt.getPaymentId()).isEqualTo(PAYMENT_ID);
    assertThat(attempt.getProvider()).isEqualTo(STRIPE);
    assertThat(attempt.getAmount()).isEqualByComparingTo(AMOUNT);
    assertThat(attempt.getCurrency()).isEqualTo("BRL");
    assertThat(attempt.getStatus()).isEqualTo(CREATED);
    assertThat(attempt.getProviderReference()).isNull();
    assertThat(attempt.getRedirectUrl()).isNull();
    assertThat(attempt.getExpiresAt()).isNull();
    assertThat(attempt.getCompletedAt()).isNull();
  }

  @ParameterizedTest
  @MethodSource("invalidCreationArguments")
  void shouldRejectInvalidAttemptCreation(
      UUID tenantId,
      UUID paymentId,
      PaymentProvider provider,
      BigDecimal amount,
      String expectedMessage) {

    assertThatThrownBy(
            () -> ExternalPaymentAttempt.createAttempt(tenantId, paymentId, provider, amount))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldRegisterCreatedCheckoutAsPending() {
    var attempt = createAttempt();

    var changed = attempt.registerCheckout(PROVIDER_REFERENCE, REDIRECT_URL, EXPIRES_AT);

    assertThat(changed).isTrue();
    assertThat(attempt.getStatus()).isEqualTo(PENDING);
    assertThat(attempt.getProviderReference()).isEqualTo(PROVIDER_REFERENCE);
    assertThat(attempt.getRedirectUrl()).isEqualTo(REDIRECT_URL);
    assertThat(attempt.getExpiresAt()).isEqualTo(EXPIRES_AT);
    assertThat(attempt.getCompletedAt()).isNull();
  }

  @ParameterizedTest
  @MethodSource("invalidCheckoutArguments")
  void shouldRejectInvalidCheckoutRegistration(
      String providerReference, URI redirectUrl, Instant expiresAt, String expectedMessage) {

    var attempt = createAttempt();

    assertThatThrownBy(() -> attempt.registerCheckout(providerReference, redirectUrl, expiresAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);

    assertThat(attempt.getStatus()).isEqualTo(CREATED);
    assertThat(attempt.getProviderReference()).isNull();
    assertThat(attempt.getRedirectUrl()).isNull();
    assertThat(attempt.getExpiresAt()).isNull();
  }

  @Test
  void shouldKeepFirstCheckoutWhenRegistrationIsRepeated() {
    var attempt = pendingAttempt();
    var anotherReference = "cs_test_another";
    var anotherRedirectUrl = URI.create("https://checkout.stripe.com/c/pay/another");
    var anotherExpiration = Instant.parse("2026-08-06T12:00:00Z");

    var changed = attempt.registerCheckout(anotherReference, anotherRedirectUrl, anotherExpiration);

    assertThat(changed).isFalse();
    assertThat(attempt.getStatus()).isEqualTo(PENDING);
    assertThat(attempt.getProviderReference()).isEqualTo(PROVIDER_REFERENCE);
    assertThat(attempt.getRedirectUrl()).isEqualTo(REDIRECT_URL);
    assertThat(attempt.getExpiresAt()).isEqualTo(EXPIRES_AT);
  }

  @Test
  void shouldMarkPendingAttemptAsSucceeded() {
    var attempt = pendingAttempt();

    var changed = attempt.markAsSucceeded(COMPLETED_AT);

    assertThat(changed).isTrue();
    assertThat(attempt.getStatus()).isEqualTo(SUCCEEDED);
    assertThat(attempt.getCompletedAt()).isEqualTo(COMPLETED_AT);
  }

  @Test
  void shouldKeepFirstCompletionWhenSuccessIsRepeated() {
    var attempt = pendingAttempt();
    attempt.markAsSucceeded(COMPLETED_AT);
    var repeatedCompletion = Instant.parse("2026-08-04T19:00:00Z");

    var changed = attempt.markAsSucceeded(repeatedCompletion);

    assertThat(changed).isFalse();
    assertThat(attempt.getStatus()).isEqualTo(SUCCEEDED);
    assertThat(attempt.getCompletedAt()).isEqualTo(COMPLETED_AT);
  }

  @Test
  void shouldMarkCreatedAttemptAsFailedWhenGatewayCallFails() {
    var attempt = createAttempt();

    var changed = attempt.markAsFailed(COMPLETED_AT);

    assertThat(changed).isTrue();
    assertThat(attempt.getStatus()).isEqualTo(FAILED);
    assertThat(attempt.getCompletedAt()).isEqualTo(COMPLETED_AT);
  }

  @Test
  void shouldMarkPendingAttemptAsFailed() {
    var attempt = pendingAttempt();

    var changed = attempt.markAsFailed(COMPLETED_AT);

    assertThat(changed).isTrue();
    assertThat(attempt.getStatus()).isEqualTo(FAILED);
    assertThat(attempt.getCompletedAt()).isEqualTo(COMPLETED_AT);
  }

  @Test
  void shouldMarkPendingAttemptAsExpired() {
    var attempt = pendingAttempt();

    var changed = attempt.markAsExpired(COMPLETED_AT);

    assertThat(changed).isTrue();
    assertThat(attempt.getStatus()).isEqualTo(EXPIRED);
    assertThat(attempt.getCompletedAt()).isEqualTo(COMPLETED_AT);
  }

  @Test
  void shouldRejectSuccessBeforeCheckoutRegistration() {
    var attempt = createAttempt();

    assertThatThrownBy(() -> attempt.markAsSucceeded(COMPLETED_AT))
        .isInstanceOf(InvalidExternalPaymentAttemptStatusTransitionException.class)
        .hasMessage("Cannot transition external payment attempt from CREATED to SUCCEEDED.");

    assertThat(attempt.getStatus()).isEqualTo(CREATED);
    assertThat(attempt.getCompletedAt()).isNull();
  }

  @Test
  void shouldRejectExpirationBeforeCheckoutRegistration() {
    var attempt = createAttempt();

    assertThatThrownBy(() -> attempt.markAsExpired(COMPLETED_AT))
        .isInstanceOf(InvalidExternalPaymentAttemptStatusTransitionException.class)
        .hasMessage("Cannot transition external payment attempt from CREATED to EXPIRED.");

    assertThat(attempt.getStatus()).isEqualTo(CREATED);
    assertThat(attempt.getCompletedAt()).isNull();
  }

  @Test
  void shouldRejectTransitionFromSuccessfulAttempt() {
    var attempt = pendingAttempt();
    attempt.markAsSucceeded(COMPLETED_AT);
    var failureTime = Instant.parse("2026-08-04T19:00:00Z");

    assertThatThrownBy(() -> attempt.markAsFailed(failureTime))
        .isInstanceOf(InvalidExternalPaymentAttemptStatusTransitionException.class)
        .hasMessage("Cannot transition external payment attempt from SUCCEEDED to FAILED.");

    assertThat(attempt.getStatus()).isEqualTo(SUCCEEDED);
    assertThat(attempt.getCompletedAt()).isEqualTo(COMPLETED_AT);
  }

  @Test
  void shouldRejectTransitionFromFailedAttempt() {
    var attempt = pendingAttempt();
    attempt.markAsFailed(COMPLETED_AT);

    assertThatThrownBy(() -> attempt.registerCheckout(PROVIDER_REFERENCE, REDIRECT_URL, EXPIRES_AT))
        .isInstanceOf(InvalidExternalPaymentAttemptStatusTransitionException.class)
        .hasMessage("Cannot transition external payment attempt from FAILED to PENDING.");

    assertThat(attempt.getStatus()).isEqualTo(FAILED);
    assertThat(attempt.getCompletedAt()).isEqualTo(COMPLETED_AT);
  }

  @Test
  void shouldRejectTransitionFromExpiredAttempt() {
    var attempt = pendingAttempt();
    attempt.markAsExpired(COMPLETED_AT);

    assertThatThrownBy(() -> attempt.markAsSucceeded(COMPLETED_AT))
        .isInstanceOf(InvalidExternalPaymentAttemptStatusTransitionException.class)
        .hasMessage("Cannot transition external payment attempt from EXPIRED to SUCCEEDED.");

    assertThat(attempt.getStatus()).isEqualTo(EXPIRED);
    assertThat(attempt.getCompletedAt()).isEqualTo(COMPLETED_AT);
  }

  @ParameterizedTest
  @MethodSource("terminalTransitionsWithoutCompletionTime")
  void shouldRejectTerminalTransitionWithoutCompletionTime(
      Consumer<ExternalPaymentAttempt> transition) {

    var attempt = pendingAttempt();

    assertThatThrownBy(() -> transition.accept(attempt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("completedAt must not be null");

    assertThat(attempt.getStatus()).isEqualTo(PENDING);
    assertThat(attempt.getCompletedAt()).isNull();
  }

  private static ExternalPaymentAttempt createAttempt() {
    return ExternalPaymentAttempt.createAttempt(TENANT_ID, PAYMENT_ID, STRIPE, AMOUNT);
  }

  private static ExternalPaymentAttempt pendingAttempt() {
    var attempt = createAttempt();

    attempt.registerCheckout(PROVIDER_REFERENCE, REDIRECT_URL, EXPIRES_AT);

    return attempt;
  }

  private static Stream<Arguments> invalidCreationArguments() {
    return Stream.of(
        Arguments.of(null, PAYMENT_ID, STRIPE, AMOUNT, "tenantId must not be null"),
        Arguments.of(TENANT_ID, null, STRIPE, AMOUNT, "paymentId must not be null"),
        Arguments.of(TENANT_ID, PAYMENT_ID, null, AMOUNT, "provider must not be null"),
        Arguments.of(TENANT_ID, PAYMENT_ID, STRIPE, null, "amount must not be null"),
        Arguments.of(
            TENANT_ID, PAYMENT_ID, STRIPE, BigDecimal.ZERO, "amount must be greater than zero"),
        Arguments.of(
            TENANT_ID,
            PAYMENT_ID,
            STRIPE,
            new BigDecimal("-1.00"),
            "amount must be greater than zero"));
  }

  private static Stream<Arguments> invalidCheckoutArguments() {
    return Stream.of(
        Arguments.of(null, REDIRECT_URL, EXPIRES_AT, "providerReference must not be null"),
        Arguments.of(" ", REDIRECT_URL, EXPIRES_AT, "providerReference must not be blank"),
        Arguments.of(PROVIDER_REFERENCE, null, EXPIRES_AT, "redirectUrl must not be null"),
        Arguments.of(PROVIDER_REFERENCE, REDIRECT_URL, null, "expiresAt must not be null"));
  }

  private static Stream<Consumer<ExternalPaymentAttempt>>
      terminalTransitionsWithoutCompletionTime() {

    return Stream.of(
        attempt -> attempt.markAsSucceeded(null),
        attempt -> attempt.markAsFailed(null),
        attempt -> attempt.markAsExpired(null));
  }
}
