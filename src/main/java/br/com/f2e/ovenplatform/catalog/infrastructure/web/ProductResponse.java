package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import br.com.f2e.ovenplatform.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID tenantId,
    UUID categoryId,
    String name,
    String description,
    BigDecimal price,
    boolean active) {

  public static ProductResponse from(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getTenantId(),
        product.getCategoryId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.isActive());
  }
}
