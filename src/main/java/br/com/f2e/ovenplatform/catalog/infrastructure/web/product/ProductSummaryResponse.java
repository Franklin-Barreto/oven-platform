package br.com.f2e.ovenplatform.catalog.infrastructure.web.product;

import br.com.f2e.ovenplatform.catalog.application.product.ProductSummaryResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

public record ProductSummaryResponse(
    UUID id,
    UUID categoryId,
    UUID imageId,
    URI imageUrl,
    String name,
    String description,
    BigDecimal displayPrice,
    boolean hasVariants,
    boolean available) {

  public static ProductSummaryResponse from(ProductSummaryResult product) {
    return new ProductSummaryResponse(
        product.id(),
        product.categoryId(),
        product.imageId(),
        product.imageUrl(),
        product.name(),
        product.description(),
        product.displayPrice(),
        product.hasVariants(),
        product.available());
  }
}
