package br.com.f2e.ovenplatform.kitchen.application;

import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.KitchenEventConstants.TICKET_READY_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.kitchen.application.event.KitchenTicketReadyPayload;
import br.com.f2e.ovenplatform.kitchen.domain.TicketStatus;
import br.com.f2e.ovenplatform.kitchen.infrastructure.outbox.OutboxKitchenTicketReadyEventPublisher;
import br.com.f2e.ovenplatform.kitchen.infrastructure.persistence.JpaTicketRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventRepository;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.outbox.JpaOutboxEventRepository;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({
  KitchenService.class,
  JpaTicketRepositoryAdapter.class,
  OutboxService.class,
  JpaOutboxEventRepository.class,
  OutboxKitchenTicketReadyEventPublisher.class
})
class KitchenServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID OTHER_TENANT_ID =
      UUID.fromString("c7210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final String PRODUCT_NAME = "Pizza Portuguesa";
  private static final int VALID_QUANTITY = 2;

  @Value("${oven.kafka.topics.kitchen}")
  private String kitchenTopic;

  private @Autowired KitchenService kitchenService;
  private @Autowired OutboxEventRepository outboxEventRepository;
  private @MockitoBean Clock clock;

  @Test
  void shouldCreateAndFindTicketWithItems() {
    var ticket = kitchenService.createTicketFromOrder(createTicketCommand());

    flushAndClear();

    var foundTicket = kitchenService.findByIdWithItems(TENANT_ID, ticket.getId());

    assertThat(foundTicket.getStatus()).isEqualTo(TicketStatus.RECEIVED);
    assertThat(foundTicket.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(foundTicket.getOrderId()).isEqualTo(ORDER_ID);
    assertThat(foundTicket.getItems()).hasSize(1);

    var item = foundTicket.getItems().getFirst();

    assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
    assertThat(item.getProductName()).isEqualTo(PRODUCT_NAME);
    assertThat(item.getQuantity()).isEqualTo(VALID_QUANTITY);
  }

  @Test
  void shouldListTicketsByTenantWithItems() {
    var firstTicket = kitchenService.createTicketFromOrder(createTicketCommand());
    kitchenService.createTicketFromOrder(createTicketCommand(OTHER_TENANT_ID, UUID.randomUUID()));

    flushAndClear();

    var tickets = kitchenService.list(TENANT_ID);

    assertThat(tickets)
        .singleElement()
        .satisfies(
            ticket -> {
              assertThat(ticket.getId()).isEqualTo(firstTicket.getId());
              assertThat(ticket.getTenantId()).isEqualTo(TENANT_ID);
              assertThat(ticket.getItems()).hasSize(1);
            });
  }

  @Test
  void shouldPersistTicketStatusTransitions() {
    var startedAt = Instant.parse("2026-05-12T20:18:00Z");
    var readyAt = Instant.parse("2026-05-12T20:30:00Z");
    when(clock.instant()).thenReturn(startedAt, readyAt);

    var ticket = kitchenService.createTicketFromOrder(createTicketCommand());

    flushAndClear();

    kitchenService.startPreparation(TENANT_ID, ticket.getId());
    kitchenService.markAsReady(TENANT_ID, ticket.getId());

    flushAndClear();

    var foundTicket = kitchenService.findById(TENANT_ID, ticket.getId());

    assertThat(foundTicket.getStatus()).isEqualTo(TicketStatus.READY);
    assertThat(foundTicket.getStartedAt()).isEqualTo(startedAt);
    assertThat(foundTicket.getReadyAt()).isEqualTo(readyAt);
    assertThat(foundTicket.getCancelledAt()).isNull();
  }

  @Test
  void shouldCreatePendingOutboxEventWhenTicketIsMarkedAsReady() {
    var startedAt = Instant.parse("2026-05-12T20:18:00Z");
    var readyAt = Instant.parse("2026-05-12T20:30:00Z");
    when(clock.instant()).thenReturn(startedAt, readyAt);

    var ticket = kitchenService.createTicketFromOrder(createTicketCommand());

    flushAndClear();

    kitchenService.startPreparation(TENANT_ID, ticket.getId());
    kitchenService.markAsReady(TENANT_ID, ticket.getId());

    flushAndClear();

    var outboxEvent =
        outboxEventRepository
            .findByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE, ticket.getId(), TICKET_READY_EVENT)
            .orElseThrow();

    assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(outboxEvent.getTopic()).isEqualTo(kitchenTopic);
    assertThat(outboxEvent.getMessageKey()).isEqualTo(ORDER_ID.toString());
    assertThat(outboxEvent.getPayloadVersion()).isEqualTo(1);
    assertThat(outboxEvent.getAttempts()).isZero();
    assertThat(outboxEvent.getPublishedAt()).isNull();
    assertThat(outboxEvent.getLastError()).isNull();

    var payload = JsonUtils.fromJson(outboxEvent.getPayload(), KitchenTicketReadyPayload.class);

    assertThat(payload.tenantId()).isEqualTo(TENANT_ID);
    assertThat(payload.ticketId()).isEqualTo(ticket.getId());
    assertThat(payload.orderId()).isEqualTo(ORDER_ID);
    assertThat(payload.readyAt()).isEqualTo(readyAt);
  }

  @Test
  void shouldNotCreateDuplicatedOutboxEventWhenTicketIsAlreadyReady() {
    var startedAt = Instant.parse("2026-05-12T20:18:00Z");
    var readyAt = Instant.parse("2026-05-12T20:30:00Z");
    var repeatedReadyAt = Instant.parse("2026-05-12T20:45:00Z");
    when(clock.instant()).thenReturn(startedAt, readyAt, repeatedReadyAt);

    var ticket = kitchenService.createTicketFromOrder(createTicketCommand());

    flushAndClear();

    kitchenService.startPreparation(TENANT_ID, ticket.getId());
    kitchenService.markAsReady(TENANT_ID, ticket.getId());
    kitchenService.markAsReady(TENANT_ID, ticket.getId());

    flushAndClear();

    var outboxEventCount =
        entityManager
            .createQuery(
                """
                select count(event)
                from OutboxEvent event
                where event.aggregateType = :aggregateType
                  and event.aggregateId = :aggregateId
                  and event.eventType = :eventType
                """,
                Long.class)
            .setParameter("aggregateType", AGGREGATE_TYPE)
            .setParameter("aggregateId", ticket.getId())
            .setParameter("eventType", TICKET_READY_EVENT)
            .getSingleResult();

    assertThat(outboxEventCount).isOne();

    var outboxEvent =
        outboxEventRepository
            .findByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE, ticket.getId(), TICKET_READY_EVENT)
            .orElseThrow();
    var payload = JsonUtils.fromJson(outboxEvent.getPayload(), KitchenTicketReadyPayload.class);

    assertThat(payload.readyAt()).isEqualTo(readyAt);
  }

  @Test
  void shouldThrowResourceNotFoundWhenTicketBelongsToAnotherTenant() {
    var ticket = kitchenService.createTicketFromOrder(createTicketCommand());

    flushAndClear();

    assertThatThrownBy(() -> kitchenService.findById(OTHER_TENANT_ID, ticket.getId()))
        .hasMessage("Ticket id: %s not found".formatted(ticket.getId()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldThrownResourceNotFoundExceptionWhenTicketDoesNotExists() {
    var unknownTicketId = UUID.randomUUID();
    assertThatThrownBy(() -> kitchenService.findById(TENANT_ID, unknownTicketId))
        .hasMessage("Ticket id: %s not found".formatted(unknownTicketId))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldNotCreateDuplicatedTicketForSameTenantAndOrder() {
    var firstTicket = kitchenService.createTicketFromOrder(createTicketCommand());

    kitchenService.createTicketFromOrder(createTicketCommand());

    flushAndClear();

    var tickets = kitchenService.list(TENANT_ID);

    assertThat(tickets)
        .singleElement()
        .satisfies(
            ticket -> {
              assertThat(ticket.getId()).isEqualTo(firstTicket.getId());
              assertThat(ticket.getTenantId()).isEqualTo(TENANT_ID);
              assertThat(ticket.getOrderId()).isEqualTo(ORDER_ID);
              assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RECEIVED);
              assertThat(ticket.getItems()).hasSize(1);
            });
  }

  @Test
  void shouldCreateTicketsForSameOrderIdInDifferentTenants() {
    kitchenService.createTicketFromOrder(createTicketCommand(TENANT_ID, ORDER_ID));
    kitchenService.createTicketFromOrder(createTicketCommand(OTHER_TENANT_ID, ORDER_ID));

    flushAndClear();

    assertThat(kitchenService.list(TENANT_ID)).hasSize(1);
    assertThat(kitchenService.list(OTHER_TENANT_ID)).hasSize(1);
  }

  @Test
  void shouldFindTicketByOrderIdWithItems() {

    var firstTicket = kitchenService.createTicketFromOrder(createTicketCommand());

    flushAndClear();

    var tickets = kitchenService.findByOrderIdWithItems(TENANT_ID, ORDER_ID);

    assertThat(tickets)
        .satisfies(
            ticket -> {
              assertThat(ticket.getId()).isEqualTo(firstTicket.getId());
              assertThat(ticket.getTenantId()).isEqualTo(TENANT_ID);
              assertThat(ticket.getItems()).hasSize(1);
            });
  }

  @Test
  void shouldThrowResourceNotFoundWhenFindingByOrderIdAndTicketDoesNotExist() {

    assertThatThrownBy(() -> kitchenService.findByOrderIdWithItems(TENANT_ID, ORDER_ID))
        .hasMessage("Ticket orderId: %s not found".formatted(ORDER_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldThrowResourceNotFoundWhenFindingByOrderIdFromAnotherTenant() {

    kitchenService.createTicketFromOrder(createTicketCommand(TENANT_ID, ORDER_ID));
    flushAndClear();

    assertThatThrownBy(() -> kitchenService.findByOrderIdWithItems(OTHER_TENANT_ID, ORDER_ID))
        .hasMessage("Ticket orderId: %s not found".formatted(ORDER_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private CreateTicketCommand createTicketCommand() {
    return createTicketCommand(TENANT_ID, ORDER_ID);
  }

  private CreateTicketCommand createTicketCommand(UUID tenantId, UUID orderId) {
    return new CreateTicketCommand(tenantId, orderId, List.of(createTicketItemCommand()));
  }

  private CreateTicketItemCommand createTicketItemCommand() {
    return new CreateTicketItemCommand(PRODUCT_ID, PRODUCT_NAME, VALID_QUANTITY);
  }
}
