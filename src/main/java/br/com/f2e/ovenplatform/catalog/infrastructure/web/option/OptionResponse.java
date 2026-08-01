package br.com.f2e.ovenplatform.catalog.infrastructure.web.option;

import br.com.f2e.ovenplatform.catalog.application.option.OptionResult;
import java.math.BigDecimal;
import java.util.UUID;

public record OptionResponse(
    UUID id,
    UUID optionGroupId,
    UUID tenantId,
    String name,
    BigDecimal priceAdjustment,
    boolean active,
    int displayPosition) {

  public static OptionResponse from(OptionResult option) {
    return new OptionResponse(
        option.id(),
        option.optionGroupId(),
        option.tenantId(),
        option.name(),
        option.priceAdjustment(),
        option.active(),
        option.displayPosition());
  }
}
