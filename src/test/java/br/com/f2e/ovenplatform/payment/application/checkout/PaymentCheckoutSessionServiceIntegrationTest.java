package br.com.f2e.ovenplatform.payment.application.checkout;

import static br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.application.ExternalPaymentAttemptReservationService;
import br.com.f2e.ovenplatform.payment.application.ExternalPaymentAttemptService;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionSpec;
import br.com.f2e.ovenplatform.payment.application.gateway.CreatedCheckoutSession;
import br.com.f2e.ovenplatform.payment.application.gateway.PaymentGateway;
import br.com.f2e.ovenplatform.payment.domain.Payment;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentProcessingMode;
import br.com.f2e.ovenplatform.payment.infrastructure.persistence.JpaExternalPaymentAttemptRepositoryAdapter;
import br.com.f2e.ovenplatform.payment.infrastructure.persistence.JpaPaymentRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Import({
  PaymentCheckoutSessionService.class,
  ExternalPaymentAttemptService.class,
  ExternalPaymentAttemptReservationService.class,
  JpaExternalPaymentAttemptRepositoryAdapter.class,
  JpaPaymentRepositoryAdapter.class,
  PaymentCheckoutSessionServiceIntegrationTest.CheckoutTestConfiguration.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentCheckoutSessionServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("e6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final BigDecimal AMOUNT = new BigDecimal("120.00");
  private static final Instant NOW = Instant.parse("2026-08-07T12:00:00Z");
  private static final URI SHARED_URL = URI.create("https://checkout.test/shared");
  private static final Instant SHARED_EXPIRES_AT = NOW.plusSeconds(3600);

  @Autowired private PaymentCheckoutSessionService service;
  @Autowired private JpaExternalPaymentAttemptRepositoryAdapter attemptRepository;
  @Autowired private JpaPaymentRepositoryAdapter paymentRepository;
  @Autowired private SynchronizingPaymentGateway gateway;

  @MockitoBean private Clock clock;

  @Test
  void shouldReturnSameCheckoutSessionUnderConcurrency()
      throws ExecutionException, InterruptedException, TimeoutException {
    var orderId = UUID.randomUUID();
    var payment = persistPendingGatewayPayment(orderId);

    when(clock.instant()).thenReturn(NOW);

    try (var executor = Executors.newFixedThreadPool(2)) {
      var future1 = executor.submit(() -> service.createOrReuseCheckoutSession(TENANT_ID, orderId));
      var future2 = executor.submit(() -> service.createOrReuseCheckoutSession(TENANT_ID, orderId));

      var result1 = future1.get(15, TimeUnit.SECONDS);
      var result2 = future2.get(15, TimeUnit.SECONDS);

      assertThat(result1.attemptId()).isEqualTo(result2.attemptId());
      assertThat(result1.checkoutUrl()).isEqualTo(result2.checkoutUrl()).isEqualTo(SHARED_URL);
      assertThat(result1.expiresAt()).isEqualTo(result2.expiresAt()).isEqualTo(SHARED_EXPIRES_AT);

      var attempts = attemptRepository.findByTenantIdAndPaymentId(TENANT_ID, payment.getId());
      assertThat(attempts).hasSize(1);

      var persisted = attempts.getFirst();
      assertThat(persisted.getId()).isEqualTo(result1.attemptId());
      assertThat(persisted.getStatus()).isEqualTo(PENDING);
      assertThat(persisted.getProviderReference()).isEqualTo("cs_test_shared");
      assertThat(persisted.getRedirectUrl()).isEqualTo(SHARED_URL);
      assertThat(persisted.getExpiresAt()).isEqualTo(SHARED_EXPIRES_AT);

      assertThat(gateway.receivedSpecs())
          .hasSize(2)
          .extracting(CheckoutSessionSpec::attemptId)
          .containsOnly(persisted.getId());
    }
  }

  private Payment persistPendingGatewayPayment(UUID orderId) {
    return paymentRepository.save(
        Payment.pending(
            TENANT_ID, orderId, AMOUNT, PaymentMethod.CARD, PaymentProcessingMode.GATEWAY));
  }

  @TestConfiguration
  static class CheckoutTestConfiguration {

    @Bean
    SynchronizingPaymentGateway paymentGateway() {
      return new SynchronizingPaymentGateway();
    }
  }

  static class SynchronizingPaymentGateway implements PaymentGateway {

    private final CountDownLatch callersAtGateway = new CountDownLatch(2);
    private final List<CheckoutSessionSpec> receivedSpecs = new CopyOnWriteArrayList<>();
    private final CreatedCheckoutSession session =
        new CreatedCheckoutSession("cs_test_shared", SHARED_URL, SHARED_EXPIRES_AT);

    @Override
    public CreatedCheckoutSession createCheckoutSession(CheckoutSessionSpec spec) {
      receivedSpecs.add(spec);
      callersAtGateway.countDown();

      try {
        if (!callersAtGateway.await(5, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Both callers did not reach the payment gateway");
        }
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(
            "Interrupted while synchronizing gateway callers", exception);
      }

      return session;
    }

    List<CheckoutSessionSpec> receivedSpecs() {
      return List.copyOf(receivedSpecs);
    }
  }
}
