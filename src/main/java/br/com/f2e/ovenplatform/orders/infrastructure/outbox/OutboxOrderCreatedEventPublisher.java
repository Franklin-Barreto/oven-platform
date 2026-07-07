package br.com.f2e.ovenplatform.orders.infrastructure.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.ORDER_CREATED_EVENT;
import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.PAYLOAD_VERSION;

import br.com.f2e.ovenplatform.orders.application.OrderCreatedEventPublisher;
import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedItemPayload;
import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedPayload;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedEvent;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OutboxOrderCreatedEventPublisher implements OrderCreatedEventPublisher {

  private final OutboxService outboxService;
  private final String orderTopic;

  public OutboxOrderCreatedEventPublisher(
      OutboxService outboxService, @Value("${oven.kafka.topics.orders}") String orderTopic) {
    this.outboxService = outboxService;
    this.orderTopic = orderTopic;
  }

  @Override
  public void publish(OrderPlacedEvent event) {
    var payload =
        new OrderCreatedPayload(
            event.tenantId(),
            event.orderId(),
            event.totalAmount(),
            event.paymentMethod(),
            event.paymentStatus(),
            event.items().stream().map(OrderCreatedItemPayload::from).toList());

    outboxService.enqueue(
        AGGREGATE_TYPE,
        event.orderId(),
        ORDER_CREATED_EVENT,
        orderTopic,
        event.orderId().toString(),
        payload,
        PAYLOAD_VERSION);
  }
}
