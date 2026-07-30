package br.com.f2e.ovenplatform.catalog.infrastructure.web.variant;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record ReorderProductVariantsRequest(@NotNull List<@NotNull UUID> variantIds) {

  public ReorderProductVariantsRequest {
    if (variantIds != null) {
      variantIds = Collections.unmodifiableList(new ArrayList<>(variantIds));
    }
  }

  @Override
  public List<UUID> variantIds() {
    return variantIds == null ? null : new ArrayList<>(variantIds);
  }
}
