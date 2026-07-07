package br.com.f2e.ovenplatform.fulfillment.application;

import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderMarkedAsReadyEvent;
import org.springframework.stereotype.Service;

@Service
public class FulfillmentService {

  private final FulfillmentOrderReadyEventPublisher eventPublisher;

  public FulfillmentService(FulfillmentOrderReadyEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public void handlePreparationReady(PreparationReadyCommand command) {
    eventPublisher.publish(
        new FulfillmentOrderMarkedAsReadyEvent(
            command.tenantId(), command.orderId(), command.readyAt()));
  }
}
