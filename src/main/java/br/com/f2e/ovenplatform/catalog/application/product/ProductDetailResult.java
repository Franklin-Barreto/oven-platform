package br.com.f2e.ovenplatform.catalog.application.product;

import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResult;
import java.util.Comparator;
import java.util.List;

public record ProductDetailResult(
    ProductSummaryResult product,
    List<ProductVariantDetailResult> variants,
    List<ProductOptionGroupDetailResult> optionGroups) {

  public ProductDetailResult {
    variants = List.copyOf(variants);
    optionGroups = List.copyOf(optionGroups);
  }

  public ProductDetailResult(
      ProductSummaryResult product, List<ProductVariantDetailResult> variants) {
    this(product, variants, List.of());
  }

  public static ProductDetailResult from(
      ProductResult product,
      List<ProductVariantResult> allVariants,
      List<ProductVariantResult> resolvedActiveVariants) {
    var summary = ProductSummaryResult.from(product, allVariants);
    var detailVariants =
        resolvedActiveVariants.stream()
            .sorted(
                Comparator.comparingInt(ProductVariantResult::displayPosition)
                    .thenComparing(ProductVariantResult::id))
            .map(variant -> ProductVariantDetailResult.from(variant, product))
            .toList();

    return new ProductDetailResult(summary, detailVariants, List.of());
  }

  public static ProductDetailResult from(
      ProductResult product,
      List<ProductVariantResult> allVariants,
      List<ProductVariantResult> resolvedActiveVariants,
      List<ProductOptionGroupDetailResult> optionGroups) {
    ProductDetailResult detail = from(product, allVariants, resolvedActiveVariants);
    return new ProductDetailResult(detail.product(), detail.variants(), optionGroups);
  }

  @Override
  public List<ProductVariantDetailResult> variants() {
    return List.copyOf(variants);
  }

  @Override
  public List<ProductOptionGroupDetailResult> optionGroups() {
    return List.copyOf(optionGroups);
  }
}
