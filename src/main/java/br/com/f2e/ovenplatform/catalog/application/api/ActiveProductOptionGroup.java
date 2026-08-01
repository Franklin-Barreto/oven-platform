package br.com.f2e.ovenplatform.catalog.application.api;

import java.util.List;
import java.util.UUID;

public record ActiveProductOptionGroup(
    UUID id,
    String name,
    int minimumSelections,
    int maximumSelections,
    List<ActiveProductOption> options) {

  public ActiveProductOptionGroup {
    options = List.copyOf(options);
  }
}
