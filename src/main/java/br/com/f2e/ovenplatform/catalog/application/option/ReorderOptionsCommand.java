package br.com.f2e.ovenplatform.catalog.application.option;

import java.util.List;
import java.util.UUID;

public record ReorderOptionsCommand(List<UUID> optionIds) {

  public ReorderOptionsCommand {
    optionIds = List.copyOf(optionIds);
  }
}
