package br.com.f2e.ovenplatform.shared.application.event.payload.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderCreatedItemPayloadTest {

  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final String PRODUCT_NAME = "Pizza Portuguesa";
  private static final BigDecimal UNIT_PRICE = new BigDecimal("60.00");

  @Test
  void shouldRejectNullProductId() {
    assertThatThrownBy(() -> new OrderCreatedItemPayload(null, PRODUCT_NAME, 2, UNIT_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("productId must not be null");
  }

  @Test
  void shouldRejectBlankProductName() {
    assertThatThrownBy(() -> new OrderCreatedItemPayload(PRODUCT_ID, " ", 2, UNIT_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("productName must not be blank");
  }

  @Test
  void shouldRejectNonPositiveQuantity() {
    assertThatThrownBy(() -> new OrderCreatedItemPayload(PRODUCT_ID, PRODUCT_NAME, 0, UNIT_PRICE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("quantity must be greater than zero");
  }

  @Test
  void shouldRejectNonPositiveUnitPrice() {
    assertThatThrownBy(
            () -> new OrderCreatedItemPayload(PRODUCT_ID, PRODUCT_NAME, 2, BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("unitPrice must be greater than zero");
  }
}
