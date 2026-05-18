package br.com.f2e.ovenplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.domain.Payment;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import br.com.f2e.ovenplatform.payment.infrastructure.event.OrderPaymentMarkedAsPaidEventListener;
import br.com.f2e.ovenplatform.payment.infrastructure.persistence.JpaPaymentRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@ActiveProfiles("test")
@Import({
  PaymentService.class,
  JpaPaymentRepositoryAdapter.class,
  OrderPaymentMarkedAsPaidEventListener.class
})
@EnableJpaAuditing
class PaymentServiceIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecd");
  private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("20.00");
  private static final Instant PAID_AT = Instant.parse("2026-05-12T20:18:00Z");

  @Autowired private PaymentService paymentService;
  @Autowired private JpaPaymentRepositoryAdapter paymentRepository;
  @Autowired private EntityManager entityManager;

  @SuppressWarnings("unused")
  @MockitoBean
  private Clock clock;

  @Test
  void shouldRegisterPaidPayment() {

    when(clock.instant()).thenReturn(PAID_AT);

    paymentService.registerPaymentFromOrder(
        new RegisterPaymentCommand(
            TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, PaymentMethod.CASH, PaymentStatus.PAID));

    flushAndClear();

    var payment = paymentRepository.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID).orElseThrow();

    assertThat(payment.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(payment.getOrderId()).isEqualTo(ORDER_ID);
    assertThat(payment.getAmount()).isEqualByComparingTo("20.00");
    assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(payment.getPaidAt()).isEqualTo(PAID_AT);

    verify(clock).instant();
  }

  @Test
  void shouldRegisterPendingPayment() {

    paymentService.registerPaymentFromOrder(
        new RegisterPaymentCommand(
            TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, PaymentMethod.CASH, PaymentStatus.PENDING));

    flushAndClear();

    var payment = paymentRepository.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID).orElseThrow();

    assertThat(payment.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(payment.getOrderId()).isEqualTo(ORDER_ID);
    assertThat(payment.getAmount()).isEqualByComparingTo("20.00");
    assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(payment.getPaidAt()).isNull();

    verifyNoInteractions(clock);
  }

  @Test
  void shouldThrowExceptionWhenResourceNotFound() {
    assertThatThrownBy(() -> paymentService.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Payment for order id: %s not found".formatted(ORDER_ID));
  }

  @Test
  void shouldMarkPendingPaymentAsPaidByOrderId() {
    when(clock.instant()).thenReturn(PAID_AT);

    var payment = Payment.pending(TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, PaymentMethod.CASH);
    paymentRepository.save(payment);

    paymentService.markAsPaid(TENANT_ID, ORDER_ID);

    flushAndClear();

    var persistedPayment = paymentService.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID);

    assertThat(persistedPayment.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(persistedPayment.getPaidAt()).isEqualTo(PAID_AT);

    verify(clock).instant();
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
