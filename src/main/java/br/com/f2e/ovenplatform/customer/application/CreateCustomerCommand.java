package br.com.f2e.ovenplatform.customer.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireMinimumSize;

import br.com.f2e.ovenplatform.customer.domain.Customer;

public record CreateCustomerCommand(String name, String phone, String notes) {

  public CreateCustomerCommand {
    requireMinimumSize(name, "name", 2);
    Customer.normalizePhone(phone);
  }
}
