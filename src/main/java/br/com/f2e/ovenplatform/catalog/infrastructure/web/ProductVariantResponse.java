package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

public record ProductVariantResponse(
    UUID id,
    UUID productId,
    UUID imageId,
    String name,
    URI imageUrl,
    BigDecimal price,
    boolean active,
    int displayPosition) {

  public static ProductVariantResponse from(ProductVariantResult productVariant) {
    return new ProductVariantResponse(
        productVariant.id(),
        productVariant.productId(),
        productVariant.imageId(),
        productVariant.name(),
        productVariant.imageUrl(),
        productVariant.price(),
        productVariant.active(),
        productVariant.displayPosition());
  }
}
