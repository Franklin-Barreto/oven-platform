package br.com.f2e.ovenplatform.kitchen.infrastructure.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.PAYLOAD_VERSION;
import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.TICKET_READY_EVENT;
import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.TOPIC;

import br.com.f2e.ovenplatform.kitchen.application.KitchenTicketReadyEventPublisher;
import br.com.f2e.ovenplatform.kitchen.application.event.KitchenTicketMarkedAsReadyEvent;
import br.com.f2e.ovenplatform.kitchen.application.event.KitchenTicketReadyPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import org.springframework.stereotype.Component;

@Component
public class OutboxKitchenTicketReadyEventPublisher implements KitchenTicketReadyEventPublisher {

  private final OutboxService outboxService;

  public OutboxKitchenTicketReadyEventPublisher(OutboxService outboxService) {
    this.outboxService = outboxService;
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
        TOPIC,
        event.orderId().toString(),
        payload,
        PAYLOAD_VERSION);
  }
}
