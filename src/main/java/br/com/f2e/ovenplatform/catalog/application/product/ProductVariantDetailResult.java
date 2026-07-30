package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

public record ProductVariantDetailResult(
    UUID id, String name, BigDecimal price, int displayPosition, UUID imageId, URI imageUrl) {

  public static ProductVariantDetailResult from(
      ProductVariantResult variant, ProductResult product) {
    if (variant.imageId() != null && variant.imageUrl() == null) {
      throw new IllegalStateException("Variant image URL must be resolved");
    }

    var effectiveImageId = variant.imageId() == null ? product.imageId() : variant.imageId();
    var effectiveImageUrl = variant.imageId() == null ? product.imageUrl() : variant.imageUrl();

    return new ProductVariantDetailResult(
        variant.id(),
        variant.name(),
        variant.price(),
        variant.displayPosition(),
        effectiveImageId,
        effectiveImageUrl);
  }
}
