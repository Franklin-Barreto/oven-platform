package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.FULFILLMENT_ORDER_READY_EVENT;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.f2e.ovenplatform.fulfillment.application.FulfillmentService;
import br.com.f2e.ovenplatform.fulfillment.infrastructure.kafka.KitchenTicketReadyConsumer;
import br.com.f2e.ovenplatform.fulfillment.infrastructure.outbox.OutboxFulfillmentOrderReadyEventPublisher;
import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfoProvider;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.application.OrderableProductProvider;
import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.orders.infrastructure.kafka.FulfillmentOrderReadyConsumer;
import br.com.f2e.ovenplatform.orders.infrastructure.persistence.JpaOrderRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants;
import br.com.f2e.ovenplatform.shared.application.event.payload.FulfillmentOrderReadyPayload;
import br.com.f2e.ovenplatform.shared.application.event.payload.KitchenTicketReadyPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence.JpaOutboxEventRepository;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({
  FulfillmentOrderReadyConsumer.class,
  FulfillmentService.class,
  JpaOrderRepositoryAdapter.class,
  JpaOutboxEventRepository.class,
  KitchenTicketReadyConsumer.class,
  OrderService.class,
  OutboxFulfillmentOrderReadyEventPublisher.class,
  OutboxService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConsumerIdempotencyIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final Instant READY_AT = Instant.parse("2026-05-12T20:30:00Z");

  @Autowired private KitchenTicketReadyConsumer fulfillmentConsumer;
  @Autowired private FulfillmentOrderReadyConsumer orderConsumer;
  @Autowired private OrderService orderService;

  @MockitoBean private Clock clock;
  @MockitoBean private OrderableProductProvider orderableProductProvider;
  @MockitoBean private CustomerDeliveryInfoProvider customerDeliveryInfoProvider;

  @Test
  void shouldIgnoreDuplicateKitchenTicketReadyDeliveryForFulfillmentEffect() {
    var orderId = UUID.randomUUID();
    var ticketId = UUID.randomUUID();
    var repeatedReadyAt = Instant.parse("2026-05-12T20:45:00Z");

    fulfillmentConsumer.on(
        JsonUtils.toJson(new KitchenTicketReadyPayload(TENANT_ID, ticketId, orderId, READY_AT)));
    fulfillmentConsumer.on(
        JsonUtils.toJson(
            new KitchenTicketReadyPayload(TENANT_ID, ticketId, orderId, repeatedReadyAt)));

    assertThat(countFulfillmentOrderReadyOutboxEvents(orderId)).isOne();
    assertThat(findFulfillmentOrderReadyOutboxEvent(orderId).getPayload())
        .contains(READY_AT.toString());
  }

  @Test
  void shouldPreserveOriginalReadyAtWhenFulfillmentOrderReadyDeliveryIsRepeated() {
    var order = orderService.save(new Order(TENANT_ID, OrderServiceType.COUNTER));
    var repeatedReadyAt = Instant.parse("2026-05-12T20:45:00Z");

    orderConsumer.on(
        JsonUtils.toJson(new FulfillmentOrderReadyPayload(TENANT_ID, order.getId(), READY_AT)));
    orderConsumer.on(
        JsonUtils.toJson(
            new FulfillmentOrderReadyPayload(TENANT_ID, order.getId(), repeatedReadyAt)));

    var persistedOrder = orderService.findOrder(TENANT_ID, order.getId()).orElseThrow();

    assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.READY);
    assertThat(persistedOrder.getReadyAt()).isEqualTo(READY_AT);
  }

  private long countFulfillmentOrderReadyOutboxEvents(UUID orderId) {
    return entityManager
        .createQuery(
            """
            select count(event)
            from OutboxEvent event
            where event.aggregateType = :aggregateType
              and event.aggregateId = :aggregateId
              and event.eventType = :eventType
            """,
            Long.class)
        .setParameter("aggregateType", FulfillmentEventConstants.AGGREGATE_TYPE)
        .setParameter("aggregateId", orderId)
        .setParameter("eventType", FULFILLMENT_ORDER_READY_EVENT)
        .getSingleResult();
  }

  private OutboxEvent findFulfillmentOrderReadyOutboxEvent(UUID orderId) {
    return entityManager
        .createQuery(
            """
            select event
            from OutboxEvent event
            where event.aggregateType = :aggregateType
              and event.aggregateId = :aggregateId
              and event.eventType = :eventType
            """,
            OutboxEvent.class)
        .setParameter("aggregateType", FulfillmentEventConstants.AGGREGATE_TYPE)
        .setParameter("aggregateId", orderId)
        .setParameter("eventType", FULFILLMENT_ORDER_READY_EVENT)
        .getSingleResult();
  }
}
