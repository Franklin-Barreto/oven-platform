package br.com.f2e.ovenplatform.orders.infrastructure.outbox;

import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.AGGREGATE_TYPE;
import static br.com.f2e.ovenplatform.shared.application.event.OrderEventConstants.ORDER_CREATED_EVENT;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.orders.application.CreateOrderCommand;
import br.com.f2e.ovenplatform.orders.application.CreateOrderItemCommand;
import br.com.f2e.ovenplatform.orders.application.CustomerDeliveryInfoProvider;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.application.OrderableProduct;
import br.com.f2e.ovenplatform.orders.application.OrderableProductProvider;
import br.com.f2e.ovenplatform.orders.application.PaymentInfo;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.shared.application.event.payload.order.OrderCreatedPayload;
import br.com.f2e.ovenplatform.shared.application.outbox.OutboxEventRepository;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import br.com.f2e.ovenplatform.shared.domain.outbox.OutboxEventStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
class OutboxOrderCreatedEventPublisherIntegrationTest {

  private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);
  private static final UUID TENANT_ID = UUID.fromString("0a8bd860-2951-44c9-83be-ed9b54d5123e");
  private static final UUID PRODUCT_ID = UUID.fromString("6937075f-cdb1-44e5-9931-e004920b7ea2");

  @Autowired private OrderService orderService;
  @Autowired private OutboxEventRepository outboxEventRepository;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactionManager;

  @Value("${oven.kafka.topics.orders}")
  private String orderTopic;

  @MockitoBean private OrderableProductProvider orderableProductProvider;
  @MockitoBean private CustomerDeliveryInfoProvider customerDeliveryInfoProvider;

  @BeforeEach
  void cleanPublicationsAndOrders() {
    jdbc.update("delete from event_publication");
    jdbc.update("delete from outbox_events");
    jdbc.update("delete from order_items");
    jdbc.update("delete from orders");
  }

  @Test
  void shouldCreateOneLegacyOutboxRecordFromCanonicalOrderCreatedEvent() {
    stubOrderableProduct();

    var order = orderService.createOrder(TENANT_ID, createOrderCommand());

    await()
        .atMost(ASYNC_TIMEOUT)
        .untilAsserted(
            () ->
                assertThat(
                        outboxEventRepository.findByAggregateTypeAndAggregateIdAndEventType(
                            AGGREGATE_TYPE, order.getId(), ORDER_CREATED_EVENT))
                    .isPresent());

    var outboxEvent =
        outboxEventRepository
            .findByAggregateTypeAndAggregateIdAndEventType(
                AGGREGATE_TYPE, order.getId(), ORDER_CREATED_EVENT)
            .orElseThrow();

    assertThat(legacyOutboxRecordCount(order.getId())).isOne();
    assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    assertThat(outboxEvent.getTopic()).isEqualTo(orderTopic);
    assertThat(outboxEvent.getMessageKey()).isEqualTo(order.getId().toString());
    assertThat(outboxEvent.getIdempotencyKey())
        .isEqualTo("%s:%s:%s".formatted(AGGREGATE_TYPE, order.getId(), ORDER_CREATED_EVENT));
    assertThat(outboxEvent.getPayloadVersion()).isOne();
    assertThat(outboxEvent.getAttempts()).isZero();
    assertThat(outboxEvent.getPublishedAt()).isNull();
    assertThat(outboxEvent.getLastError()).isNull();

    var payload = JsonUtils.fromJson(outboxEvent.getPayload(), OrderCreatedPayload.class);

    assertThat(payload.tenantId()).isEqualTo(TENANT_ID);
    assertThat(payload.orderId()).isEqualTo(order.getId());
    assertThat(payload.totalAmount()).isEqualByComparingTo(order.getTotalAmount());
    assertThat(payload.paymentMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(payload.paymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(payload.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.productId()).isEqualTo(PRODUCT_ID);
              assertThat(item.productName()).isEqualTo("Pizza Portuguesa");
              assertThat(item.quantity()).isEqualTo(2);
              assertThat(item.unitPrice()).isEqualByComparingTo("35.40");
            });

    await()
        .atMost(ASYNC_TIMEOUT)
        .untilAsserted(() -> assertThat(completedPublicationCount(order.getId())).isOne());
  }

  @Test
  void shouldNotPublishCanonicalOrLegacyEventWhenOrderTransactionRollsBack() {
    stubOrderableProduct();
    var orderId = new AtomicReference<UUID>();
    var transactions = new TransactionTemplate(transactionManager);

    transactions.executeWithoutResult(
        status -> {
          orderId.set(orderService.createOrder(TENANT_ID, createOrderCommand()).getId());
          status.setRollbackOnly();
        });

    await()
        .during(Duration.ofMillis(300))
        .atMost(Duration.ofSeconds(1))
        .untilAsserted(
            () -> {
              assertThat(orderRecordCount(orderId.get())).isZero();
              assertThat(publicationCount(orderId.get())).isZero();
              assertThat(legacyOutboxRecordCount(orderId.get())).isZero();
            });
  }

  private void stubOrderableProduct() {
    when(orderableProductProvider.findOrderableProducts(TENANT_ID, Set.of(PRODUCT_ID)))
        .thenReturn(
            List.of(new OrderableProduct(PRODUCT_ID, "Pizza Portuguesa", new BigDecimal("35.40"))));
  }

  private CreateOrderCommand createOrderCommand() {
    return new CreateOrderCommand(
        List.of(new CreateOrderItemCommand(PRODUCT_ID, 2)),
        new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PAID),
        OrderServiceType.COUNTER);
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
            ORDER_CREATED_EVENT),
        "Legacy outbox record count not returned for order " + orderId);
  }

  private int publicationCount(UUID orderId) {
    return requireNonNull(
        jdbc.queryForObject(
            "select count(*) from event_publication where serialized_event like ?",
            Integer.class,
            serializedEventPattern(orderId)),
        "Publication count not returned for order " + orderId);
  }

  private int completedPublicationCount(UUID orderId) {
    return requireNonNull(
        jdbc.queryForObject(
            """
            select count(*)
            from event_publication
            where listener_id = 'orders-order-created-outbox-publisher'
              and serialized_event like ?
              and status = 'COMPLETED'
            """,
            Integer.class,
            serializedEventPattern(orderId)),
        "Completed publication count not returned for order " + orderId);
  }

  private int orderRecordCount(UUID orderId) {
    return requireNonNull(
        jdbc.queryForObject("select count(*) from orders where id = ?", Integer.class, orderId),
        "Order record count not returned for order " + orderId);
  }

  private String serializedEventPattern(UUID orderId) {
    return "%" + orderId + "%";
  }
}
