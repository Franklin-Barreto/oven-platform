package br.com.f2e.ovenplatform.fulfillment.infrastructure.event;

import br.com.f2e.ovenplatform.fulfillment.application.FulfillmentService;
import br.com.f2e.ovenplatform.fulfillment.application.PreparationReadyCommand;
import br.com.f2e.ovenplatform.kitchen.application.event.KitchenTicketMarkedAsReadyEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class KitchenTicketReadyEventListener {

  private final FulfillmentService fulfillmentService;

  public KitchenTicketReadyEventListener(FulfillmentService fulfillmentService) {
    this.fulfillmentService = fulfillmentService;
  }

  @ApplicationModuleListener(id = "fulfillment-kitchen-ticket-ready-listener")
  public void on(KitchenTicketMarkedAsReadyEvent event) {
    fulfillmentService.handlePreparationReady(
        new PreparationReadyCommand(event.tenantId(), event.orderId(), event.readyAt()));
  }
}
