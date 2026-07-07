package br.com.f2e.ovenplatform.fulfillment.infrastructure.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.FULFILLMENT_ORDER_READY_EVENT;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.PAYLOAD_VERSION;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.TOPIC;

import br.com.f2e.ovenplatform.fulfillment.application.FulfillmentOrderReadyEventPublisher;
import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderMarkedAsReadyEvent;
import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderReadyPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import org.springframework.stereotype.Component;

@Component
public class OutboxFulfillmentOrderReadyEventPublisher
    implements FulfillmentOrderReadyEventPublisher {

  private final OutboxService outboxService;

  public OutboxFulfillmentOrderReadyEventPublisher(OutboxService outboxService) {
    this.outboxService = outboxService;
  }

  @Override
  public void publish(FulfillmentOrderMarkedAsReadyEvent event) {
    var payload =
        new FulfillmentOrderReadyPayload(event.tenantId(), event.orderId(), event.readyAt());
    outboxService.enqueueIfAbsent(
        AGGREGATE_TYPE,
        event.orderId(),
        FULFILLMENT_ORDER_READY_EVENT,
        TOPIC,
        event.orderId().toString(),
        payload,
        PAYLOAD_VERSION);
  }
}
