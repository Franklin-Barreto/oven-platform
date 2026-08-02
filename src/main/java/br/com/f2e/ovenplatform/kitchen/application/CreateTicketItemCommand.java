package br.com.f2e.ovenplatform.kitchen.application;

import java.util.UUID;

public record CreateTicketItemCommand(
    UUID productId, String productName, UUID variantId, String variantName, int quantity) {

  public CreateTicketItemCommand(UUID productId, String productName, int quantity) {
    this(productId, productName, null, null, quantity);
  }
}
