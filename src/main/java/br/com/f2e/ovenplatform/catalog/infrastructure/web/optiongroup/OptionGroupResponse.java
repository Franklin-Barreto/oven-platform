package br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup;

import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupResult;
import java.util.UUID;

public record OptionGroupResponse(
    UUID id,
    UUID productId,
    UUID tenantId,
    String name,
    int minimumSelections,
    int maximumSelections,
    boolean active,
    int displayPosition) {

  public static OptionGroupResponse from(OptionGroupResult optionGroup) {
    return new OptionGroupResponse(
        optionGroup.id(),
        optionGroup.productId(),
        optionGroup.tenantId(),
        optionGroup.name(),
        optionGroup.minimumSelections(),
        optionGroup.maximumSelections(),
        optionGroup.active(),
        optionGroup.displayPosition());
  }
}
