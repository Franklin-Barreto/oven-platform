package br.com.f2e.ovenplatform.catalog.infrastructure.web.product;

import br.com.f2e.ovenplatform.catalog.application.product.ProductVariantDetailResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

public record ProductVariantDetailResponse(
    UUID id, String name, BigDecimal price, int displayPosition, UUID imageId, URI imageUrl) {

  public static ProductVariantDetailResponse from(ProductVariantDetailResult variant) {
    return new ProductVariantDetailResponse(
        variant.id(),
        variant.name(),
        variant.price(),
        variant.displayPosition(),
        variant.imageId(),
        variant.imageUrl());
  }
}
