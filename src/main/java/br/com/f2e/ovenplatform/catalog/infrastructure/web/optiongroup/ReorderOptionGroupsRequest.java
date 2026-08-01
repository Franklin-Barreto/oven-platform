package br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ReorderOptionGroupsRequest(@NotNull List<@NotNull UUID> optionGroupIds) {

  public ReorderOptionGroupsRequest {
    optionGroupIds = optionGroupIds == null ? null : List.copyOf(optionGroupIds);
  }
}
