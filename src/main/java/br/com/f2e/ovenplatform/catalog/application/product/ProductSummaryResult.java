package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public record ProductSummaryResult(
    UUID id,
    UUID categoryId,
    UUID imageId,
    URI imageUrl,
    String name,
    String description,
    BigDecimal price,
    boolean active,
    BigDecimal displayPrice,
    boolean hasVariants,
    boolean available) {

  public static ProductSummaryResult from(
      ProductResult product, List<ProductVariantResult> variants) {
    var hasVariants = !variants.isEmpty();
    var minimumActiveVariantPrice =
        variants.stream()
            .filter(ProductVariantResult::active)
            .map(ProductVariantResult::price)
            .min(BigDecimal::compareTo);
    var available = product.active() && (!hasVariants || minimumActiveVariantPrice.isPresent());
    var displayPrice = hasVariants ? minimumActiveVariantPrice.orElse(null) : product.price();

    return new ProductSummaryResult(
        product.id(),
        product.categoryId(),
        product.imageId(),
        product.imageUrl(),
        product.name(),
        product.description(),
        product.price(),
        product.active(),
        displayPrice,
        hasVariants,
        available);
  }
}
