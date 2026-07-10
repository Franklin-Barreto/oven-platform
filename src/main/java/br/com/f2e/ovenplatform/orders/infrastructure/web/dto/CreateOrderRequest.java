package br.com.f2e.ovenplatform.orders.infrastructure.web.dto;

import br.com.f2e.ovenplatform.orders.application.CreateOrderCommand;
import br.com.f2e.ovenplatform.orders.application.PaymentInfo;
import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateOrderRequest(
    @NotNull OrderServiceType serviceType,
    @NotNull(message = "must not be null")
        @Size(min = 1, message = "items must have at least 1 item")
        @Valid
        List<OrderItemRequest> items,
    @NotNull @Valid PaymentInfo paymentInfo) {

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
        items.stream().map(OrderItemRequest::toCommand).toList(), paymentInfo, serviceType);
  }
}
