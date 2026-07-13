package br.com.f2e.ovenplatform.orders.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.normalizeOptional;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.util.UUID;

public record DeliveryAddressDetails(
    UUID addressId,
    String label,
    DeliveryAddressLine line,
    DeliveryAddressLocation location,
    String reference) {

  public DeliveryAddressDetails {
    requireNotNull(addressId, "addressId");
    label = normalizeOptional(label);
    requireNotNull(line, "line");
    requireNotNull(location, "location");
    reference = normalizeOptional(reference);
  }
}
