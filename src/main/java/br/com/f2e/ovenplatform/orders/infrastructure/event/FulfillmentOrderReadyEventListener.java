package br.com.f2e.ovenplatform.orders.infrastructure.event;

import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderMarkedAsReadyEvent;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class FulfillmentOrderReadyEventListener {

  private final OrderService orderService;

  public FulfillmentOrderReadyEventListener(OrderService orderService) {
    this.orderService = orderService;
  }

  @ApplicationModuleListener(id = "orders-fulfillment-order-ready-listener")
  public void on(FulfillmentOrderMarkedAsReadyEvent event) {
    orderService.markAsReady(event.tenantId(), event.orderId(), event.readyAt());
  }
}
