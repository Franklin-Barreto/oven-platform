package br.com.f2e.ovenplatform.payment.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.domain.Payment;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentProcessingMode;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentConfirmedEvent;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  private static final UUID TENANT_ID = UUID.randomUUID();
  private static final UUID ORDER_ID = UUID.randomUUID();
  private static final Instant PAID_AT = Instant.parse("2026-08-03T12:00:00Z");

  @Mock private PaymentRepository paymentRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  @Test
  void shouldPublishConfirmationWhenPendingPaymentBecomesPaid() {
    var payment = Payment.pending(TENANT_ID, ORDER_ID, new BigDecimal("20.00"), PaymentMethod.CARD);
    when(paymentRepository.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID))
        .thenReturn(Optional.of(payment));
    var service =
        new PaymentService(paymentRepository, Clock.fixed(PAID_AT, ZoneOffset.UTC), eventPublisher);

    service.markManualPaymentAsPaid(TENANT_ID, ORDER_ID);

    verify(eventPublisher).publishEvent(new PaymentConfirmedEvent(TENANT_ID, ORDER_ID, PAID_AT));
  }

  @Test
  void shouldNotPublishDuplicateConfirmationForPaidPayment() {
    var payment =
        Payment.paid(TENANT_ID, ORDER_ID, new BigDecimal("20.00"), PaymentMethod.CARD, PAID_AT);
    when(paymentRepository.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID))
        .thenReturn(Optional.of(payment));
    var service =
        new PaymentService(
            paymentRepository,
            Clock.fixed(PAID_AT.plusSeconds(60), ZoneOffset.UTC),
            eventPublisher);

    service.markManualPaymentAsPaid(TENANT_ID, ORDER_ID);

    verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldConfirmGatewayPaymentThroughTrustedPath() {
    var payment =
        Payment.pending(
            TENANT_ID,
            ORDER_ID,
            new BigDecimal("20.00"),
            PaymentMethod.CARD,
            PaymentProcessingMode.GATEWAY);
    when(paymentRepository.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID))
        .thenReturn(Optional.of(payment));
    var service =
        new PaymentService(paymentRepository, Clock.fixed(PAID_AT, ZoneOffset.UTC), eventPublisher);

    service.confirmGatewayPayment(TENANT_ID, ORDER_ID);

    verify(eventPublisher).publishEvent(new PaymentConfirmedEvent(TENANT_ID, ORDER_ID, PAID_AT));
  }

  @Test
  void shouldRejectGatewayPaymentThroughManualConfirmationPath() {
    var payment =
        Payment.pending(
            TENANT_ID,
            ORDER_ID,
            new BigDecimal("20.00"),
            PaymentMethod.CARD,
            PaymentProcessingMode.GATEWAY);
    when(paymentRepository.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID))
        .thenReturn(Optional.of(payment));
    var service =
        new PaymentService(paymentRepository, Clock.fixed(PAID_AT, ZoneOffset.UTC), eventPublisher);

    assertThatThrownBy(() -> service.markManualPaymentAsPaid(TENANT_ID, ORDER_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("payment processing mode must be MANUAL");

    verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
  }
}
