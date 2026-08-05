package br.com.f2e.ovenplatform.payment.application;

import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.CREATED;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.EXPIRED;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.FAILED;
import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.PENDING;
import static br.com.f2e.ovenplatform.payment.domain.PaymentProvider.STRIPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttempt;
import br.com.f2e.ovenplatform.payment.domain.Payment;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentProcessingMode;
import br.com.f2e.ovenplatform.payment.infrastructure.persistence.JpaExternalPaymentAttemptRepositoryAdapter;
import br.com.f2e.ovenplatform.payment.infrastructure.persistence.JpaPaymentRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({
  ExternalPaymentAttemptService.class,
  JpaExternalPaymentAttemptRepositoryAdapter.class,
  JpaPaymentRepositoryAdapter.class
})
class ExternalPaymentAttemptServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID OTHER_TENANT_ID =
      UUID.fromString("b6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("c6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("120.00");
  private static final Instant NOW = Instant.parse("2026-08-04T18:00:00Z");
  private static final URI REDIRECT_URL = URI.create("https://checkout.test/session");

  @Autowired private ExternalPaymentAttemptService service;
  @Autowired private JpaExternalPaymentAttemptRepositoryAdapter attemptRepository;
  @Autowired private JpaPaymentRepositoryAdapter paymentRepository;

  @MockitoBean private Clock clock;

  @BeforeEach
  void setUpClock() {
    when(clock.instant()).thenReturn(NOW);
  }

  @Test
  void shouldCreateAndPersistAttemptForEligiblePayment() {
    var payment = persistPendingGatewayPayment();

    var result = service.createOrReuseAttempt(command(payment.getId()));

    flushAndClear();

    var persistedAttempt =
        attemptRepository.findByIdAndTenantId(result.attemptId(), TENANT_ID).orElseThrow();

    assertThat(result.paymentId()).isEqualTo(payment.getId());
    assertThat(result.provider()).isEqualTo(STRIPE);
    assertThat(result.status()).isEqualTo(CREATED);
    assertThat(persistedAttempt.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(persistedAttempt.getPaymentId()).isEqualTo(payment.getId());
    assertThat(persistedAttempt.getAmount()).isEqualByComparingTo(PAYMENT_AMOUNT);
    assertThat(persistedAttempt.getCurrency()).isEqualTo("BRL");
    assertThat(persistedAttempt.getProviderReference()).isNull();
    assertThat(persistedAttempt.getRedirectUrl()).isNull();
    assertThat(persistedAttempt.getExpiresAt()).isNull();
  }

  @Test
  void shouldReuseCreatedAttempt() {
    var payment = persistPendingGatewayPayment();

    var first = service.createOrReuseAttempt(command(payment.getId()));
    var second = service.createOrReuseAttempt(command(payment.getId()));

    flushAndClear();

    assertThat(second.attemptId()).isEqualTo(first.attemptId());
    assertThat(attemptRepository.findByTenantIdAndPaymentId(TENANT_ID, payment.getId())).hasSize(1);
  }

  @Test
  void shouldReusePendingAttemptThatHasNotExpired() {
    var payment = persistPendingGatewayPayment();
    var created = service.createOrReuseAttempt(command(payment.getId()));
    var attempt =
        attemptRepository.findByIdAndTenantId(created.attemptId(), TENANT_ID).orElseThrow();
    attempt.registerCheckout("checkout-reference", REDIRECT_URL, NOW.plusSeconds(300));
    flushAndClear();

    var result = service.createOrReuseAttempt(command(payment.getId()));

    assertThat(result.attemptId()).isEqualTo(created.attemptId());
    assertThat(result.status()).isEqualTo(PENDING);
    assertThat(attemptRepository.findByTenantIdAndPaymentId(TENANT_ID, payment.getId())).hasSize(1);
  }

  @Test
  void shouldExpirePendingAttemptAndCreateAnotherAttempt() {
    var payment = persistPendingGatewayPayment();
    var created = service.createOrReuseAttempt(command(payment.getId()));
    var attempt =
        attemptRepository.findByIdAndTenantId(created.attemptId(), TENANT_ID).orElseThrow();
    attempt.registerCheckout("expired-checkout-reference", REDIRECT_URL, NOW.minusSeconds(1));
    flushAndClear();

    var result = service.createOrReuseAttempt(command(payment.getId()));

    flushAndClear();

    var attempts = attemptRepository.findByTenantIdAndPaymentId(TENANT_ID, payment.getId());

    assertThat(result.attemptId()).isNotEqualTo(created.attemptId());
    assertThat(result.status()).isEqualTo(CREATED);
    assertThat(attempts).hasSize(2);
    assertThat(attempts)
        .filteredOn(candidate -> candidate.getId().equals(created.attemptId()))
        .singleElement()
        .satisfies(
            expired -> {
              assertThat(expired.getStatus()).isEqualTo(EXPIRED);
              assertThat(expired.getCompletedAt()).isEqualTo(NOW);
            });
  }

  @Test
  void shouldRejectPaidPayment() {
    var payment =
        paymentRepository.save(
            Payment.paid(
                TENANT_ID,
                ORDER_ID,
                PAYMENT_AMOUNT,
                PaymentMethod.CARD,
                PaymentProcessingMode.GATEWAY,
                NOW));
    var attempt = command(payment.getId());

    assertThatThrownBy(() -> service.createOrReuseAttempt(attempt))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Only pending payments can create external payment attempts.");
  }

  @Test
  void shouldRejectManualPayment() {
    var payment =
        paymentRepository.save(
            Payment.pending(
                TENANT_ID,
                ORDER_ID,
                PAYMENT_AMOUNT,
                PaymentMethod.CARD,
                PaymentProcessingMode.MANUAL));

    var attempt = command(payment.getId());

    assertThatThrownBy(() -> service.createOrReuseAttempt(attempt))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Only gateway-processed payments can create external payment attempts.");
  }

  @Test
  void shouldPreventMoreThanOneActiveAttemptForPayment() {
    var payment = persistPendingGatewayPayment();

    attemptRepository.saveAndFlush(payment.createExternalAttempt(STRIPE));

    var anotherActiveAttempt = payment.createExternalAttempt(STRIPE);

    assertThatThrownBy(() -> attemptRepository.saveAndFlush(anotherActiveAttempt))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldAllowNewAttemptAfterPreviousAttemptBecomesTerminal() {
    var payment = persistPendingGatewayPayment();
    var previousAttempt = payment.createExternalAttempt(STRIPE);
    previousAttempt.markAsFailed(NOW);
    attemptRepository.saveAndFlush(previousAttempt);

    var newAttempt = payment.createExternalAttempt(STRIPE);
    attemptRepository.saveAndFlush(newAttempt);

    flushAndClear();

    assertThat(attemptRepository.findByTenantIdAndPaymentId(TENANT_ID, payment.getId()))
        .hasSize(2)
        .extracting(ExternalPaymentAttempt::getStatus)
        .containsExactlyInAnyOrder(FAILED, CREATED);
  }

  @Test
  void shouldNotFindPaymentFromAnotherTenant() {
    var payment = persistPendingGatewayPayment();
    var attempt = new CreateExternalPaymentAttemptCommand(OTHER_TENANT_ID, payment.getId(), STRIPE);

    assertThatThrownBy(() -> service.createOrReuseAttempt(attempt))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Payment id: %s not found".formatted(payment.getId()));
  }

  private Payment persistPendingGatewayPayment() {
    return paymentRepository.save(
        Payment.pending(
            TENANT_ID,
            ORDER_ID,
            PAYMENT_AMOUNT,
            PaymentMethod.CARD,
            PaymentProcessingMode.GATEWAY));
  }

  private CreateExternalPaymentAttemptCommand command(UUID paymentId) {
    return new CreateExternalPaymentAttemptCommand(TENANT_ID, paymentId, STRIPE);
  }
}
