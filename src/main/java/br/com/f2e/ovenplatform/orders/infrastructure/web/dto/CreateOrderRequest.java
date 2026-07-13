package br.com.f2e.ovenplatform.orders.infrastructure.web.dto;

import br.com.f2e.ovenplatform.orders.application.CreateOrderCommand;
import br.com.f2e.ovenplatform.orders.application.PaymentInfo;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
    @NotNull OrderServiceType serviceType,
    UUID customerId,
    UUID customerAddressId,
    @NotNull(message = "must not be null")
        @Size(min = 1, message = "items must have at least 1 item")
        @Valid
        List<OrderItemRequest> items,
    @NotNull @Valid PaymentInfo paymentInfo) {

  public CreateOrderRequest(
      OrderServiceType serviceType, List<OrderItemRequest> items, PaymentInfo paymentInfo) {
    this(serviceType, null, null, items, paymentInfo);
  }

  public CreateOrderRequest {
    if (items != null) {
      if (items.stream().anyMatch(java.util.Objects::isNull)) {
        throw new IllegalArgumentException("items must not contain null elements");
      }

      items = List.copyOf(items);
    }
  }

  @Override
  public List<OrderItemRequest> items() {
    return items != null ? List.copyOf(items) : null;
  }

  public CreateOrderCommand toCommand() {
    return new CreateOrderCommand(
        items.stream().map(OrderItemRequest::toCommand).toList(),
        paymentInfo,
        serviceType,
        customerId,
        customerAddressId);
  }

  @AssertTrue(message = "customerId and customerAddressId are required for delivery orders")
  public boolean isDeliveryCustomerInfoValid() {
    return serviceType != OrderServiceType.DELIVERY
        || (customerId != null && customerAddressId != null);
  }
}
