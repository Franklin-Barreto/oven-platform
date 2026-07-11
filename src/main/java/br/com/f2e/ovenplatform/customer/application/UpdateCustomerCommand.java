package br.com.f2e.ovenplatform.customer.application;

import static br.com.f2e.ovenplatform.shared.domain.validation.Preconditions.requireMinimumSize;

import br.com.f2e.ovenplatform.customer.domain.Customer;

public record UpdateCustomerCommand(String name, String phone, String notes) {

  public UpdateCustomerCommand {
    requireMinimumSize(name, "name", 2);
    Customer.normalizePhone(phone);
  }
}
