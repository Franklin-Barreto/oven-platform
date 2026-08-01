package br.com.f2e.ovenplatform.orders.infrastructure.web.dto;

import br.com.f2e.ovenplatform.orders.application.CreateOrderItemCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record OrderItemRequest(@NotNull UUID productId, UUID variantId, @Positive int quantity) {

  public OrderItemRequest(UUID productId, int quantity) {
    this(productId, null, quantity);
  }

  public CreateOrderItemCommand toCommand() {
    return new CreateOrderItemCommand(productId, variantId, quantity);
  }
}
