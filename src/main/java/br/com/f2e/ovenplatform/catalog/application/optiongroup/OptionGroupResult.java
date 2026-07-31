package br.com.f2e.ovenplatform.catalog.application.optiongroup;

import br.com.f2e.ovenplatform.catalog.domain.OptionGroup;
import java.util.UUID;

public record OptionGroupResult(
    UUID id,
    UUID tenantId,
    UUID productId,
    String name,
    int minimumSelections,
    int maximumSelections,
    boolean active,
    int displayPosition) {

  public static OptionGroupResult from(OptionGroup optionGroup) {
    return new OptionGroupResult(
        optionGroup.getId(),
        optionGroup.getTenantId(),
        optionGroup.getProductId(),
        optionGroup.getName(),
        optionGroup.getMinimumSelections(),
        optionGroup.getMaximumSelections(),
        optionGroup.isActive(),
        optionGroup.getDisplayPosition());
  }
}
