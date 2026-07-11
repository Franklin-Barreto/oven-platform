package br.com.f2e.ovenplatform.customer.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.normalizeOptional;

public record CustomerAddressDetails(
    String label, AddressLine addressLine, CustomerAddressLocation location, String reference) {

  public CustomerAddressDetails {
    AddressLine.requireValid(addressLine);
    CustomerAddressLocation.requireValid(location);
    label = normalizeOptional(label);
    reference = normalizeOptional(reference);
  }
}
