package br.com.f2e.ovenplatform.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.orders.application.event.OrderPaymentMarkedAsPaidEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPaymentMethod;
import br.com.f2e.ovenplatform.orders.application.event.OrderPaymentStatus;
import br.com.f2e.ovenplatform.orders.application.event.OrderPlacedEvent;
import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.orders.infrastructure.persistence.JpaOrderRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@DataJpaTest
@ActiveProfiles("test")
@Import({OrderService.class, JpaOrderRepositoryAdapter.class})
@EnableJpaAuditing
@RecordApplicationEvents
class OrderServiceIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");

  private static final UUID ANOTHER_TENANT_ID =
      UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecd");
  private static final String PRODUCT_NAME = "Pizza Portuguesa";

  private record OrderItemFixture(
      CreateOrderItemCommand command, OrderableProduct orderableProduct) {}

  @Autowired private OrderService orderService;
  @Autowired private EntityManager entityManager;

  @SuppressWarnings("unused")
  @MockitoBean
  private OrderableProductProvider orderableProductProvider;

  @SuppressWarnings("unused")
  @MockitoBean
  private Clock clock;

  @Autowired private ApplicationEvents applicationEvents;

  @Test
  void shouldCreateOrderWithItemsUsingOrderableProductPrices() {
    var fixtures = createOrderItemFixtures();
    var command = createOrderCommand(fixtures);
    var orderableProducts = createOrderableProducts(fixtures);
    var productIds = extractProductIds(command);

    when(orderableProductProvider.findOrderableProducts(TENANT_ID, productIds))
        .thenReturn(orderableProducts);

    var order = orderService.createOrder(TENANT_ID, command);

    assertThat(order.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(order.getId()).isNotNull();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(order.getItems()).hasSize(fixtures.size());
    assertThat(order.getCreatedAt()).isNotNull();
    assertThat(order.getUpdatedAt()).isNotNull();

    assertOrderItemsMatchFixtures(order, fixtures);

    verify(orderableProductProvider).findOrderableProducts(TENANT_ID, productIds);

    var orderPlacedEvents = applicationEvents.stream(OrderPlacedEvent.class).toList();

    assertThat(orderPlacedEvents).hasSize(1);

    var orderPlacedEvent = orderPlacedEvents.getFirst();

    assertThat(orderPlacedEvent.orderId()).isEqualTo(order.getId());
    assertThat(orderPlacedEvent.paymentMethod()).isEqualTo(OrderPaymentMethod.CASH);
    assertThat(orderPlacedEvent.paymentStatus()).isEqualTo(OrderPaymentStatus.PAID);
    assertThat(orderPlacedEvent.totalAmount()).isEqualByComparingTo(order.getTotalAmount());
  }

  @Test
  void shouldSaveAndFindOrderWithItems() {
    var order = createOrderWithItems(TENANT_ID, 1);

    var savedOrder = orderService.save(order);

    flushAndClear();

    var foundOrder = orderService.findOrderWithItems(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder).isPresent();

    var persistedOrder = foundOrder.get();

    assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(persistedOrder.getTotalAmount()).isEqualByComparingTo("1.00");
    assertThat(persistedOrder.getItems()).hasSize(1);

    var item = persistedOrder.getItems().getFirst();

    assertThat(item.getProductId()).isEqualTo(order.getItems().getFirst().getProductId());
    assertThat(item.getProductName()).isEqualTo(PRODUCT_NAME);
    assertThat(item.getQuantity()).isEqualTo(1);
    assertThat(item.getUnitPrice()).isEqualByComparingTo("1.00");
    assertThat(item.getSubtotal()).isEqualByComparingTo("1.00");
  }

  @Test
  void shouldKeepOrderItemSnapshotAfterProductInformationChanges() {
    var productId = UUID.randomUUID();
    var originalProductName = "Pizza Calabresa";
    var updatedProductName = "Pizza Calabresa Especial";
    var originalPrice = new BigDecimal("42.00");
    var updatedPrice = new BigDecimal("55.00");
    var paymentInfo = new PaymentInfo(OrderPaymentMethod.CASH, OrderPaymentStatus.PAID);
    var command =
        new CreateOrderCommand(List.of(new CreateOrderItemCommand(productId, 2)), paymentInfo);

    when(orderableProductProvider.findOrderableProducts(TENANT_ID, Set.of(productId)))
        .thenReturn(List.of(new OrderableProduct(productId, originalProductName, originalPrice)))
        .thenReturn(List.of(new OrderableProduct(productId, updatedProductName, updatedPrice)));

    var savedOrder = orderService.createOrder(TENANT_ID, command);

    flushAndClear();

    orderService.createOrder(TENANT_ID, command);

    flushAndClear();

    var foundOrder = orderService.findOrderWithItems(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder).isPresent();

    var item = foundOrder.get().getItems().getFirst();

    assertThat(item.getProductName()).isEqualTo(originalProductName);
    assertThat(item.getUnitPrice()).isEqualByComparingTo(originalPrice);
    assertThat(item.getSubtotal()).isEqualByComparingTo("84.00");
  }

  @Test
  void shouldFindOrderByIdAndTenantId() {
    var savedOrder = orderService.save(createOrderWithItems(TENANT_ID, 1));

    flushAndClear();

    var foundOrder = orderService.findOrder(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder)
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getId()).isEqualTo(savedOrder.getId());
              assertThat(order.getTenantId()).isEqualTo(TENANT_ID);
              assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
              assertThat(order.getTotalAmount()).isEqualByComparingTo("1.00");
            });
  }

  @Test
  void shouldReturnEmptyWhenOrderDoesNotExist() {
    var unknownOrderId = UUID.randomUUID();

    var order = orderService.findOrder(TENANT_ID, unknownOrderId);

    assertThat(order).isEmpty();
  }

  @Test
  void shouldReturnEmptyWhenOrderBelongsToAnotherTenant() {
    var order = orderService.save(createOrderWithItems(TENANT_ID, 1));

    flushAndClear();

    assertThat(orderService.findOrder(ANOTHER_TENANT_ID, order.getId())).isEmpty();
  }

  @Test
  void shouldListOrdersByTenant() {
    orderService.save(createOrderWithItems(TENANT_ID, 1));
    orderService.save(createOrderWithItems(TENANT_ID, 2));
    orderService.save(createOrderWithItems(ANOTHER_TENANT_ID, 1));

    flushAndClear();

    var orders = orderService.listOrders(TENANT_ID);

    assertThat(orders).hasSize(2).extracting(Order::getTenantId).containsOnly(TENANT_ID);
  }

  @Test
  void shouldNotListOrdersFromAnotherTenant() {
    orderService.save(createOrderWithItems(TENANT_ID, 1));

    flushAndClear();

    assertThat(orderService.listOrders(ANOTHER_TENANT_ID)).isEmpty();
  }

  @Test
  void shouldThrowExceptionWhenCatalogReturnNullProduct() {
    var productId = UUID.randomUUID();
    var paymentInfo = new PaymentInfo(OrderPaymentMethod.CASH, OrderPaymentStatus.PAID);
    CreateOrderCommand orderCommand =
        new CreateOrderCommand(List.of(new CreateOrderItemCommand(productId, 1)), paymentInfo);
    assertThatThrownBy(() -> orderService.createOrder(TENANT_ID, orderCommand))
        .isInstanceOf(ProductNotAvailableForOrderingException.class)
        .hasMessage("Product is not available for ordering: %s".formatted(productId));
  }

  @Test
  void shouldMarkOrderAsReady() {
    var occurredAt = Instant.parse("2026-05-12T20:18:00Z");
    when(clock.instant()).thenReturn(occurredAt);

    var savedOrder = orderService.save(createOrderWithItems(TENANT_ID, 1));

    flushAndClear();

    orderService.markAsReady(TENANT_ID, savedOrder.getId());

    flushAndClear();

    var foundOrder = orderService.findOrder(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder)
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
              assertThat(order.getReadyAt()).isEqualTo(occurredAt);
              assertThat(order.getDeliveredAt()).isNull();
              assertThat(order.getCancelledAt()).isNull();
            });
  }

  @Test
  void shouldThrowResourceNotFoundWhenMarkingUnknownOrderAsReady() {
    var orderId = UUID.randomUUID();

    assertThatThrownBy(() -> orderService.markAsReady(TENANT_ID, orderId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Order id: %s not found".formatted(orderId));

    verifyNoInteractions(clock);
  }

  @Test
  void shouldMarkOrderAsDelivered() {

    var readyAt = Instant.parse("2026-05-12T20:18:00Z");
    var deliveredAt = Instant.parse("2026-05-12T20:30:00Z");

    when(clock.instant()).thenReturn(readyAt, deliveredAt);

    var savedOrder = createReadyOrder();

    flushAndClear();

    orderService.markAsDelivered(TENANT_ID, savedOrder.getId());

    flushAndClear();

    var foundOrder = orderService.findOrder(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder)
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
              assertThat(order.getReadyAt()).isEqualTo(readyAt);
              assertThat(order.getDeliveredAt()).isEqualTo(deliveredAt);
              assertThat(order.getCancelledAt()).isNull();
            });
  }

  @Test
  void shouldMarkOrderAsCancelled() {

    var occurredAt = Instant.parse("2026-05-12T20:18:00Z");
    when(clock.instant()).thenReturn(occurredAt);

    var savedOrder = orderService.save(createOrderWithItems(TENANT_ID, 1));

    flushAndClear();

    orderService.cancel(TENANT_ID, savedOrder.getId());

    flushAndClear();

    var foundOrder = orderService.findOrder(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder)
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
              assertThat(order.getReadyAt()).isNull();
              assertThat(order.getDeliveredAt()).isNull();
              assertThat(order.getCancelledAt()).isEqualTo(occurredAt);
            });
  }

  @Test
  void shouldPublishEventWhenMarkingOrderPaymentAsPaid() {
    var savedOrder = orderService.save(createOrderWithItems(TENANT_ID, 1));

    flushAndClear();

    orderService.markPaymentAsPaid(savedOrder.getTenantId(), savedOrder.getId());

    flushAndClear();

    var markedAsPaidEvents = applicationEvents.stream(OrderPaymentMarkedAsPaidEvent.class).toList();

    assertThat(markedAsPaidEvents).hasSize(1);

    var paidEvent = markedAsPaidEvents.getFirst();

    assertThat(paidEvent.orderId()).isEqualTo(savedOrder.getId());
    assertThat(paidEvent.tenantId()).isEqualTo(savedOrder.getTenantId());
  }

  @Test
  void shouldNotPublishEventWhenOrderDoesNotExist() {
    var orderId = UUID.randomUUID();

    assertThatThrownBy(() -> orderService.markPaymentAsPaid(TENANT_ID, orderId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Order id: %s not found".formatted(orderId));

    var events = applicationEvents.stream(OrderPaymentMarkedAsPaidEvent.class).toList();

    assertThat(events).isEmpty();
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  private Order createOrderWithItems(UUID tenantId, int itemQuantity) {
    var order = new Order(tenantId);
    order.addItem(UUID.randomUUID(), PRODUCT_NAME, itemQuantity, BigDecimal.ONE);
    return order;
  }

  private List<OrderItemFixture> createOrderItemFixtures() {
    var itemCount = 3;
    var fixtures = new ArrayList<OrderItemFixture>(itemCount);

    for (int i = 1; i <= itemCount; i++) {
      var productId = UUID.randomUUID();
      var productName = "Product %d".formatted(i);
      var unitPrice = BigDecimal.valueOf(i);

      fixtures.add(
          new OrderItemFixture(
              new CreateOrderItemCommand(productId, i),
              new OrderableProduct(productId, productName, unitPrice)));
    }

    return fixtures;
  }

  private CreateOrderCommand createOrderCommand(List<OrderItemFixture> fixtures) {
    var paymentInfo = new PaymentInfo(OrderPaymentMethod.CASH, OrderPaymentStatus.PAID);
    return new CreateOrderCommand(
        fixtures.stream().map(OrderItemFixture::command).toList(), paymentInfo);
  }

  private List<OrderableProduct> createOrderableProducts(List<OrderItemFixture> fixtures) {
    return fixtures.stream().map(OrderItemFixture::orderableProduct).toList();
  }

  private Set<UUID> extractProductIds(CreateOrderCommand command) {
    return command.items().stream()
        .map(CreateOrderItemCommand::productId)
        .collect(Collectors.toSet());
  }

  private Order createReadyOrder() {
    var savedOrder = orderService.save(createOrderWithItems(TENANT_ID, 1));
    orderService.markAsReady(TENANT_ID, savedOrder.getId());
    return savedOrder;
  }

  private void assertOrderItemsMatchFixtures(Order order, List<OrderItemFixture> fixtures) {
    var expectedItemsByProductId =
        fixtures.stream()
            .collect(
                Collectors.toMap(fixture -> fixture.command().productId(), fixture -> fixture));

    order
        .getItems()
        .forEach(
            item -> {
              var fixture = expectedItemsByProductId.get(item.getProductId());

              assertThat(fixture).isNotNull();
              assertThat(item.getQuantity()).isEqualTo(fixture.command().quantity());
              assertThat(item.getProductName()).isEqualTo(fixture.orderableProduct().productName());
              assertThat(item.getUnitPrice())
                  .isEqualByComparingTo(fixture.orderableProduct().unitPrice());
              assertThat(item.getSubtotal())
                  .isEqualByComparingTo(
                      fixture
                          .orderableProduct()
                          .unitPrice()
                          .multiply(BigDecimal.valueOf(fixture.command().quantity())));
            });
  }
}
