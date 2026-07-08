package br.com.f2e.ovenplatform.shared.application.event.payload.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.shared.application.event.payload.PaymentMethod;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderCreatedPayloadTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("120.00");

  @Test
  void shouldRejectNullTenantId() {
    var items = items();
    assertThatThrownBy(
            () ->
                new OrderCreatedPayload(
                    null,
                    ORDER_ID,
                    TOTAL_AMOUNT,
                    PaymentMethod.CASH,
                    OrderPaymentStatus.PAID,
                    items))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("tenantId must not be null");
  }

  @Test
  void shouldRejectNullOrderId() {
    var items = items();
    assertThatThrownBy(
            () ->
                new OrderCreatedPayload(
                    TENANT_ID,
                    null,
                    TOTAL_AMOUNT,
                    PaymentMethod.CASH,
                    OrderPaymentStatus.PAID,
                    items))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("orderId must not be null");
  }

  @Test
  void shouldRejectNonPositiveTotalAmount() {
    var items = items();
    assertThatThrownBy(
            () ->
                new OrderCreatedPayload(
                    TENANT_ID,
                    ORDER_ID,
                    BigDecimal.ZERO,
                    PaymentMethod.CASH,
                    OrderPaymentStatus.PAID,
                    items))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("totalAmount must be greater than zero");
  }

  @Test
  void shouldRejectNullPaymentMethod() {
    var items = items();
    assertThatThrownBy(
            () ->
                new OrderCreatedPayload(
                    TENANT_ID, ORDER_ID, TOTAL_AMOUNT, null, OrderPaymentStatus.PAID, items))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("paymentMethod must not be null");
  }

  @Test
  void shouldRejectNullPaymentStatus() {
    var items = items();
    assertThatThrownBy(
            () ->
                new OrderCreatedPayload(
                    TENANT_ID, ORDER_ID, TOTAL_AMOUNT, PaymentMethod.CASH, null, items))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("paymentStatus must not be null");
  }

  @Test
  void shouldRejectEmptyItems() {
    assertThatThrownBy(
            () ->
                new OrderCreatedPayload(
                    TENANT_ID,
                    ORDER_ID,
                    TOTAL_AMOUNT,
                    PaymentMethod.CASH,
                    OrderPaymentStatus.PAID,
                    List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("items must have at least 1 item");
  }

  @Test
  void shouldDefensivelyCopyItems() {
    var items = new ArrayList<>(items());

    var payload =
        new OrderCreatedPayload(
            TENANT_ID, ORDER_ID, TOTAL_AMOUNT, PaymentMethod.CASH, OrderPaymentStatus.PAID, items);

    items.clear();

    var payloadItems = payload.items();

    assertThat(payloadItems).hasSize(1);
    assertThatThrownBy(payloadItems::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  private static List<OrderCreatedItemPayload> items() {
    return List.of(
        new OrderCreatedItemPayload(
            UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234"),
            "Pizza Portuguesa",
            2,
            new BigDecimal("60.00")));
  }
}
