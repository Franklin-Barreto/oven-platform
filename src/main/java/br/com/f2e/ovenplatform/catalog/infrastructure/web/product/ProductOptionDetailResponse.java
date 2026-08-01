package br.com.f2e.ovenplatform.catalog.infrastructure.web.product;

import br.com.f2e.ovenplatform.catalog.application.product.ProductOptionDetailResult;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductOptionDetailResponse(
    UUID id, String name, BigDecimal priceAdjustment, int displayPosition) {

  public static ProductOptionDetailResponse from(ProductOptionDetailResult option) {
    return new ProductOptionDetailResponse(
        option.id(), option.name(), option.priceAdjustment(), option.displayPosition());
  }
}
