package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResult;
import java.util.Comparator;
import java.util.List;

public record ProductDetailResult(
    ProductSummaryResult product, List<ProductVariantDetailResult> variants) {

  public ProductDetailResult {
    variants = List.copyOf(variants);
  }

  public static ProductDetailResult from(
      ProductResult product, List<ProductVariantResult> variants) {
    var summary = ProductSummaryResult.from(product, variants);
    var activeVariants =
        variants.stream()
            .filter(ProductVariantResult::active)
            .sorted(
                Comparator.comparingInt(ProductVariantResult::displayPosition)
                    .thenComparing(ProductVariantResult::id))
            .map(variant -> ProductVariantDetailResult.from(variant, product))
            .toList();

    return new ProductDetailResult(summary, activeVariants);
  }

  @Override
  public List<ProductVariantDetailResult> variants() {
    return List.copyOf(variants);
  }
}
