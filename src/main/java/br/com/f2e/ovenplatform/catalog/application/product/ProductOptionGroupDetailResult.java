package br.com.f2e.ovenplatform.catalog.application.product;

import java.util.List;
import java.util.UUID;

public record ProductOptionGroupDetailResult(
    UUID id,
    String name,
    int minimumSelections,
    int maximumSelections,
    int displayPosition,
    List<ProductOptionDetailResult> options) {

  public ProductOptionGroupDetailResult {
    options = List.copyOf(options);
  }
}
