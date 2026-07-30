package br.com.f2e.ovenplatform.catalog.application;

import br.com.f2e.ovenplatform.catalog.domain.ProductVariant;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

public record ProductVariantResult(
    UUID id,
    UUID tenantId,
    UUID productId,
    UUID imageId,
    URI imageUrl,
    String name,
    BigDecimal price,
    boolean active,
    int displayPosition) {

  public static ProductVariantResult from(ProductVariant variant, URI imageUrl) {
    return new ProductVariantResult(
        variant.getId(),
        variant.getTenantId(),
        variant.getProductId(),
        variant.getImageId(),
        imageUrl,
        variant.getName(),
        variant.getPrice(),
        variant.isActive(),
        variant.getDisplayPosition());
  }
}
