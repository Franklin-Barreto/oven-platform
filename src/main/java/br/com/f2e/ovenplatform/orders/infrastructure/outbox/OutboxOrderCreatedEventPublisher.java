package br.com.f2e.ovenplatform.orders.infrastructure.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.ORDER_CREATED_EVENT;
import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.PAYLOAD_VERSION;

import br.com.f2e.ovenplatform.orders.application.OrderCreatedEventPublisher;
import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderCreatedItemPayload;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderCreatedPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.EnqueueOutboxEventCommand;
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
  public void publish(OrderCreatedEvent event) {
    var payload =
        new OrderCreatedPayload(
            event.tenantId(),
            event.orderId(),
            event.totalAmount(),
            event.paymentMethod(),
            event.paymentStatus(),
            event.items().stream()
                .map(
                    orderPlacedItem ->
                        new OrderCreatedItemPayload(
                            orderPlacedItem.productId(),
                            orderPlacedItem.productName(),
                            orderPlacedItem.quantity(),
                            orderPlacedItem.unitPrice()))
                .toList());

    outboxService.enqueueIdempotently(
        new EnqueueOutboxEventCommand(
            AGGREGATE_TYPE,
            event.orderId(),
            ORDER_CREATED_EVENT,
            orderTopic,
            event.orderId().toString(),
            payload,
            PAYLOAD_VERSION));
  }
}
