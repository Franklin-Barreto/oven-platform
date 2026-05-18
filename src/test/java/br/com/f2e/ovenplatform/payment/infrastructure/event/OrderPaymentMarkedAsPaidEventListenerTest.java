package br.com.f2e.ovenplatform.payment.infrastructure.event;

import static org.mockito.Mockito.verify;

import br.com.f2e.ovenplatform.orders.application.event.OrderPaymentMarkedAsPaidEvent;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPaymentMarkedAsPaidEventListenerTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecd");

  @Mock private PaymentService paymentService;

  @InjectMocks private OrderPaymentMarkedAsPaidEventListener listener;

  @Test
  void shouldMarkPaymentAsPaidWhenReceivingEvent() {
    listener.on(new OrderPaymentMarkedAsPaidEvent(TENANT_ID, ORDER_ID));

    verify(paymentService).markAsPaid(TENANT_ID, ORDER_ID);
  }
}
