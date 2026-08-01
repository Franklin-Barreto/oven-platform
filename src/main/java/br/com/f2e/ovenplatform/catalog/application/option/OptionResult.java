package br.com.f2e.ovenplatform.catalog.application.option;

import br.com.f2e.ovenplatform.catalog.domain.Option;
import java.math.BigDecimal;
import java.util.UUID;

public record OptionResult(
    UUID id,
    UUID optionGroupId,
    String name,
    BigDecimal priceAdjustment,
    boolean active,
    int displayPosition) {

  public static OptionResult from(Option option) {
    return new OptionResult(
        option.getId(),
        option.getOptionGroupId(),
        option.getName(),
        option.getPriceAdjustment(),
        option.isActive(),
        option.getDisplayPosition());
  }
}
