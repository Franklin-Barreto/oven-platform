package br.com.f2e.ovenplatform.customer.application;

import br.com.f2e.ovenplatform.customer.domain.Customer;

public record UpdateCustomerCommand(String name, String phone, String notes) {

  public UpdateCustomerCommand {
    name = Customer.requireName(name);
    Customer.normalizePhone(phone);
  }
}
