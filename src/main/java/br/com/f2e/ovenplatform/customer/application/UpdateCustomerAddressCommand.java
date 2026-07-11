package br.com.f2e.ovenplatform.customer.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;

public record UpdateCustomerAddressCommand(
    String label,
    String addressLine1,
    String number,
    String complement,
    String neighborhood,
    String city,
    String state,
    String postalCode,
    String reference) {

  public UpdateCustomerAddressCommand {
    requireNotBlank(addressLine1, "addressLine1");
    requireNotBlank(number, "number");
    requireNotBlank(neighborhood, "neighborhood");
    requireNotBlank(city, "city");
    requireNotBlank(state, "state");
    requireNotBlank(postalCode, "postalCode");
  }
}
