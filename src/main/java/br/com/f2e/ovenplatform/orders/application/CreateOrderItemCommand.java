package br.com.f2e.ovenplatform.orders.application;

import java.util.UUID;

public record CreateOrderItemCommand(UUID productId, UUID variantId, int quantity) {

  public CreateOrderItemCommand(UUID productId, int quantity) {
    this(productId, null, quantity);
  }
}
