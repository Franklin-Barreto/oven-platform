package br.com.f2e.ovenplatform.kitchen.infrastructure.event;

import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.TICKET_READY_EVENT;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.f2e.ovenplatform.kitchen.application.CreateTicketCommand;
import br.com.f2e.ovenplatform.kitchen.application.CreateTicketItemCommand;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.kitchen.domain.TicketStatus;
import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedItem;
import br.com.f2e.ovenplatform.shared.application.event.payload.KitchenTicketReadyPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventRepository;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
      "oven.events.publication.maintenance.enabled=false",
      "oven.outbox.publishing.enabled=false",
      "spring.kafka.admin.auto-create=false",
      "spring.kafka.listener.auto-startup=false"
    })
@Import(PostgresTestContainerConfiguration.class)
class KitchenModuleEventsIntegrationTest {

  private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");

  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private KitchenService kitchenService;
  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactionManager;

  @Value("${oven.kafka.topics.kitchen}")
  private String kitchenTopic;

  @BeforeEach
  void cleanPublicationsAndTickets() {
    jdbc.update("delete from event_publication");
    jdbc.update("delete from outbox_events");
    jdbc.update("delete from kitchen_ticket_items");
    jdbc.update("delete from kitchen_tickets");
  }

  @Test
  void shouldConsumeCanonicalOrderCreatedEventIdempotently() {
    var event = orderCreatedEvent();

    publishInTransaction(event);

    await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> assertThat(ticketCount(ORDER_ID)).isOne());
    awaitCompletedPublication("kitchen-order-created-listener", ORDER_ID, 1);

    publishInTransaction(event);

    awaitCompletedPublication("kitchen-order-created-listener", ORDER_ID, 2);

    var ticket = kitchenService.findByOrderIdWithItems(TENANT_ID, ORDER_ID);

    assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RECEIVED);
    assertThat(ticket.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(ticket.getOrderId()).isEqualTo(ORDER_ID);
    assertThat(ticket.getItems())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
              assertThat(item.getProductName()).isEqualTo("Pizza Portuguesa");
              assertThat(item.getQuantity()).isEqualTo(2);
            });
  }

  @Test
  void shouldPublishOneCanonicalReadyEventAndOneLegacyOutboxRecord() {
    var ticket = kitchenService.createTicketFromOrder(createTicketCommand());

    kitchenService.startPreparation(TENANT_ID, ticket.getId());
    kitchenService.markAsReady(TENANT_ID, ticket.getId());
    var readyAt = kitchenService.findById(TENANT_ID, ticket.getId()).getReadyAt();

    await()
        .atMost(ASYNC_TIMEOUT)
        .untilAsserted(
            () ->
                assertThat(
                        outboxEventRepository.findByAggregateTypeAndAggregateIdAndEventType(
                            AGGREGATE_TYPE, ticket.getId(), TICKET_READY_EVENT))
                    .isPresent());
    awaitCompletedPublication("kitchen-ticket-ready-outbox-publisher", ticket.getId(), 1);

    kitchenService.markAsReady(TENANT_ID, ticket.getId());

    var outboxEvent =
        outboxEventRepository
            .findByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE, ticket.getId(), TICKET_READY_EVENT)
            .orElseThrow();

    assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(outboxEvent.getTopic()).isEqualTo(kitchenTopic);
    assertThat(outboxEvent.getMessageKey()).isEqualTo(ORDER_ID.toString());
    assertThat(legacyOutboxRecordCount(ticket.getId())).isOne();
    assertThat(completedPublicationCount("kitchen-ticket-ready-outbox-publisher", ticket.getId()))
        .isOne();

    var payload = JsonUtils.fromJson(outboxEvent.getPayload(), KitchenTicketReadyPayload.class);

    assertThat(payload.tenantId()).isEqualTo(TENANT_ID);
    assertThat(payload.ticketId()).isEqualTo(ticket.getId());
    assertThat(payload.orderId()).isEqualTo(ORDER_ID);
    assertThat(payload.readyAt()).isEqualTo(readyAt);
  }

  private void publishInTransaction(OrderCreatedEvent event) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(_ -> eventPublisher.publishEvent(event));
  }

  private OrderCreatedEvent orderCreatedEvent() {
    return new OrderCreatedEvent(
        TENANT_ID,
        ORDER_ID,
        PaymentMethod.CASH,
        PaymentStatus.PAID,
        new BigDecimal("120.00"),
        List.of(new OrderPlacedItem(PRODUCT_ID, "Pizza Portuguesa", 2, new BigDecimal("60.00"))));
  }

  private CreateTicketCommand createTicketCommand() {
    return new CreateTicketCommand(
        TENANT_ID,
        ORDER_ID,
        List.of(new CreateTicketItemCommand(PRODUCT_ID, "Pizza Portuguesa", 2)));
  }

  private int ticketCount(UUID orderId) {
    return requireNonNull(
        jdbc.queryForObject(
            "select count(*) from kitchen_tickets where tenant_id = ? and order_id = ?",
            Integer.class,
            TENANT_ID,
            orderId),
        "Kitchen ticket count not returned for order " + orderId);
  }

  private int legacyOutboxRecordCount(UUID ticketId) {
    return requireNonNull(
        jdbc.queryForObject(
            """
            select count(*)
            from outbox_events
            where aggregate_type = ? and aggregate_id = ? and event_type = ?
            """,
            Integer.class,
            AGGREGATE_TYPE,
            ticketId,
            TICKET_READY_EVENT),
        "Legacy outbox record count not returned for ticket " + ticketId);
  }

  private void awaitCompletedPublication(String listenerId, UUID eventId, int expectedCount) {
    await()
        .atMost(ASYNC_TIMEOUT)
        .untilAsserted(
            () ->
                assertThat(completedPublicationCount(listenerId, eventId))
                    .isEqualTo(expectedCount));
  }

  private int completedPublicationCount(String listenerId, UUID eventId) {
    return requireNonNull(
        jdbc.queryForObject(
            """
            select count(*)
            from event_publication
            where listener_id = ?
              and serialized_event like ?
              and status = 'COMPLETED'
            """,
            Integer.class,
            listenerId,
            serializedEventPattern(eventId)),
        "Completed publication count not returned for event " + eventId);
  }

  private String serializedEventPattern(UUID id) {
    return "%" + id + "%";
  }
}
