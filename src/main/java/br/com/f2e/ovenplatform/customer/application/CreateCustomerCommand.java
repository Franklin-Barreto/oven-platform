package br.com.f2e.ovenplatform.customer.application;

import br.com.f2e.ovenplatform.customer.domain.Customer;

public record CreateCustomerCommand(String name, String phone, String notes) {

  public CreateCustomerCommand {
    name = Customer.requireName(name);
    Customer.normalizePhone(phone);
  }
}
