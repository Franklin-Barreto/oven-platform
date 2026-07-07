package br.com.f2e.ovenplatform.kitchen.infrastructure.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.PAYLOAD_VERSION;
import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.TICKET_READY_EVENT;

import br.com.f2e.ovenplatform.kitchen.application.KitchenTicketReadyEventPublisher;
import br.com.f2e.ovenplatform.kitchen.application.event.KitchenTicketMarkedAsReadyEvent;
import br.com.f2e.ovenplatform.kitchen.application.event.KitchenTicketReadyPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OutboxKitchenTicketReadyEventPublisher implements KitchenTicketReadyEventPublisher {

  private final OutboxService outboxService;
  private final String kitchenTopic;

  public OutboxKitchenTicketReadyEventPublisher(
      OutboxService outboxService, @Value("${oven.kafka.topics.kitchen}") String kitchenTopic) {
    this.outboxService = outboxService;
    this.kitchenTopic = kitchenTopic;
  }

  @Override
  public void publish(KitchenTicketMarkedAsReadyEvent event) {
    var payload =
        new KitchenTicketReadyPayload(
            event.tenantId(), event.ticketId(), event.orderId(), event.readyAt());

    outboxService.enqueue(
        AGGREGATE_TYPE,
        event.ticketId(),
        TICKET_READY_EVENT,
        kitchenTopic,
        event.orderId().toString(),
        payload,
        PAYLOAD_VERSION);
  }
}
