package br.com.f2e.ovenplatform.shared.infrastructure.kafka;

import static br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants.FULFILLMENT_ORDER_READY_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.fulfillment.application.FulfillmentService;
import br.com.f2e.ovenplatform.fulfillment.infrastructure.kafka.KitchenTicketReadyConsumer;
import br.com.f2e.ovenplatform.fulfillment.infrastructure.outbox.OutboxFulfillmentOrderReadyEventPublisher;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.kitchen.application.KitchenTicketReadyEventPublisher;
import br.com.f2e.ovenplatform.kitchen.domain.TicketStatus;
import br.com.f2e.ovenplatform.kitchen.infrastructure.kafka.OrderCreatedKitchenTicketConsumer;
import br.com.f2e.ovenplatform.kitchen.infrastructure.persistence.JpaTicketRepositoryAdapter;
import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfoProvider;
import br.com.f2e.ovenplatform.orders.application.OrderCreatedEventPublisher;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.application.OrderableProductProvider;
import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.orders.infrastructure.kafka.FulfillmentOrderReadyConsumer;
import br.com.f2e.ovenplatform.orders.infrastructure.persistence.JpaOrderRepositoryAdapter;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import br.com.f2e.ovenplatform.payment.infrastructure.kafka.OrderCreatedPaymentConsumer;
import br.com.f2e.ovenplatform.payment.infrastructure.persistence.JpaPaymentRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.event.FulfillmentEventConstants;
import br.com.f2e.ovenplatform.shared.application.event.payload.FulfillmentOrderReadyPayload;
import br.com.f2e.ovenplatform.shared.application.event.payload.KitchenTicketReadyPayload;
import br.com.f2e.ovenplatform.shared.application.event.payload.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderCreatedItemPayload;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderCreatedPayload;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderPaymentStatus;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxService;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEvent;
import br.com.f2e.ovenplatform.shared.infrastructure.outbox.persistence.JpaOutboxEventRepository;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
  JpaPaymentRepositoryAdapter.class,
  JpaTicketRepositoryAdapter.class,
  KitchenService.class,
  KitchenTicketReadyConsumer.class,
  OrderCreatedKitchenTicketConsumer.class,
  OrderCreatedPaymentConsumer.class,
  OrderService.class,
  OutboxFulfillmentOrderReadyEventPublisher.class,
  OutboxService.class,
  PaymentService.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConsumerIdempotencyIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("120.00");
  private static final BigDecimal UNIT_PRICE = new BigDecimal("60.00");
  private static final Instant PAID_AT = Instant.parse("2026-05-12T20:18:00Z");
  private static final Instant READY_AT = Instant.parse("2026-05-12T20:30:00Z");

  @Autowired private OrderCreatedKitchenTicketConsumer kitchenConsumer;
  @Autowired private OrderCreatedPaymentConsumer paymentConsumer;
  @Autowired private KitchenTicketReadyConsumer fulfillmentConsumer;
  @Autowired private FulfillmentOrderReadyConsumer orderConsumer;
  @Autowired private OrderService orderService;

  @MockitoBean private Clock clock;
  @MockitoBean private KitchenTicketReadyEventPublisher kitchenTicketReadyEventPublisher;
  @MockitoBean private OrderableProductProvider orderableProductProvider;
  @MockitoBean private CustomerDeliveryInfoProvider customerDeliveryInfoProvider;
  @MockitoBean private OrderCreatedEventPublisher orderCreatedEventPublisher;

  @Test
  void shouldIgnoreSequentialDuplicateOrderCreatedDeliveryForKitchenTicketCreation() {
    var orderId = UUID.randomUUID();
    var payload = orderCreatedPayload(orderId);
    var json = JsonUtils.toJson(payload);

    kitchenConsumer.on(json);
    kitchenConsumer.on(json);

    assertThat(countKitchenTickets(orderId)).isOne();
    assertThat(findKitchenTicketStatus(orderId)).isEqualTo(TicketStatus.RECEIVED);
  }

  @Test
  void shouldIgnoreConcurrentDuplicateOrderCreatedDeliveryForKitchenTicketCreation()
      throws Exception {
    var orderId = UUID.randomUUID();
    var json = JsonUtils.toJson(orderCreatedPayload(orderId));

    runConcurrently(() -> kitchenConsumer.on(json), () -> kitchenConsumer.on(json));

    assertThat(countKitchenTickets(orderId)).isOne();
    assertThat(findKitchenTicketStatus(orderId)).isEqualTo(TicketStatus.RECEIVED);
  }

  @Test
  void shouldIgnoreSequentialDuplicateOrderCreatedDeliveryForPaymentCreation() {
    when(clock.instant()).thenReturn(PAID_AT);
    var orderId = UUID.randomUUID();
    var payload = orderCreatedPayload(orderId);
    var json = JsonUtils.toJson(payload);

    paymentConsumer.on(json);
    paymentConsumer.on(json);

    assertThat(countPayments(orderId)).isOne();
    assertThat(findPaymentStatus(orderId)).isEqualTo(PaymentStatus.PAID);
  }

  @Test
  void shouldIgnoreConcurrentDuplicateOrderCreatedDeliveryForPaymentCreation() throws Exception {
    when(clock.instant()).thenReturn(PAID_AT);
    var orderId = UUID.randomUUID();
    var json = JsonUtils.toJson(orderCreatedPayload(orderId));

    runConcurrently(() -> paymentConsumer.on(json), () -> paymentConsumer.on(json));

    assertThat(countPayments(orderId)).isOne();
    assertThat(findPaymentStatus(orderId)).isEqualTo(PaymentStatus.PAID);
  }

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

  private OrderCreatedPayload orderCreatedPayload(UUID orderId) {
    return new OrderCreatedPayload(
        TENANT_ID,
        orderId,
        TOTAL_AMOUNT,
        PaymentMethod.CASH,
        OrderPaymentStatus.PAID,
        List.of(new OrderCreatedItemPayload(PRODUCT_ID, "Pizza Portuguesa", 2, UNIT_PRICE)));
  }

  private void runConcurrently(ThrowingRunnable first, ThrowingRunnable second) throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);

    try (var executor = Executors.newFixedThreadPool(2)) {
      List<Future<Void>> futures =
          List.of(
              executor.submit(concurrentTask(first, ready, start)),
              executor.submit(concurrentTask(second, ready, start)));

      ready.await();
      start.countDown();

      for (var future : futures) {
        future.get();
      }
    }
  }

  private Callable<Void> concurrentTask(
      ThrowingRunnable runnable, CountDownLatch ready, CountDownLatch start) {
    return () -> {
      ready.countDown();
      start.await();
      runnable.run();
      return null;
    };
  }

  private long countKitchenTickets(UUID orderId) {
    return entityManager
        .createQuery(
            """
            select count(ticket)
            from Ticket ticket
            where ticket.tenantId = :tenantId
              and ticket.orderId = :orderId
            """,
            Long.class)
        .setParameter("tenantId", TENANT_ID)
        .setParameter("orderId", orderId)
        .getSingleResult();
  }

  private TicketStatus findKitchenTicketStatus(UUID orderId) {
    return entityManager
        .createQuery(
            """
            select ticket.status
            from Ticket ticket
            where ticket.tenantId = :tenantId
              and ticket.orderId = :orderId
            """,
            TicketStatus.class)
        .setParameter("tenantId", TENANT_ID)
        .setParameter("orderId", orderId)
        .getSingleResult();
  }

  private long countPayments(UUID orderId) {
    return entityManager
        .createQuery(
            """
            select count(payment)
            from Payment payment
            where payment.tenantId = :tenantId
              and payment.orderId = :orderId
            """,
            Long.class)
        .setParameter("tenantId", TENANT_ID)
        .setParameter("orderId", orderId)
        .getSingleResult();
  }

  private PaymentStatus findPaymentStatus(UUID orderId) {
    return entityManager
        .createQuery(
            """
            select payment.status
            from Payment payment
            where payment.tenantId = :tenantId
              and payment.orderId = :orderId
            """,
            PaymentStatus.class)
        .setParameter("tenantId", TENANT_ID)
        .setParameter("orderId", orderId)
        .getSingleResult();
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

  @FunctionalInterface
  private interface ThrowingRunnable {

    void run() throws Exception;
  }
}
