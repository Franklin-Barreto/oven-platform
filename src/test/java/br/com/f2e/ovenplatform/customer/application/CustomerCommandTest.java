package br.com.f2e.ovenplatform.customer.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CustomerCommandTest {

  @Test
  void shouldValidateCreateCustomerCommand() {
    assertThatThrownBy(() -> new CreateCustomerCommand(null, "(11) 99999-8888", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must not be null");

    assertThatThrownBy(() -> new CreateCustomerCommand("Maria", "no digits", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("phone must contain at least one digit");
  }

  @Test
  void shouldValidateUpdateCustomerCommand() {
    var longName = "M".repeat(81);

    assertThatThrownBy(() -> new UpdateCustomerCommand("M", "(11) 99999-8888", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must have at least 2 characters");

    assertThatThrownBy(() -> new UpdateCustomerCommand(longName, "(11) 99999-8888", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must have at most 80 characters");

    assertThatThrownBy(() -> new UpdateCustomerCommand("Maria", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("phone must not be null");
  }

  @Test
  void shouldValidateCreateAddressCommand() {
    assertThatThrownBy(
            () ->
                new CreateCustomerAddressCommand(
                    "Home", null, "123", null, "Centro", "Sao Paulo", "SP", "01000-000", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("addressLine1 must not be null");
  }

  @Test
  void shouldValidateUpdateAddressCommand() {
    assertThatThrownBy(
            () ->
                new UpdateCustomerAddressCommand(
                    "Home",
                    "Rua das Flores",
                    "123",
                    null,
                    "",
                    "Sao Paulo",
                    "SP",
                    "01000-000",
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("neighborhood must not be blank");
  }
}
