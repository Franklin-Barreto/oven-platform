package br.com.f2e.ovenplatform.orders.infrastructure.event;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.fulfillment.application.event.FulfillmentOrderMarkedAsReadyEvent;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.orders.application.CreateOrderCommand;
import br.com.f2e.ovenplatform.orders.application.CreateOrderItemCommand;
import br.com.f2e.ovenplatform.orders.application.OrderRepository;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.application.OrderableProduct;
import br.com.f2e.ovenplatform.orders.application.OrderableProductProvider;
import br.com.f2e.ovenplatform.orders.application.OrderableProductSelection;
import br.com.f2e.ovenplatform.orders.application.PaymentInfo;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedItem;
import br.com.f2e.ovenplatform.orders.application.event.OrderReadyForPreparationEvent;
import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentProcessingMode;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
    properties = {
      "jwt.secret=0123456789012345678901234567890123456789012345678901234567890123",
      "oven.events.publication.maintenance.enabled=false"
    })
@Import(PostgresTestContainerConfiguration.class)
@Execution(ExecutionMode.SAME_THREAD)
class OrdersReadinessModuleEventsIntegrationTest {

  private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final BigDecimal UNIT_PRICE = new BigDecimal("60.00");

  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private KitchenService kitchenService;
  @Autowired private OrderService orderService;
  @Autowired private OrderRepository orderRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactionManager;

  @MockitoBean private OrderableProductProvider orderableProductProvider;

  @BeforeEach
  void cleanPublicationsPaymentsTicketsAndOrders() {
    jdbc.update("delete from event_publication");
    jdbc.update("delete from payments");
    jdbc.update("delete from kitchen_ticket_items");
    jdbc.update("delete from kitchen_tickets");
    jdbc.update("delete from order_items");
    jdbc.update("delete from orders");
  }

  @Test
  void shouldPropagateOrderCreationThroughKitchenReadiness() {
    when(orderableProductProvider.findOrderableProducts(
            TENANT_ID, List.of(new OrderableProductSelection(PRODUCT_ID, null))))
        .thenReturn(List.of(new OrderableProduct(PRODUCT_ID, "Pizza Portuguesa", UNIT_PRICE)));

    var order = orderService.createOrder(TENANT_ID, createOrderCommand());

    await()
        .atMost(ASYNC_TIMEOUT)
        .untilAsserted(() -> assertThat(ticketCount(order.getId())).isOne());
    var ticket = kitchenService.findByOrderIdWithItems(TENANT_ID, order.getId());

    kitchenService.startPreparation(TENANT_ID, ticket.getId());
    kitchenService.markAsReady(TENANT_ID, ticket.getId());

    await()
        .atMost(ASYNC_TIMEOUT)
        .untilAsserted(
            () -> {
              var readyOrder = orderService.findOrder(TENANT_ID, order.getId()).orElseThrow();
              assertThat(readyOrder.getStatus()).isEqualTo(OrderStatus.READY);
              assertThat(readyOrder.getReadyAt()).isEqualTo(ticketReadyAt(ticket.getId()));
            });

    awaitCompletedPublication("fulfillment-kitchen-ticket-ready-listener", order.getId(), 1);
    awaitCompletedPublication("orders-fulfillment-order-ready-listener", order.getId(), 1);
  }

  @Test
  void shouldPreserveFirstReadyAtWhenFulfillmentEventIsRedelivered() {
    var order = orderService.save(new Order(TENANT_ID, OrderServiceType.COUNTER));
    var readyAt = Instant.parse("2026-07-14T19:01:16.681Z");
    var repeatedReadyAt = Instant.parse("2026-07-14T19:15:00Z");

    publishInTransaction(new FulfillmentOrderMarkedAsReadyEvent(TENANT_ID, order.getId(), readyAt));
    awaitCompletedPublication("orders-fulfillment-order-ready-listener", order.getId(), 1);

    publishInTransaction(
        new FulfillmentOrderMarkedAsReadyEvent(TENANT_ID, order.getId(), repeatedReadyAt));
    awaitCompletedPublication("orders-fulfillment-order-ready-listener", order.getId(), 2);

    var readyOrder = orderService.findOrder(TENANT_ID, order.getId()).orElseThrow();

    assertThat(readyOrder.getStatus()).isEqualTo(OrderStatus.READY);
    assertThat(readyOrder.getReadyAt()).isEqualTo(readyAt);
  }

  @Test
  void shouldPersistOneReadinessPublicationForConcurrentReleases() throws Exception {
    var orderId =
        inTransaction(
            () -> {
              var order = new Order(TENANT_ID, OrderServiceType.COUNTER);
              order.addSimpleItem(PRODUCT_ID, "Pizza Portuguesa", 1, UNIT_PRICE);
              return orderRepository.save(order).getId();
            });
    var barrier = new CyclicBarrier(2);

    var outcomes =
        executeConcurrently(
            () -> {
              var order =
                  orderRepository.findByIdAndTenantIdWithItems(orderId, TENANT_ID).orElseThrow();
              awaitBothTransactions(barrier);
              order.releaseForPreparation(Instant.parse("2026-08-03T12:00:00Z"));
              eventPublisher.publishEvent(
                  new OrderReadyForPreparationEvent(
                      TENANT_ID,
                      orderId,
                      Instant.parse("2026-08-03T12:00:00Z"),
                      order.getItems().stream().map(OrderPlacedItem::from).toList()));
            });

    assertThat(outcomes).containsExactlyInAnyOrder(true, false);
    awaitCompletedPublication("kitchen-order-ready-for-preparation-listener", orderId, 1);
  }

  private CreateOrderCommand createOrderCommand() {
    return new CreateOrderCommand(
        List.of(new CreateOrderItemCommand(PRODUCT_ID, 2)),
        new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PAID, PaymentProcessingMode.MANUAL),
        OrderServiceType.COUNTER);
  }

  private void publishInTransaction(FulfillmentOrderMarkedAsReadyEvent event) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(_ -> eventPublisher.publishEvent(event));
  }

  private List<Boolean> executeConcurrently(Runnable transition) throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      Future<Boolean> first = executor.submit(() -> executeTransition(transition));
      Future<Boolean> second = executor.submit(() -> executeTransition(transition));
      return List.of(first.get(), second.get());
    }
  }

  private boolean executeTransition(Runnable transition) {
    try {
      inTransaction(
          () -> {
            transition.run();
            entityManager.flush();
            return null;
          });
      return true;
    } catch (OptimisticLockException | OptimisticLockingFailureException _) {
      return false;
    }
  }

  private <T> T inTransaction(Supplier<T> operation) {
    return new TransactionTemplate(transactionManager).execute(_ -> operation.get());
  }

  private void awaitBothTransactions(CyclicBarrier barrier) {
    try {
      barrier.await();
    } catch (Exception exception) {
      throw new IllegalStateException("Could not synchronize concurrent transactions", exception);
    }
  }

  private Instant ticketReadyAt(UUID ticketId) {
    return kitchenService.findById(TENANT_ID, ticketId).getReadyAt();
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

  private void awaitCompletedPublication(String listenerId, UUID orderId, int expectedCount) {
    await()
        .atMost(ASYNC_TIMEOUT)
        .untilAsserted(
            () ->
                assertThat(completedPublicationCount(listenerId, orderId))
                    .isEqualTo(expectedCount));
  }

  private int completedPublicationCount(String listenerId, UUID orderId) {
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
            serializedEventPattern(orderId)),
        "Completed publication count not returned for order " + orderId);
  }

  private String serializedEventPattern(UUID orderId) {
    return "%" + orderId + "%";
  }
}
