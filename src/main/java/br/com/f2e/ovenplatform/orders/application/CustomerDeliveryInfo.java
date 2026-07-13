package br.com.f2e.ovenplatform.orders.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.normalizeOptional;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.util.UUID;

public record CustomerDeliveryInfo(
    UUID customerId, String customerName, String customerPhone, Address address) {

  public record Address(
      UUID addressId, String label, Line line, Location location, String reference) {

    public Address {
      requireNotNull(addressId, "addressId");
      label = normalizeOptional(label);
      requireNotNull(line, "line");
      requireNotNull(location, "location");
      reference = normalizeOptional(reference);
    }
  }

  public record Line(String addressLine1, String number, String complement) {

    public Line {
      addressLine1 = requireNotBlank(addressLine1, "addressLine1");
      number = requireNotBlank(number, "number");
      complement = normalizeOptional(complement);
    }
  }

  public record Location(String neighborhood, String city, String state, String postalCode) {

    public Location {
      neighborhood = requireNotBlank(neighborhood, "neighborhood");
      city = requireNotBlank(city, "city");
      state = requireNotBlank(state, "state");
      postalCode = requireNotBlank(postalCode, "postalCode");
    }
  }

  public CustomerDeliveryInfo {
    requireNotNull(customerId, "customerId");
    customerName = requireNotBlank(customerName, "customerName");
    customerPhone = requireNotBlank(customerPhone, "customerPhone");
    requireNotNull(address, "address");
  }
}
