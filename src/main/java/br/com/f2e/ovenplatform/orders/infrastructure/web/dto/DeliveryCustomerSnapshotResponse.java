package br.com.f2e.ovenplatform.orders.infrastructure.web.dto;

import br.com.f2e.ovenplatform.orders.domain.DeliveryCustomerSnapshot;
import java.util.UUID;

public record DeliveryCustomerSnapshotResponse(
    UUID customerId, String customerName, String customerPhone, Address address) {

  public record Address(
      UUID addressId, String label, Line line, Location location, String reference) {}

  public record Line(String addressLine1, String number, String complement) {}

  public record Location(String neighborhood, String city, String state, String postalCode) {}

  static DeliveryCustomerSnapshotResponse from(DeliveryCustomerSnapshot snapshot) {
    if (snapshot == null) {
      return null;
    }

    var address = snapshot.getAddress();
    var line = address.line();
    var location = address.location();

    return new DeliveryCustomerSnapshotResponse(
        snapshot.getCustomerId(),
        snapshot.getCustomerName(),
        snapshot.getCustomerPhone(),
        new Address(
            address.addressId(),
            address.label(),
            new Line(line.addressLine1(), line.number(), line.complement()),
            new Location(
                location.neighborhood(), location.city(), location.state(), location.postalCode()),
            address.reference()));
  }
}
