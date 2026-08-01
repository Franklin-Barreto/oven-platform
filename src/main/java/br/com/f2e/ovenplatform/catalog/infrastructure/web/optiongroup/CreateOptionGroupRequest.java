package br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateOptionGroupRequest(
    @NotBlank @Size(max = 80) String name,
    @NotNull @PositiveOrZero Integer minimumSelections,
    @NotNull @PositiveOrZero Integer maximumSelections) {

  @AssertTrue(message = "maximumSelections must be greater than or equal to minimumSelections")
  public boolean hasValidSelectionLimits() {
    return minimumSelections == null
        || maximumSelections == null
        || maximumSelections >= minimumSelections;
  }
}
