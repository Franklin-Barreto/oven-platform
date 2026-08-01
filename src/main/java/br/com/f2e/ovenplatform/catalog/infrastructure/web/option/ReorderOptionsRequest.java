package br.com.f2e.ovenplatform.catalog.infrastructure.web.option;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderOptionsRequest(@NotNull List<@NotNull UUID> optionIds) {

  public ReorderOptionsRequest {
    optionIds = optionIds == null ? null : List.copyOf(optionIds);
  }
}
