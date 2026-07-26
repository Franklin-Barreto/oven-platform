package br.com.f2e.ovenplatform.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

class OwnerProvisioningPropertiesTest {

  @Test
  void shouldAcceptValidProperties() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();
      var properties =
          new OwnerProvisioningProperties(
              "Don Corleone Pizzeria", "owner@oven.local", "OwnerPass123!");

      assertThat(validator.validate(properties)).isEmpty();
    }
  }

  @Test
  void shouldRejectInvalidProperties() {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();
      var properties = new OwnerProvisioningProperties("", "invalid-email", "short");

      assertThat(validator.validate(properties))
          .extracting(violation -> violation.getPropertyPath().toString())
          .contains("tenantName", "email", "password");
    }
  }
}
