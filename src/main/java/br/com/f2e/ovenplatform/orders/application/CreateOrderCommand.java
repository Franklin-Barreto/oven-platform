package br.com.f2e.ovenplatform.orders.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import br.com.f2e.ovenplatform.orders.domain.OrderServiceType;
import java.util.List;

public record CreateOrderCommand(
    List<CreateOrderItemCommand> items, PaymentInfo paymentInfo, OrderServiceType serviceType) {

  public CreateOrderCommand {
    items = List.copyOf(requireNotNull(items, "items"));
    requireNotNull(paymentInfo, "paymentInfo");
    requireNotNull(serviceType, "serviceType");
  }
}
