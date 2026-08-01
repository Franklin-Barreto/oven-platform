package br.com.f2e.ovenplatform.catalog.application.api;

import java.util.List;
import java.util.UUID;

public record ActiveProductOptionConfiguration(
    UUID productId, List<ActiveProductOptionGroup> optionGroups) {

  public ActiveProductOptionConfiguration {
    optionGroups = List.copyOf(optionGroups);
  }
}
