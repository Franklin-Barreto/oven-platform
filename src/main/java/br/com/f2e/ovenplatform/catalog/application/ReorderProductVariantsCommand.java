package br.com.f2e.ovenplatform.catalog.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ReorderProductVariantsCommand(List<UUID> variantIds) {

  public ReorderProductVariantsCommand {
    requireNotNull(variantIds, "variantIds");
    if (variantIds.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("variantIds must not contain null elements");
    }
    variantIds = List.copyOf(variantIds);
  }
}
