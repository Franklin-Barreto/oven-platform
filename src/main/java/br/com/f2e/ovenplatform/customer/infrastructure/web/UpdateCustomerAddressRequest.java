package br.com.f2e.ovenplatform.customer.infrastructure.web;

import br.com.f2e.ovenplatform.customer.application.UpdateCustomerAddressCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerAddressRequest(
    @Size(max = 40, message = "label must have at most 40 characters") String label,
    @NotBlank @Size(max = 120, message = "addressLine1 must have at most 120 characters")
        String addressLine1,
    @NotBlank @Size(max = 20, message = "number must have at most 20 characters") String number,
    @Size(max = 120, message = "complement must have at most 120 characters") String complement,
    @NotBlank @Size(max = 80, message = "neighborhood must have at most 80 characters")
        String neighborhood,
    @NotBlank @Size(max = 80, message = "city must have at most 80 characters") String city,
    @NotBlank @Size(max = 40, message = "state must have at most 40 characters") String state,
    @NotBlank @Size(max = 20, message = "postalCode must have at most 20 characters")
        String postalCode,
    @Size(max = 160, message = "reference must have at most 160 characters") String reference) {

  UpdateCustomerAddressCommand toCommand() {
    return new UpdateCustomerAddressCommand(
        label, addressLine1, number, complement, neighborhood, city, state, postalCode, reference);
  }
}
