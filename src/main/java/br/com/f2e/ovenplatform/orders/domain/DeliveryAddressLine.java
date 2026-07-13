package br.com.f2e.ovenplatform.orders.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.normalizeOptional;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

public record DeliveryAddressLine(String addressLine1, String number, String complement) {

  public DeliveryAddressLine {
    addressLine1 = requireNotBlank(addressLine1, "addressLine1");
    number = requireNotBlank(number, "number");
    complement = normalizeOptional(complement);
  }
}
