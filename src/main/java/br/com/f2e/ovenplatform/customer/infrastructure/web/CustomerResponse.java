package br.com.f2e.ovenplatform.customer.infrastructure.web;

import br.com.f2e.ovenplatform.customer.domain.Customer;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
    UUID id,
    UUID tenantId,
    String name,
    String phone,
    String normalizedPhone,
    String notes,
    List<CustomerAddressResponse> addresses) {

  public CustomerResponse {
    addresses = List.copyOf(addresses);
  }

  @Override
  public List<CustomerAddressResponse> addresses() {
    return List.copyOf(addresses);
  }

  static CustomerResponse from(Customer customer) {
    return new CustomerResponse(
        customer.getId(),
        customer.getTenantId(),
        customer.getName(),
        customer.getPhone(),
        customer.getNormalizedPhone(),
        customer.getNotes(),
        customer.getAddresses().stream().map(CustomerAddressResponse::from).toList());
  }
}
