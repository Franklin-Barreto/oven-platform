package br.com.f2e.ovenplatform.fulfillment.application;

import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderMarkedAsReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class FulfillmentService {

  private final ApplicationEventPublisher eventPublisher;

  public FulfillmentService(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public void handlePreparationReady(PreparationReadyCommand command) {
    eventPublisher.publishEvent(
        new FulfillmentOrderMarkedAsReadyEvent(
            command.tenantId(), command.orderId(), command.readyAt()));
  }
}
