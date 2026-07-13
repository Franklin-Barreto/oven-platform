package br.com.f2e.ovenplatform.orders.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

public record DeliveryAddressLocation(
    String neighborhood, String city, String state, String postalCode) {

  public DeliveryAddressLocation {
    neighborhood = requireNotBlank(neighborhood, "neighborhood");
    city = requireNotBlank(city, "city");
    state = requireNotBlank(state, "state");
    postalCode = requireNotBlank(postalCode, "postalCode");
  }
}
