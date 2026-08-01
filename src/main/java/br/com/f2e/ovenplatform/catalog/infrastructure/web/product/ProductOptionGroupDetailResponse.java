package br.com.f2e.ovenplatform.catalog.infrastructure.web.product;

import br.com.f2e.ovenplatform.catalog.application.product.ProductOptionGroupDetailResult;
import java.util.List;
import java.util.UUID;

public record ProductOptionGroupDetailResponse(
    UUID id,
    String name,
    int minimumSelections,
    int maximumSelections,
    int displayPosition,
    List<ProductOptionDetailResponse> options) {

  public ProductOptionGroupDetailResponse {
    options = List.copyOf(options);
  }

  public static ProductOptionGroupDetailResponse from(ProductOptionGroupDetailResult optionGroup) {
    return new ProductOptionGroupDetailResponse(
        optionGroup.id(),
        optionGroup.name(),
        optionGroup.minimumSelections(),
        optionGroup.maximumSelections(),
        optionGroup.displayPosition(),
        optionGroup.options().stream().map(ProductOptionDetailResponse::from).toList());
  }
}
