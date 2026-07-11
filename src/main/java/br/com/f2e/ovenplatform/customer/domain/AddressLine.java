package br.com.f2e.ovenplatform.customer.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.normalizeOptional;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

public record AddressLine(String addressLine1, String number, String complement) {

  public AddressLine {
    addressLine1 = requireNotBlank(addressLine1, "addressLine1");
    number = requireNotBlank(number, "number");
    complement = normalizeOptional(complement);
  }

  static void requireValid(AddressLine addressLine) {
    requireNotNull(addressLine, "addressLine");
  }
}
