package br.com.f2e.ovenplatform.shared.application.event.payload.order;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requirePositive;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedItemPayload(
    UUID productId, String productName, int quantity, BigDecimal unitPrice) {

  public OrderCreatedItemPayload {
    requireNotNull(productId, "productId");
    requireNotBlank(productName, "productName");
    requirePositive(quantity, "quantity");
    requirePositive(unitPrice, "unitPrice");
  }
}
