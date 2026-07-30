package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import br.com.f2e.ovenplatform.catalog.application.product.ProductDetailResult;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
    UUID id,
    UUID categoryId,
    UUID imageId,
    URI imageUrl,
    String name,
    String description,
    BigDecimal displayPrice,
    boolean hasVariants,
    boolean available,
    List<ProductVariantDetailResponse> variants) {

  public ProductDetailResponse {
    variants = List.copyOf(variants);
  }

  public static ProductDetailResponse from(ProductDetailResult detail) {
    var product = detail.product();

    return new ProductDetailResponse(
        product.id(),
        product.categoryId(),
        product.imageId(),
        product.imageUrl(),
        product.name(),
        product.description(),
        product.displayPrice(),
        product.hasVariants(),
        product.available(),
        detail.variants().stream().map(ProductVariantDetailResponse::from).toList());
  }

  @Override
  public List<ProductVariantDetailResponse> variants() {
    return List.copyOf(variants);
  }
}
