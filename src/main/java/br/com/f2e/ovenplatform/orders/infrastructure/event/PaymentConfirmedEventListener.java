package br.com.f2e.ovenplatform.orders.infrastructure.event;

import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentConfirmedEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfirmedEventListener {

  private final OrderService orderService;

  public PaymentConfirmedEventListener(OrderService orderService) {
    this.orderService = orderService;
  }

  @ApplicationModuleListener(id = "orders-payment-confirmed-listener")
  public void on(PaymentConfirmedEvent event) {
    orderService.releaseForPreparation(event.tenantId(), event.orderId(), event.paidAt());
  }
}
