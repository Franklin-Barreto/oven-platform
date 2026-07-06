package br.com.f2e.ovenplatform.orders.application;

import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedEvent;

public interface OrderCreatedEventPublisher {

  void publish(OrderPlacedEvent event);
}
