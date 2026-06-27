package br.com.f2e.ovenplatform.orders.infrastructure.web.dto;

import br.com.f2e.ovenplatform.orders.domain.OrderItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
    UUID productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
  public static List<OrderItemResponse> from(List<OrderItem> items) {
    return items.stream()
        .map(
            item ->
                new OrderItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal()))
        .toList();
  }
}
