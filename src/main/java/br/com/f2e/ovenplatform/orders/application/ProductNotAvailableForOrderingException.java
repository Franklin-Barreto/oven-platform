package br.com.f2e.ovenplatform.orders.application;

import java.util.UUID;

public class ProductNotAvailableForOrderingException extends RuntimeException {

  public ProductNotAvailableForOrderingException(UUID productId) {
    super("Product is not available for ordering: " + productId);
  }
}
