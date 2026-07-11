package br.com.f2e.ovenplatform.customer.infrastructure.web;

import br.com.f2e.ovenplatform.customer.application.CreateCustomerCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
    @NotBlank @Size(min = 2, max = 80, message = "name must have between 2 and 80 characters")
        String name,
    @NotBlank @Size(max = 30, message = "phone must have at most 30 characters") String phone,
    @Size(max = 500, message = "notes must have at most 500 characters") String notes) {

  CreateCustomerCommand toCommand() {
    return new CreateCustomerCommand(name, phone, notes);
  }
}
