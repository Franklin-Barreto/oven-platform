package br.com.f2e.ovenplatform.customer.application.api;

import java.util.UUID;

public record CustomerDeliveryInfoResult(
    UUID customerId, String customerName, String customerPhone, Address address) {

  public record Address(
      UUID addressId, String label, Line line, Location location, String reference) {}

  public record Line(String addressLine1, String number, String complement) {}

  public record Location(String neighborhood, String city, String state, String postalCode) {}
}
