package br.com.f2e.ovenplatform.payment.infrastructure.event;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.orders.application.CreateOrderCommand;
import br.com.f2e.ovenplatform.orders.application.CreateOrderItemCommand;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.application.OrderableProduct;
import br.com.f2e.ovenplatform.orders.application.OrderableProductProvider;
import br.com.f2e.ovenplatform.orders.application.PaymentInfo;
import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedItem;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.PostgresTestContainerConfiguration;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
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
class PaymentModuleEventsIntegrationTest {

  private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final BigDecimal UNIT_PRICE = new BigDecimal("60.00");
  private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("120.00");

  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private OrderService orderService;
  @Autowired private PaymentService paymentService;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactionManager;

  @MockitoBean private OrderableProductProvider orderableProductProvider;

  @BeforeEach
  void cleanPublicationsOrdersTicketsAndPayments() {
    jdbc.update("delete from event_publication");
    jdbc.update("delete from payments");
    jdbc.update("delete from kitchen_ticket_items");
    jdbc.update("delete from kitchen_tickets");
    jdbc.update("delete from order_items");
    jdbc.update("delete from orders");
  }

  @Test
  void shouldRegisterPaymentWhenOrderIsCreated() {
    when(orderableProductProvider.findOrderableProducts(TENANT_ID, Set.of(PRODUCT_ID)))
        .thenReturn(List.of(new OrderableProduct(PRODUCT_ID, "Pizza Portuguesa", UNIT_PRICE)));

    var order = orderService.createOrder(TENANT_ID, createOrderCommand());

    awaitPaymentAndConsumers(order.getId(), 1);

    var payment = paymentService.findByTenantIdAndOrderId(TENANT_ID, order.getId());

    assertThat(payment.getAmount()).isEqualByComparingTo(TOTAL_AMOUNT);
    assertThat(payment.getMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(payment.getPaidAt()).isNotNull();
  }

  @Test
  void shouldRegisterOnePaymentWhenCanonicalEventIsRedelivered() {
    var event = orderCreatedEvent();

    publishInTransaction(event);
    awaitPaymentAndConsumers(ORDER_ID, 1);

    publishInTransaction(event);
    awaitPaymentAndConsumers(ORDER_ID, 2);

    assertThat(paymentCount(ORDER_ID)).isOne();
    assertThat(paymentService.findByTenantIdAndOrderId(TENANT_ID, ORDER_ID).getStatus())
        .isEqualTo(PaymentStatus.PAID);
  }

  private CreateOrderCommand createOrderCommand() {
    return new CreateOrderCommand(
        List.of(new CreateOrderItemCommand(PRODUCT_ID, 2)),
        new PaymentInfo(
            br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod.CASH,
            br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus.PAID),
        OrderServiceType.COUNTER);
  }

  private OrderCreatedEvent orderCreatedEvent() {
    return new OrderCreatedEvent(
        TENANT_ID,
        ORDER_ID,
        br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod.CASH,
        br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus.PAID,
        TOTAL_AMOUNT,
        List.of(new OrderPlacedItem(PRODUCT_ID, "Pizza Portuguesa", 2, UNIT_PRICE)));
  }

  private void publishInTransaction(OrderCreatedEvent event) {
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(_ -> eventPublisher.publishEvent(event));
  }

  private void awaitPaymentAndConsumers(UUID orderId, int expectedPublicationCount) {
    await().atMost(ASYNC_TIMEOUT).untilAsserted(() -> assertThat(paymentCount(orderId)).isOne());
    awaitCompletedPublication("payment-order-created-listener", orderId, expectedPublicationCount);
    awaitCompletedPublication("kitchen-order-created-listener", orderId, expectedPublicationCount);
  }

  private void awaitCompletedPublication(String listenerId, UUID orderId, int expectedCount) {
    await()
        .atMost(ASYNC_TIMEOUT)
        .untilAsserted(
            () ->
                assertThat(completedPublicationCount(listenerId, orderId))
                    .isEqualTo(expectedCount));
  }

  private int paymentCount(UUID orderId) {
    return requireNonNull(
        jdbc.queryForObject(
            "select count(*) from payments where tenant_id = ? and order_id = ?",
            Integer.class,
            TENANT_ID,
            orderId),
        "Payment count not returned for order " + orderId);
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
