package br.com.f2e.ovenplatform.orders.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateOrderCommand(
    List<CreateOrderItemCommand> items,
    PaymentInfo paymentInfo,
    OrderServiceType serviceType,
    UUID customerId,
    UUID customerAddressId) {

  public CreateOrderCommand(
      List<CreateOrderItemCommand> items, PaymentInfo paymentInfo, OrderServiceType serviceType) {
    this(items, paymentInfo, serviceType, null, null);
  }

  public CreateOrderCommand {
    items = List.copyOf(requireNotNull(items, "items"));
    if (items.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("items must not contain null elements");
    }

    requireNotNull(paymentInfo, "paymentInfo");
    var requiredServiceType = requireNotNull(serviceType, "serviceType");

    if (requiredServiceType == OrderServiceType.DELIVERY) {
      requireNotNull(customerId, "customerId");
      requireNotNull(customerAddressId, "customerAddressId");
    }
  }
}
