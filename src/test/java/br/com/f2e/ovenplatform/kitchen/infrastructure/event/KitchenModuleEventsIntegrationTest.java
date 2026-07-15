package br.com.f2e.ovenplatform.kitchen.infrastructure.event;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.kitchen.domain.TicketStatus;
import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedItem;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
      "oven.events.publication.maintenance.enabled=false"
    })
@Import(PostgresTestContainerConfiguration.class)
class KitchenModuleEventsIntegrationTest {

  private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");

  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private KitchenService kitchenService;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactionManager;

  @BeforeEach
  void cleanPublicationsAndTickets() {
    jdbc.update("delete from event_publication");
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

  private int ticketCount(UUID orderId) {
    return requireNonNull(
        jdbc.queryForObject(
            "select count(*) from kitchen_tickets where tenant_id = ? and order_id = ?",
            Integer.class,
            TENANT_ID,
            orderId),
        "Kitchen ticket count not returned for order " + orderId);
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
