package br.com.f2e.ovenplatform.kitchen.infrastructure.web.dto;

import br.com.f2e.ovenplatform.kitchen.domain.TicketItem;
import java.util.UUID;

public record TicketItemResponse(
    UUID id, UUID productId, String productName, UUID variantId, String variantName, int quantity) {

  public static TicketItemResponse from(TicketItem item) {
    return new TicketItemResponse(
        item.getId(),
        item.getProductId(),
        item.getProductName(),
        item.getVariantId(),
        item.getVariantName(),
        item.getQuantity());
  }
}
