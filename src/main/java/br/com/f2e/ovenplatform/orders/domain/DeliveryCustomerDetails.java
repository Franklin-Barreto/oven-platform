package br.com.f2e.ovenplatform.orders.domain;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotBlank;
import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireNotNull;

import java.util.UUID;

public record DeliveryCustomerDetails(
    UUID customerId, String customerName, String customerPhone, DeliveryAddressDetails address) {

  public DeliveryCustomerDetails {
    requireNotNull(customerId, "customerId");
    customerName = requireNotBlank(customerName, "customerName");
    customerPhone = requireNotBlank(customerPhone, "customerPhone");
    requireNotNull(address, "address");
  }
}
