package br.com.f2e.ovenplatform.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentProcessingMode;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import org.junit.jupiter.api.Test;

class PaymentInfoTest {

  @Test
  void shouldAcceptExplicitManualProcessing() {
    var paymentInfo =
        new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PENDING, PaymentProcessingMode.MANUAL);

    assertThat(paymentInfo.processingMode()).isEqualTo(PaymentProcessingMode.MANUAL);
  }

  @Test
  void shouldAllowPendingGatewayCardPayment() {
    var paymentInfo =
        new PaymentInfo(PaymentMethod.CARD, PaymentStatus.PENDING, PaymentProcessingMode.GATEWAY);

    assertThat(paymentInfo.processingMode()).isEqualTo(PaymentProcessingMode.GATEWAY);
  }

  @Test
  void shouldRejectCashProcessedByGateway() {
    assertThatThrownBy(
            () ->
                new PaymentInfo(
                    PaymentMethod.CASH, PaymentStatus.PENDING, PaymentProcessingMode.GATEWAY))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("gateway payment does not support cash");
  }

  @Test
  void shouldRejectGatewayPaymentAlreadyMarkedAsPaid() {
    assertThatThrownBy(
            () ->
                new PaymentInfo(
                    PaymentMethod.CARD, PaymentStatus.PAID, PaymentProcessingMode.GATEWAY))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("gateway payment must start pending");
  }
}
