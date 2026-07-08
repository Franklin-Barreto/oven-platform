package br.com.f2e.ovenplatform.orders.application;

import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;

public interface OrderCreatedEventPublisher {

  void publish(OrderCreatedEvent event);
}
