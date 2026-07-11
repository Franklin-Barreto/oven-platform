package br.com.f2e.ovenplatform.customer.infrastructure.web;

import br.com.f2e.ovenplatform.customer.domain.CustomerAddress;
import java.util.UUID;

public record CustomerAddressResponse(
    UUID id,
    String label,
    String addressLine1,
    String number,
    String complement,
    String neighborhood,
    String city,
    String state,
    String postalCode,
    String reference) {

  static CustomerAddressResponse from(CustomerAddress address) {
    return new CustomerAddressResponse(
        address.getId(),
        address.getLabel(),
        address.getAddressLine1(),
        address.getNumber(),
        address.getComplement(),
        address.getNeighborhood(),
        address.getCity(),
        address.getState(),
        address.getPostalCode(),
        address.getReference());
  }
}
