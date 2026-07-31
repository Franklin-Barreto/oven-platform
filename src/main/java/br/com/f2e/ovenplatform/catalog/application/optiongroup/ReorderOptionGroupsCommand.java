package br.com.f2e.ovenplatform.catalog.application.optiongroup;

import java.util.List;
import java.util.UUID;

public record ReorderOptionGroupsCommand(List<UUID> optionGroupIds) {

  public ReorderOptionGroupsCommand {
    optionGroupIds = List.copyOf(optionGroupIds);
  }
}
