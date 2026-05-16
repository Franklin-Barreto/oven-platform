package br.com.f2e.ovenplatform.orders.infrastructure.web.dto;

import br.com.f2e.ovenplatform.orders.application.CreateOrderItemCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record OrderItemRequest(@NotNull UUID productId, @Positive int quantity) {
  public CreateOrderItemCommand toCommand() {
    return new CreateOrderItemCommand(productId, quantity);
  }
}
