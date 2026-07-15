package br.com.f2e.ovenplatform.fulfillment.infrastructure.event;

import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.FULFILLMENT_ORDER_READY_EVENT;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.f2e.ovenplatform.kitchen.application.CreateTicketCommand;
import br.com.f2e.ovenplatform.kitchen.application.CreateTicketItemCommand;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.kitchen.application.event.KitchenTicketMarkedAsReadyEvent;
import br.com.f2e.ovenplatform.shared.application.event.payload.FulfillmentOrderReadyPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventRepository;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.time.Duration;
import java.time.Instant;
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
class FulfillmentModuleEventsIntegrationTest {

  private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");

  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private KitchenService kitchenService;
  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactionManager;

  @Value("${oven.kafka.topics.fulfillment}")
  private String fulfillmentTopic;

  @BeforeEach
  void cleanPublicationsOutboxAndTickets() {
    jdbc.update("delete from event_publication");
    jdbc.update("delete from outbox_events");
    jdbc.update("delete from kitchen_ticket_items");
    jdbc.update("delete from kitchen_tickets");
  }

  @Test
  void shouldPropagateKitchenReadinessToFulfillmentWithoutKafka() {
    var ticket = kitchenService.createTicketFromOrder(createTicketCommand(ORDER_ID));

    kitchenService.startPreparation(TENANT_ID, ticket.getId());
    kitchenService.markAsReady(TENANT_ID, ticket.getId());
    var readyAt = kitchenService.findById(TENANT_ID, ticket.getId()).getReadyAt();

    awaitLegacyOutboxRecord(ORDER_ID);
    awaitCompletedPublication("fulfillment-kitchen-ticket-ready-listener", ORDER_ID, 1);
    awaitCompletedPublication("fulfillment-order-ready-outbox-publisher", ORDER_ID, 1);

    var outboxEvent = findLegacyOutboxRecord(ORDER_ID);
    var payload = JsonUtils.fromJson(outboxEvent.getPayload(), FulfillmentOrderReadyPayload.class);

    assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(outboxEvent.getTopic()).isEqualTo(fulfillmentTopic);
    assertThat(outboxEvent.getMessageKey()).isEqualTo(ORDER_ID.toString());
    assertThat(payload.tenantId()).isEqualTo(TENANT_ID);
    assertThat(payload.orderId()).isEqualTo(ORDER_ID);
    assertThat(payload.readyAt()).isEqualTo(readyAt);
  }

  @Test
  void shouldPreserveOriginalReadyAtWhenKitchenEventIsRedelivered() {
    var ticketId = UUID.randomUUID();
    var orderId = UUID.randomUUID();
    var readyAt = Instant.parse("2026-07-14T19:01:16.681Z");
    var repeatedReadyAt = Instant.parse("2026-07-14T19:15:00Z");

    publishInTransaction(
        new KitchenTicketMarkedAsReadyEvent(TENANT_ID, ticketId, orderId, readyAt));
    awaitLegacyOutboxRecord(orderId);

    publishInTransaction(
        new KitchenTicketMarkedAsReadyEvent(TENANT_ID, ticketId, orderId, repeatedReadyAt));

    awaitCompletedPublication("fulfillment-kitchen-ticket-ready-listener", orderId, 2);
    awaitCompletedPublication("fulfillment-order-ready-outbox-publisher", orderId, 2);

    assertThat(legacyOutboxRecordCount(orderId)).isOne();

    var payload =
        JsonUtils.fromJson(
            findLegacyOutboxRecord(orderId).getPayload(), FulfillmentOrderReadyPayload.class);

    assertThat(payload.readyAt()).isEqualTo(readyAt);
  }

  private CreateTicketCommand createTicketCommand(UUID orderId) {
    return new CreateTicketCommand(
        TENANT_ID,
        orderId,
        List.of(new CreateTicketItemCommand(PRODUCT_ID, "Pizza Portuguesa", 2)));
  }

  private void publishInTransaction(KitchenTicketMarkedAsReadyEvent event) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(_ -> eventPublisher.publishEvent(event));
  }

  private void awaitLegacyOutboxRecord(UUID orderId) {
    await()
        .atMost(ASYNC_TIMEOUT)
        .untilAsserted(
            () ->
                assertThat(
                        outboxEventRepository.findByAggregateTypeAndAggregateIdAndEventType(
                            AGGREGATE_TYPE, orderId, FULFILLMENT_ORDER_READY_EVENT))
                    .isPresent());
  }

  private OutboxEvent findLegacyOutboxRecord(UUID orderId) {
    return outboxEventRepository
        .findByAggregateTypeAndAggregateIdAndEventType(
            AGGREGATE_TYPE, orderId, FULFILLMENT_ORDER_READY_EVENT)
        .orElseThrow();
  }

  private int legacyOutboxRecordCount(UUID orderId) {
    return requireNonNull(
        jdbc.queryForObject(
            """
            select count(*)
            from outbox_events
            where aggregate_type = ? and aggregate_id = ? and event_type = ?
            """,
            Integer.class,
            AGGREGATE_TYPE,
            orderId,
            FULFILLMENT_ORDER_READY_EVENT),
        "Legacy outbox record count not returned for order " + orderId);
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
