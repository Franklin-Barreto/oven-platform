package br.com.f2e.ovenplatform.fulfillment.application;

import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderMarkedAsReadyEvent;

public interface FulfillmentOrderReadyEventPublisher {

  void publish(FulfillmentOrderMarkedAsReadyEvent event);
}
