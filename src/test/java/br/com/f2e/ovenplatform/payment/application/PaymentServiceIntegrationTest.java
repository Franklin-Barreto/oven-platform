package br.com.f2e.ovenplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.domain.Payment;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import br.com.f2e.ovenplatform.payment.infrastructure.persistence.JpaPaymentRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import({PaymentService.class, JpaPaymentRepositoryAdapter.class})
class PaymentServiceIntegrationTest extends DataJpaIntegrationTest {

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

  @Test
  void shouldListPaymentsByOrderIds() {
    var quantity = 5;
    var orderIds = createPendingPayments(TENANT_ID, quantity);

    var response = paymentService.findByTenantIdAndOrderIdIn(TENANT_ID, orderIds);

    assertThat(response)
        .hasSize(5)
        .allSatisfy(
            payment -> {
              assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
              assertThat(payment.method()).isEqualTo(PaymentMethod.CASH);
              assertThat(payment.paidAt()).isNull();
            });

    assertThat(response)
        .extracting(OrderPaymentResponse::orderId)
        .containsExactlyInAnyOrderElementsOf(orderIds);
  }

  @Test
  void shouldReturnOnlyPaymentsFromTenant() {
    var quantity = 5;
    var orderIds = createPendingPayments(TENANT_ID, quantity);
    var orderIdsFromAnotherTenant = createPendingPayments(UUID.randomUUID(), 3);
    orderIdsFromAnotherTenant.addAll(orderIds);
    var response = paymentService.findByTenantIdAndOrderIdIn(TENANT_ID, orderIdsFromAnotherTenant);

    assertThat(response)
        .hasSize(quantity)
        .allSatisfy(
            payment -> {
              assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
              assertThat(payment.method()).isEqualTo(PaymentMethod.CASH);
              assertThat(payment.paidAt()).isNull();
            });

    assertThat(response)
        .extracting(OrderPaymentResponse::orderId)
        .containsExactlyInAnyOrderElementsOf(orderIds);
  }

  private List<UUID> createPendingPayments(UUID tenantId, int quantity) {

    var orderIds = new ArrayList<UUID>(quantity);

    for (int i = 1; i <= quantity; i++) {
      Payment saved =
          paymentRepository.save(
              Payment.pending(tenantId, UUID.randomUUID(), PAYMENT_AMOUNT, PaymentMethod.CASH));
      orderIds.add(saved.getOrderId());
    }
    return orderIds;
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
