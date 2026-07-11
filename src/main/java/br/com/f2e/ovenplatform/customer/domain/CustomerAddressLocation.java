package br.com.f2e.ovenplatform.customer.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

public record CustomerAddressLocation(
    String neighborhood, String city, String state, String postalCode) {

  public CustomerAddressLocation {
    neighborhood = requireNotBlank(neighborhood, "neighborhood");
    city = requireNotBlank(city, "city");
    state = requireNotBlank(state, "state");
    postalCode = requireNotBlank(postalCode, "postalCode");
  }

  static void requireValid(CustomerAddressLocation location) {
    requireNotNull(location, "location");
  }
}
