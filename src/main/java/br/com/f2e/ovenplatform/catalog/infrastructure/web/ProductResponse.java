package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import br.com.f2e.ovenplatform.catalog.application.ProductResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID tenantId,
    UUID categoryId,
    UUID imageId,
    URI imageUrl,
    String name,
    String description,
    BigDecimal price,
    boolean active) {

  public static ProductResponse from(ProductResult product) {
    return new ProductResponse(
        product.id(),
        product.tenantId(),
        product.categoryId(),
        product.imageId(),
        product.imageUrl(),
        product.name(),
        product.description(),
        product.price(),
        product.active());
  }
}
