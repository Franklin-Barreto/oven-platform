package br.com.f2e.ovenplatform.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.orders.domain.exception.InvalidOrderStatusTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OrderTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final String PRODUCT_NAME = "Pizza Portuguesa";
  private static final int VALID_QUANTITY = 2;
  private static final BigDecimal VALID_UNIT_PRICE = new BigDecimal("35.40");

  @Test
  void shouldCreateOrderWithInitialState() {

    Order order = order();

    assertThat(order.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(order.getServiceType()).isEqualTo(OrderServiceType.COUNTER);
    assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(order.getReadyAt()).isNull();
    assertThat(order.getDeliveredAt()).isNull();
    assertThat(order.getCancelledAt()).isNull();
    assertThat(order.getItems()).isEmpty();
  }

  @Test
  void shouldRejectNullTenantId() {
    assertThatThrownBy(() -> new Order(null, OrderServiceType.COUNTER))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tenantId must not be null");
  }

  @Test
  void shouldRejectNullServiceType() {
    assertThatThrownBy(() -> new Order(TENANT_ID, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("serviceType must not be null");
  }

  @Test
  void shouldAddItemToOrder() {
    var order = order();

    order.addItem(PRODUCT_ID, PRODUCT_NAME, VALID_QUANTITY, VALID_UNIT_PRICE);

    assertThat(order.getItems()).hasSize(1);

    var item = order.getItems().getFirst();

    assertThat(item.getQuantity()).isEqualTo(VALID_QUANTITY);
    assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
    assertThat(item.getProductName()).isEqualTo(PRODUCT_NAME);
    assertThat(item.getUnitPrice()).isEqualByComparingTo(VALID_UNIT_PRICE);
    assertThat(item.getSubtotal()).isEqualByComparingTo("70.80");
  }

  @Test
  void shouldRecalculateTotalWhenAddingOneItem() {
    var order = order();

    order.addItem(PRODUCT_ID, PRODUCT_NAME, VALID_QUANTITY, VALID_UNIT_PRICE);

    assertThat(order.getTotalAmount()).isEqualByComparingTo("70.80");
  }

  @Test
  void shouldRecalculateTotalWhenAddingMultipleItems() {
    var order = order();

    order.addItem(PRODUCT_ID, PRODUCT_NAME, 2, new BigDecimal("35.40"));
    order.addItem(UUID.randomUUID(), "Coca-cola lata", 3, new BigDecimal("10.00"));

    assertThat(order.getTotalAmount()).isEqualByComparingTo("100.80");
  }

  @ParameterizedTest
  @MethodSource("invalidItems")
  void shouldRejectInvalidItemData(
      UUID productId,
      String productName,
      int quantity,
      BigDecimal unitPrice,
      String expectedMessage) {
    var order = new Order(TENANT_ID, OrderServiceType.COUNTER);
    assertThatThrownBy(() -> order.addItem(productId, productName, quantity, unitPrice))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldExposeItemsAsReadOnlyCollection() {
    var order = order();
    order.addItem(PRODUCT_ID, PRODUCT_NAME, VALID_QUANTITY, VALID_UNIT_PRICE);

    var items = order.getItems();

    assertThatThrownBy(items::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldThrowExceptionWhenCancellingReadyOrder() {
    var readyAt = Instant.parse("2026-05-12T20:18:00Z");
    var cancelledAt = Instant.parse("2026-05-12T20:30:00Z");

    var order = order();
    order.markAsReady(readyAt);

    assertThatThrownBy(() -> order.cancel(cancelledAt))
        .isInstanceOf(InvalidOrderStatusTransitionException.class)
        .hasMessage(
            "Cannot transition order from %s to %s."
                .formatted(OrderStatus.READY, OrderStatus.CANCELLED));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
    assertThat(order.getReadyAt()).isEqualTo(readyAt);
    assertThat(order.getCancelledAt()).isNull();
  }

  @Test
  void shouldThrowExceptionWhenCancellingDeliveredOrder() {
    var readyAt = Instant.parse("2026-05-12T20:18:00Z");
    var deliveredAt = Instant.parse("2026-05-12T20:25:00Z");
    var cancelledAt = Instant.parse("2026-05-12T20:30:00Z");

    var order = order();
    order.markAsReady(readyAt);
    order.markAsDelivered(deliveredAt);

    assertThatThrownBy(() -> order.cancel(cancelledAt))
        .isInstanceOf(InvalidOrderStatusTransitionException.class)
        .hasMessage(
            "Cannot transition order from %s to %s."
                .formatted(OrderStatus.DELIVERED, OrderStatus.CANCELLED));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    assertThat(order.getReadyAt()).isEqualTo(readyAt);
    assertThat(order.getDeliveredAt()).isEqualTo(deliveredAt);
    assertThat(order.getCancelledAt()).isNull();
  }

  @Test
  void shouldKeepReadyTimestampWhenMarkingReadyOrderAsReadyAgain() {
    var readyAt = Instant.parse("2026-05-12T20:18:00Z");
    var secondAttemptAt = Instant.parse("2026-05-12T20:25:00Z");

    var order = order();

    order.markAsReady(readyAt);
    order.markAsReady(secondAttemptAt);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
    assertThat(order.getReadyAt()).isEqualTo(readyAt);
    assertThat(order.getReadyAt()).isNotEqualTo(secondAttemptAt);
    assertThat(order.getDeliveredAt()).isNull();
    assertThat(order.getCancelledAt()).isNull();
  }

  @Test
  void shouldKeepDeliveredTimestampWhenMarkingDeliveredOrderAsDeliveredAgain() {
    var readyAt = Instant.parse("2026-05-12T20:18:00Z");
    var deliveredAt = Instant.parse("2026-05-12T20:20:00Z");
    var secondAttemptAt = Instant.parse("2026-05-12T20:25:00Z");

    var order = order();

    order.markAsReady(readyAt);
    order.markAsDelivered(deliveredAt);
    order.markAsDelivered(secondAttemptAt);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    assertThat(order.getReadyAt()).isEqualTo(readyAt);
    assertThat(order.getDeliveredAt()).isEqualTo(deliveredAt);
    assertThat(order.getDeliveredAt()).isNotEqualTo(secondAttemptAt);
    assertThat(order.getCancelledAt()).isNull();
  }

  @Test
  void shouldKeepCancelledTimestampWhenMarkingCancelledOrderAsCancelledAgain() {
    var cancelledAt = Instant.parse("2026-05-12T20:18:00Z");
    var secondAttemptAt = Instant.parse("2026-05-12T20:25:00Z");

    var order = order();

    order.cancel(cancelledAt);
    order.cancel(secondAttemptAt);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(order.getReadyAt()).isNull();
    assertThat(order.getDeliveredAt()).isNull();
    assertThat(order.getCancelledAt()).isEqualTo(cancelledAt);
    assertThat(order.getCancelledAt()).isNotEqualTo(secondAttemptAt);
  }

  private static Stream<Arguments> invalidItems() {
    return Stream.of(
        Arguments.of(
            null, PRODUCT_NAME, VALID_QUANTITY, VALID_UNIT_PRICE, "productId must not be null"),
        Arguments.of(
            PRODUCT_ID, null, VALID_QUANTITY, VALID_UNIT_PRICE, "productName must not be null"),
        Arguments.of(
            PRODUCT_ID, "", VALID_QUANTITY, VALID_UNIT_PRICE, "productName must not be blank"),
        Arguments.of(
            PRODUCT_ID,
            "beer",
            VALID_QUANTITY,
            VALID_UNIT_PRICE,
            "productName must have at least 5 characters"),
        Arguments.of(
            PRODUCT_ID, PRODUCT_NAME, 0, VALID_UNIT_PRICE, "quantity must be greater than zero"),
        Arguments.of(
            PRODUCT_ID, PRODUCT_NAME, -1, VALID_UNIT_PRICE, "quantity must be greater than zero"),
        Arguments.of(PRODUCT_ID, PRODUCT_NAME, VALID_QUANTITY, null, "unitPrice must not be null"),
        Arguments.of(
            PRODUCT_ID,
            PRODUCT_NAME,
            VALID_QUANTITY,
            BigDecimal.ZERO,
            "unitPrice must be greater than zero"),
        Arguments.of(
            PRODUCT_ID,
            PRODUCT_NAME,
            VALID_QUANTITY,
            new BigDecimal("-1.00"),
            "unitPrice must be greater than zero"));
  }

  private static Order order() {
    return new Order(TENANT_ID, OrderServiceType.COUNTER);
  }
}
