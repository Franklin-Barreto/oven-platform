package br.com.f2e.ovenplatform.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PaymentTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecd");
  private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("20.00");
  private static final Instant PAID_AT = Instant.parse("2026-05-12T20:18:00Z");

  @Test
  void shouldCreatePaidPaymentWithPaidAt() {

    var paymentPaid =
        Payment.paid(TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, PaymentMethod.CASH, PAID_AT);

    assertThat(paymentPaid.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(paymentPaid.getOrderId()).isEqualTo(ORDER_ID);
    assertThat(paymentPaid.getMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(paymentPaid.getPaidAt()).isEqualTo(PAID_AT);
    assertThat(paymentPaid.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(paymentPaid.getAmount()).isEqualByComparingTo("20.00");
  }

  @Test
  void shouldRejectPaidPaymentWithoutPaidAt() {
    assertThatThrownBy(
            () -> Payment.paid(TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, PaymentMethod.CASH, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("paidAt must not be null");
  }

  @Test
  void shouldCreatePendingPaymentWithoutPaidAt() {

    var pendingPayment = Payment.pending(TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, PaymentMethod.CASH);

    assertThat(pendingPayment.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(pendingPayment.getOrderId()).isEqualTo(ORDER_ID);
    assertThat(pendingPayment.getMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(pendingPayment.getPaidAt()).isNull();
    assertThat(pendingPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    assertThat(pendingPayment.getAmount()).isEqualByComparingTo("20.00");
  }

  @ParameterizedTest
  @MethodSource("invalidPaidPayments")
  void shouldRejectRequiredFields(
      UUID tenantId,
      UUID orderId,
      BigDecimal paymentAmount,
      PaymentMethod paymentMethod,
      Instant paidAt,
      String expectedMessage) {
    assertThatThrownBy(() -> Payment.paid(tenantId, orderId, paymentAmount, paymentMethod, paidAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldMarkPendingPaymentAsPaid() {

    var payment = Payment.pending(TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, PaymentMethod.CASH);

    payment.markAsPaid(PAID_AT);

    assertThat(payment.getPaidAt()).isEqualTo(PAID_AT);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
  }

  @Test
  void shouldKeepAlreadyPaidPaymentUnchanged() {

    var payment = Payment.paid(TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, PaymentMethod.CASH, PAID_AT);
    var secondPaymentTime = Instant.parse("2026-05-12T21:00:00Z");

    payment.markAsPaid(secondPaymentTime);

    assertThat(payment.getPaidAt()).isEqualTo(PAID_AT);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
  }

  @Test
  void shouldRejectMarkAsPaidWithoutPaidAt() {
    var payment = Payment.pending(TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, PaymentMethod.CASH);

    assertThatThrownBy(() -> payment.markAsPaid(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("paidAt must not be null");
  }

  private static Stream<Arguments> invalidPaidPayments() {
    return Stream.of(
        Arguments.of(
            null,
            ORDER_ID,
            PAYMENT_AMOUNT,
            PaymentMethod.CASH,
            PAID_AT,
            "tenantId must not be null"),
        Arguments.of(
            TENANT_ID,
            null,
            PAYMENT_AMOUNT,
            PaymentMethod.CASH,
            PAID_AT,
            "orderId must not be null"),
        Arguments.of(
            TENANT_ID,
            ORDER_ID,
            BigDecimal.ZERO,
            PaymentMethod.CASH,
            PAID_AT,
            "amount must be greater than zero"),
        Arguments.of(
            TENANT_ID, ORDER_ID, null, PaymentMethod.CASH, PAID_AT, "amount must not be null"),
        Arguments.of(
            TENANT_ID, ORDER_ID, PAYMENT_AMOUNT, null, PAID_AT, "paymentMethod must not be null"),
        Arguments.of(
            TENANT_ID,
            ORDER_ID,
            PAYMENT_AMOUNT,
            PaymentMethod.CASH,
            null,
            "paidAt must not be null"));
  }
}
