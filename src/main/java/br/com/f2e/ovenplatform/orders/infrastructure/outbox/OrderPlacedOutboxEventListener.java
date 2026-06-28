package br.com.f2e.ovenplatform.orders.infrastructure.outbox;

import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedIntegrationEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedEvent;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Spring dependency injection stores managed beans by reference.")
@Component
public class OrderPlacedOutboxEventListener {

  private static final String AGGREGATE_TYPE = "ORDER";
  private static final String EVENT_TYPE = "order.created";
  private static final String TOPIC = "order.created";
  private static final int PAYLOAD_VERSION = 1;

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
        EVENT_TYPE,
        TOPIC,
        event.orderId().toString(),
        payload,
        PAYLOAD_VERSION);
  }
}
