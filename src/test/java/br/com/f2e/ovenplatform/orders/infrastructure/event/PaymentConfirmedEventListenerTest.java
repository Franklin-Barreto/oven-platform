package br.com.f2e.ovenplatform.orders.infrastructure.event;

import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentConfirmedEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmedEventListenerTest {

  @Mock private OrderService orderService;

  @InjectMocks private PaymentConfirmedEventListener listener;

  @Test
  void shouldReleaseOrderForPreparationAfterPaymentConfirmation() {
    var tenantId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    var paidAt = Instant.parse("2026-08-03T12:00:00Z");

    listener.on(new PaymentConfirmedEvent(tenantId, orderId, paidAt));

    verify(orderService).releaseForPreparation(tenantId, orderId, paidAt);
  }
}
