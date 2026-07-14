package br.com.f2e.ovenplatform.orders.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentMethod;
import br.com.f2e.ovenplatform.shared.application.payment.PaymentStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateOrderCommandTest {

  @Test
  void shouldRequireCustomerIdForDeliveryOrder() {
    var productId = UUID.randomUUID();
    var customerAddressId = UUID.randomUUID();
    var paymentInfo = new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PENDING);
    var items = List.of(new CreateOrderItemCommand(productId, 1));

    assertThatThrownBy(
            () ->
                new CreateOrderCommand(
                    items, paymentInfo, OrderServiceType.DELIVERY, null, customerAddressId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("customerId must not be null");
  }

  @Test
  void shouldRequireCustomerAddressIdForDeliveryOrder() {
    var productId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var paymentInfo = new PaymentInfo(PaymentMethod.CASH, PaymentStatus.PENDING);
    var items = List.of(new CreateOrderItemCommand(productId, 1));

    assertThatThrownBy(
            () ->
                new CreateOrderCommand(
                    items, paymentInfo, OrderServiceType.DELIVERY, customerId, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("customerAddressId must not be null");
  }
}
