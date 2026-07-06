package br.com.f2e.ovenplatform.kitchen.application;

import br.com.f2e.ovenplatform.kitchen.application.event.KitchenTicketMarkedAsReadyEvent;

public interface KitchenTicketReadyEventPublisher {

  void publish(KitchenTicketMarkedAsReadyEvent event);
}
