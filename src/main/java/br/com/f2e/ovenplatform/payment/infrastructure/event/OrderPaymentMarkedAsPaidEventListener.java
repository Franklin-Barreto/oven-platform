package br.com.f2e.ovenplatform.payment.infrastructure.event;

import br.com.f2e.ovenplatform.orders.application.event.OrderPaymentMarkedAsPaidEvent;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPaymentMarkedAsPaidEventListener {

  private final PaymentService paymentService;

  public OrderPaymentMarkedAsPaidEventListener(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @ApplicationModuleListener
  public void on(OrderPaymentMarkedAsPaidEvent event) {
    paymentService.markAsPaid(event.tenantId(), event.orderId());
  }
}
