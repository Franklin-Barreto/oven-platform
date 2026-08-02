package br.com.f2e.ovenplatform.orders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.orders.application.event.OrderCreatedEvent;
import br.com.f2e.ovenplatform.orders.application.event.OrderPaymentMarkedAsPaidEvent;
import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderItem;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.orders.infrastructure.persistence.JpaOrderRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({OrderService.class, JpaOrderRepositoryAdapter.class})
@RecordApplicationEvents
class OrderServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");

  private static final UUID ANOTHER_TENANT_ID =
      UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID CUSTOMER_ID = UUID.fromString("c6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID CUSTOMER_ADDRESS_ID =
      UUID.fromString("d6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final String PRODUCT_NAME = "Pizza Portuguesa";

  private record OrderItemFixture(
      CreateOrderItemCommand command, OrderableProduct orderableProduct) {}

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private ApplicationEvents applicationEvents;

  @Autowired private OrderService orderService;

  @SuppressWarnings("unused")
  @MockitoBean
  private OrderableProductProvider orderableProductProvider;

  @SuppressWarnings("unused")
  @MockitoBean
  private CustomerDeliveryInfoProvider customerDeliveryInfoProvider;

  @SuppressWarnings("unused")
  @MockitoBean
  private Clock clock;

  @Test
  void shouldCreateOrderWithItemsUsingOrderableProductPrices() {
    var fixtures = createOrderItemFixtures();
    var command = createOrderCommand(fixtures);
    var orderableProducts = createOrderableProducts(fixtures);
    var selections = extractSelections(command);

    when(orderableProductProvider.findOrderableProducts(TENANT_ID, selections))
        .thenReturn(orderableProducts);

    var order = orderService.createOrder(TENANT_ID, command);

    assertThat(order.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(order.getId()).isNotNull();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(order.getServiceType()).isEqualTo(OrderServiceType.COUNTER);
    assertThat(order.getItems()).hasSize(fixtures.size());
    assertThat(order.getCreatedAt()).isNotNull();
    assertThat(order.getUpdatedAt()).isNotNull();

    assertOrderItemsMatchFixtures(order, fixtures);

    verify(orderableProductProvider).findOrderableProducts(TENANT_ID, selections);
    verifyNoInteractions(customerDeliveryInfoProvider);

    var orderCreatedEvents = applicationEvents.stream(OrderCreatedEvent.class).toList();

    assertThat(orderCreatedEvents).hasSize(1);

    var orderCreatedEvent = orderCreatedEvents.getFirst();

    assertThat(orderCreatedEvent.orderId()).isEqualTo(order.getId());
    assertThat(orderCreatedEvent.paymentMethod()).isEqualTo(PaymentMethod.CASH);
    assertThat(orderCreatedEvent.paymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(orderCreatedEvent.totalAmount()).isEqualByComparingTo(order.getTotalAmount());
    assertThat(orderCreatedEvent.items()).hasSize(fixtures.size());
    assertThat(orderCreatedEvent.items())
        .allSatisfy(
            item -> {
              var fixture =
                  fixtures.stream()
                      .filter(candidate -> candidate.command().productId().equals(item.productId()))
                      .findFirst()
                      .orElseThrow();

              assertThat(item.productName()).isEqualTo(fixture.orderableProduct().productName());
              assertThat(item.variantId()).isEqualTo(fixture.orderableProduct().variantId());
              assertThat(item.variantName()).isEqualTo(fixture.orderableProduct().variantName());
              assertThat(item.quantity()).isEqualTo(fixture.command().quantity());
              assertThat(item.unitPrice())
                  .isEqualByComparingTo(fixture.orderableProduct().unitPrice());
            });
  }

  @Test
  void shouldCreateDistinctItemsForVariantsOfTheSameProduct() {
    var productId = UUID.randomUUID();
    var largeVariantId = UUID.randomUUID();
    var mediumVariantId = UUID.randomUUID();
    var command =
        new CreateOrderCommand(
            List.of(
                new CreateOrderItemCommand(productId, largeVariantId, 1),
                new CreateOrderItemCommand(productId, mediumVariantId, 2)),
            new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PAID),
            OrderServiceType.COUNTER);
    var selections = extractSelections(command);

    when(orderableProductProvider.findOrderableProducts(TENANT_ID, selections))
        .thenReturn(
            List.of(
                new OrderableProduct(
                    productId, PRODUCT_NAME, largeVariantId, "Grande", new BigDecimal("50.00")),
                new OrderableProduct(
                    productId, PRODUCT_NAME, mediumVariantId, "Media", new BigDecimal("40.00"))));

    var savedOrder = orderService.createOrder(TENANT_ID, command);
    flushAndClear();

    var persistedOrder =
        orderService.findOrderWithItems(TENANT_ID, savedOrder.getId()).orElseThrow();

    assertThat(persistedOrder.getItems())
        .extracting(
            OrderItem::getProductId,
            OrderItem::getVariantId,
            OrderItem::getVariantName,
            OrderItem::getQuantity,
            OrderItem::getUnitPrice)
        .containsExactly(
            tuple(productId, largeVariantId, "Grande", 1, new BigDecimal("50.00")),
            tuple(productId, mediumVariantId, "Media", 2, new BigDecimal("40.00")));
    assertThat(persistedOrder.getTotalAmount()).isEqualByComparingTo("130.00");
    verify(orderableProductProvider).findOrderableProducts(TENANT_ID, selections);
  }

  @Test
  void shouldCreateDeliveryOrderWithCustomerSnapshot() {
    var fixtures = createOrderItemFixtures();
    var command = createDeliveryOrderCommand(fixtures, CUSTOMER_ID, CUSTOMER_ADDRESS_ID);
    var orderableProducts = createOrderableProducts(fixtures);
    var productIds = extractSelections(command);

    when(orderableProductProvider.findOrderableProducts(TENANT_ID, productIds))
        .thenReturn(orderableProducts);
    when(customerDeliveryInfoProvider.findCustomerDeliveryInfo(
            TENANT_ID, CUSTOMER_ID, CUSTOMER_ADDRESS_ID))
        .thenReturn(customerDeliveryInfo());

    var order = orderService.createOrder(TENANT_ID, command);

    assertThat(order.getServiceType()).isEqualTo(OrderServiceType.DELIVERY);
    assertThat(order.getDeliveryCustomerSnapshot())
        .satisfies(
            snapshot -> {
              var address = snapshot.getAddress();
              var line = address.line();
              var location = address.location();

              assertThat(snapshot.getCustomerId()).isEqualTo(CUSTOMER_ID);
              assertThat(snapshot.getCustomerName()).isEqualTo("Maria");
              assertThat(snapshot.getCustomerPhone()).isEqualTo("(11) 99999-8888");
              assertThat(address.addressId()).isEqualTo(CUSTOMER_ADDRESS_ID);
              assertThat(line.addressLine1()).isEqualTo("Rua das Flores");
              assertThat(line.number()).isEqualTo("123");
              assertThat(location.neighborhood()).isEqualTo("Centro");
              assertThat(location.city()).isEqualTo("Sao Paulo");
              assertThat(location.state()).isEqualTo("SP");
              assertThat(location.postalCode()).isEqualTo("01000-000");
            });

    verify(customerDeliveryInfoProvider)
        .findCustomerDeliveryInfo(TENANT_ID, CUSTOMER_ID, CUSTOMER_ADDRESS_ID);
  }

  @Test
  void shouldPersistDeliveryCustomerSnapshot() {
    var fixtures = createOrderItemFixtures();
    var command = createDeliveryOrderCommand(fixtures, CUSTOMER_ID, CUSTOMER_ADDRESS_ID);
    var productIds = extractSelections(command);

    when(orderableProductProvider.findOrderableProducts(TENANT_ID, productIds))
        .thenReturn(createOrderableProducts(fixtures));
    when(customerDeliveryInfoProvider.findCustomerDeliveryInfo(
            TENANT_ID, CUSTOMER_ID, CUSTOMER_ADDRESS_ID))
        .thenReturn(customerDeliveryInfo());

    var savedOrder = orderService.createOrder(TENANT_ID, command);

    flushAndClear();

    var foundOrder = orderService.findOrderWithItems(TENANT_ID, savedOrder.getId()).orElseThrow();

    assertThat(foundOrder.getDeliveryCustomerSnapshot())
        .satisfies(
            snapshot -> {
              var address = snapshot.getAddress();

              assertThat(snapshot.getCustomerId()).isEqualTo(CUSTOMER_ID);
              assertThat(address.addressId()).isEqualTo(CUSTOMER_ADDRESS_ID);
              assertThat(snapshot.getCustomerName()).isEqualTo("Maria");
              assertThat(address.line().addressLine1()).isEqualTo("Rua das Flores");
            });
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
    assertThat(persistedOrder.getServiceType()).isEqualTo(OrderServiceType.DELIVERY);
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
    var paymentInfo = new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PAID);
    var command =
        new CreateOrderCommand(
            List.of(new CreateOrderItemCommand(productId, 2)),
            paymentInfo,
            OrderServiceType.COUNTER);

    when(orderableProductProvider.findOrderableProducts(
            TENANT_ID, List.of(new OrderableProductSelection(productId, null))))
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
  void shouldKeepVariantSnapshotAfterVariantInformationChanges() {
    var productId = UUID.randomUUID();
    var variantId = UUID.randomUUID();
    var command =
        new CreateOrderCommand(
            List.of(new CreateOrderItemCommand(productId, variantId, 2)),
            new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PAID),
            OrderServiceType.COUNTER);
    var selection = new OrderableProductSelection(productId, variantId);

    when(orderableProductProvider.findOrderableProducts(TENANT_ID, List.of(selection)))
        .thenReturn(
            List.of(
                new OrderableProduct(
                    productId, PRODUCT_NAME, variantId, "Grande", new BigDecimal("42.00"))))
        .thenReturn(
            List.of(
                new OrderableProduct(
                    productId, PRODUCT_NAME, variantId, "Familia", new BigDecimal("55.00"))));

    var savedOrder = orderService.createOrder(TENANT_ID, command);
    flushAndClear();
    orderService.createOrder(TENANT_ID, command);
    flushAndClear();

    var item =
        orderService
            .findOrderWithItems(TENANT_ID, savedOrder.getId())
            .orElseThrow()
            .getItems()
            .getFirst();

    assertThat(item.getVariantId()).isEqualTo(variantId);
    assertThat(item.getVariantName()).isEqualTo("Grande");
    assertThat(item.getUnitPrice()).isEqualByComparingTo("42.00");
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
    var paymentInfo = new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PAID);
    CreateOrderCommand orderCommand =
        new CreateOrderCommand(
            List.of(new CreateOrderItemCommand(productId, 1)),
            paymentInfo,
            OrderServiceType.COUNTER);
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
              assertThat(order.getCompletedAt()).isNull();
              assertThat(order.getCancelledAt()).isNull();
            });
  }

  @Test
  void shouldMarkOrderAsReadyUsingProvidedReadyAt() {
    var readyAt = Instant.parse("2026-05-12T20:18:00Z");

    var savedOrder = orderService.save(createOrderWithItems(TENANT_ID, 1));

    flushAndClear();

    orderService.markAsReady(TENANT_ID, savedOrder.getId(), readyAt);

    flushAndClear();

    var foundOrder = orderService.findOrder(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder)
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
              assertThat(order.getReadyAt()).isEqualTo(readyAt);
              assertThat(order.getCompletedAt()).isNull();
              assertThat(order.getCancelledAt()).isNull();
            });
  }

  @Test
  void shouldPreserveOriginalReadyAtWhenMarkAsReadyIsCalledAgainWithProvidedReadyAt() {

    var readyAt = Instant.parse("2026-05-12T20:18:00Z");
    var secondReadyAt = Instant.parse("2026-05-12T22:18:00Z");

    var savedOrder = orderService.save(createOrderWithItems(TENANT_ID, 1));

    flushAndClear();

    orderService.markAsReady(TENANT_ID, savedOrder.getId(), readyAt);
    orderService.markAsReady(TENANT_ID, savedOrder.getId(), secondReadyAt);

    flushAndClear();

    var foundOrder = orderService.findOrder(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder)
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
              assertThat(order.getReadyAt()).isEqualTo(readyAt);
              assertThat(order.getCompletedAt()).isNull();
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
  void shouldCompleteOrder() {

    var readyAt = Instant.parse("2026-05-12T20:18:00Z");
    var completedAt = Instant.parse("2026-05-12T20:30:00Z");

    when(clock.instant()).thenReturn(readyAt, completedAt);

    var savedOrder = createReadyOrder();

    flushAndClear();

    orderService.complete(TENANT_ID, savedOrder.getId());

    flushAndClear();

    var foundOrder = orderService.findOrder(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder)
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
              assertThat(order.getReadyAt()).isEqualTo(readyAt);
              assertThat(order.getCompletedAt()).isEqualTo(completedAt);
              assertThat(order.getCancelledAt()).isNull();
            });
  }

  @Test
  void shouldPreserveOriginalCompletedAtWhenCompleteIsCalledAgain() {
    var readyAt = Instant.parse("2026-05-12T20:18:00Z");
    var completedAt = Instant.parse("2026-05-12T20:30:00Z");
    var secondCompletedAt = Instant.parse("2026-05-12T20:45:00Z");

    when(clock.instant()).thenReturn(readyAt, completedAt, secondCompletedAt);

    var savedOrder = createReadyOrder();

    flushAndClear();

    orderService.complete(TENANT_ID, savedOrder.getId());
    orderService.complete(TENANT_ID, savedOrder.getId());

    flushAndClear();

    var foundOrder = orderService.findOrder(TENANT_ID, savedOrder.getId());

    assertThat(foundOrder)
        .isPresent()
        .get()
        .satisfies(
            order -> {
              assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
              assertThat(order.getReadyAt()).isEqualTo(readyAt);
              assertThat(order.getCompletedAt()).isEqualTo(completedAt);
              assertThat(order.getCompletedAt()).isNotEqualTo(secondCompletedAt);
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
              assertThat(order.getCompletedAt()).isNull();
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

  private Order createOrderWithItems(UUID tenantId, int itemQuantity) {
    var order = new Order(tenantId, OrderServiceType.DELIVERY);
    order.addSimpleItem(UUID.randomUUID(), PRODUCT_NAME, itemQuantity, BigDecimal.ONE);
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
    var paymentInfo = new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PAID);
    return new CreateOrderCommand(
        fixtures.stream().map(OrderItemFixture::command).toList(),
        paymentInfo,
        OrderServiceType.COUNTER);
  }

  private CreateOrderCommand createDeliveryOrderCommand(
      List<OrderItemFixture> fixtures, UUID customerId, UUID customerAddressId) {
    var paymentInfo = new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PENDING);
    return new CreateOrderCommand(
        fixtures.stream().map(OrderItemFixture::command).toList(),
        paymentInfo,
        OrderServiceType.DELIVERY,
        customerId,
        customerAddressId);
  }

  private CustomerDeliveryInfo customerDeliveryInfo() {
    return new CustomerDeliveryInfo(
        CUSTOMER_ID,
        "Maria",
        "(11) 99999-8888",
        new CustomerDeliveryInfo.Address(
            CUSTOMER_ADDRESS_ID,
            "Home",
            new CustomerDeliveryInfo.Line("Rua das Flores", "123", null),
            new CustomerDeliveryInfo.Location("Centro", "Sao Paulo", "SP", "01000-000"),
            "Portao azul"));
  }

  private List<OrderableProduct> createOrderableProducts(List<OrderItemFixture> fixtures) {
    return fixtures.stream().map(OrderItemFixture::orderableProduct).toList();
  }

  private List<OrderableProductSelection> extractSelections(CreateOrderCommand command) {
    return command.items().stream()
        .map(item -> new OrderableProductSelection(item.productId(), item.variantId()))
        .toList();
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
