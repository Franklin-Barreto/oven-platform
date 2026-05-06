package br.com.f2e.ovenplatform.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OrderTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final int VALID_QUANTITY = 2;
  private static final BigDecimal VALID_UNIT_PRICE = new BigDecimal("35.40");

  @Test
  void shouldCreateOrderWithInitialState() {

    assertThat(order().getTenantId()).isEqualTo(TENANT_ID);
    assertThat(order().getStatus()).isEqualTo(OrderStatus.CREATED);
    assertThat(order().getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(order().getItems()).isEmpty();
  }

  @Test
  void shouldRejectNullTenantId() {
    assertThatThrownBy(() -> new Order(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tenantId must not be null");
  }

  @Test
  void shouldAddItemToOrder() {
    var order = order();

    order.addItem(PRODUCT_ID, VALID_QUANTITY, VALID_UNIT_PRICE);

    assertThat(order.getItems()).hasSize(1);

    var item = order.getItems().getFirst();

    assertThat(item.getQuantity()).isEqualTo(VALID_QUANTITY);
    assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
    assertThat(item.getUnitPrice()).isEqualByComparingTo(VALID_UNIT_PRICE);
    assertThat(item.getSubtotal()).isEqualByComparingTo("70.80");
  }

  @Test
  void shouldRecalculateTotalWhenAddingOneItem() {
    var order = order();

    order.addItem(PRODUCT_ID, VALID_QUANTITY, VALID_UNIT_PRICE);

    assertThat(order.getTotalAmount()).isEqualByComparingTo("70.80");
  }

  @Test
  void shouldRecalculateTotalWhenAddingMultipleItems() {
    var order = order();

    order.addItem(PRODUCT_ID, 2, new BigDecimal("35.40"));
    order.addItem(UUID.randomUUID(), 3, new BigDecimal("10.00"));

    assertThat(order.getTotalAmount()).isEqualByComparingTo("100.80");
  }

  @ParameterizedTest
  @MethodSource("invalidItems")
  void shouldRejectInvalidItemData(
      UUID productId, int quantity, BigDecimal unitPrice, String expectedMessage) {
    var order = new Order(TENANT_ID);
    assertThatThrownBy(() -> order.addItem(productId, quantity, unitPrice))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void shouldExposeItemsAsReadOnlyCollection() {
    var order = order();
    order.addItem(PRODUCT_ID, VALID_QUANTITY, VALID_UNIT_PRICE);

    var items = order.getItems();

    assertThatThrownBy(items::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  private static Stream<Arguments> invalidItems() {
    return Stream.of(
        Arguments.of(null, VALID_QUANTITY, VALID_UNIT_PRICE, "productId must not be null"),
        Arguments.of(PRODUCT_ID, 0, VALID_UNIT_PRICE, "quantity must be greater than zero"),
        Arguments.of(PRODUCT_ID, -1, VALID_UNIT_PRICE, "quantity must be greater than zero"),
        Arguments.of(PRODUCT_ID, VALID_QUANTITY, null, "unitPrice must not be null"),
        Arguments.of(
            PRODUCT_ID, VALID_QUANTITY, BigDecimal.ZERO, "unitPrice must be greater than zero"),
        Arguments.of(
            PRODUCT_ID,
            VALID_QUANTITY,
            new BigDecimal("-1.00"),
            "unitPrice must be greater than zero"));
  }

  private static Order order() {
    return new Order(TENANT_ID);
  }
}
