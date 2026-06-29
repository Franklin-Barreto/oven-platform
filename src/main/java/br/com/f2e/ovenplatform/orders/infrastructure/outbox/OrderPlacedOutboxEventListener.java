package br.com.f2e.ovenplatform.orders.infrastructure.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.OrderIntegrationEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.OrderIntegrationEventConstants.ORDER_CREATED_EVENT;
import static br.com.f2e.ovenplatform.shared.application.event.OrderIntegrationEventConstants.PAYLOAD_VERSION;
import static br.com.f2e.ovenplatform.shared.application.event.OrderIntegrationEventConstants.TOPIC;

import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedIntegrationEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedEvent;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderPlacedOutboxEventListener {

  private final OutboxService outboxService;

  public OrderPlacedOutboxEventListener(OutboxService outboxService) {
    this.outboxService = outboxService;
  }

  @EventListener
  void on(OrderPlacedEvent event) {
    var payload =
        new OrderCreatedIntegrationEvent(
            event.tenantId(),
            event.orderId(),
            event.totalAmount(),
            event.paymentMethod(),
            event.paymentStatus());

    outboxService.enqueue(
        AGGREGATE_TYPE,
        event.orderId(),
        ORDER_CREATED_EVENT,
        TOPIC,
        event.orderId().toString(),
        payload,
        PAYLOAD_VERSION);
  }
}
