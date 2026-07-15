package br.com.f2e.ovenplatform.fulfillment.infrastructure.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.FULFILLMENT_ORDER_READY_EVENT;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.PAYLOAD_VERSION;

import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderMarkedAsReadyEvent;
import br.com.f2e.ovenplatform.shared.application.event.payload.FulfillmentOrderReadyPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.EnqueueOutboxEventCommand;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class OutboxFulfillmentOrderReadyEventPublisher {

  private final OutboxService outboxService;
  private final String fulfillmentTopic;

  public OutboxFulfillmentOrderReadyEventPublisher(
      OutboxService outboxService,
      @Value("${oven.kafka.topics.fulfillment}") String fulfillmentTopic) {
    this.outboxService = outboxService;
    this.fulfillmentTopic = fulfillmentTopic;
  }

  @ApplicationModuleListener(id = "fulfillment-order-ready-outbox-publisher")
  public void publish(FulfillmentOrderMarkedAsReadyEvent event) {
    var payload =
        new FulfillmentOrderReadyPayload(event.tenantId(), event.orderId(), event.readyAt());
    outboxService.enqueueIdempotently(
        new EnqueueOutboxEventCommand(
            AGGREGATE_TYPE,
            event.orderId(),
            FULFILLMENT_ORDER_READY_EVENT,
            fulfillmentTopic,
            event.orderId().toString(),
            payload,
            PAYLOAD_VERSION));
  }
}
