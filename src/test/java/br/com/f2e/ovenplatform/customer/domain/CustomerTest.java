package br.com.f2e.ovenplatform.customer.domain;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withRandomId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerTest {

  private static final UUID TENANT_ID = UUID.fromString("8325813e-3202-4ec3-9ad2-d86b922fb522");

  @Test
  void shouldCreateCustomerWithNormalizedPhone() {
    var customer = Customer.create(TENANT_ID, "Maria", "(11) 99999-8888", "Prefers WhatsApp");

    assertThat(customer.getTenantId()).isEqualTo(TENANT_ID);
    assertThat(customer.getName()).isEqualTo("Maria");
    assertThat(customer.getPhone()).isEqualTo("(11) 99999-8888");
    assertThat(customer.getNormalizedPhone()).isEqualTo("11999998888");
    assertThat(customer.getNotes()).isEqualTo("Prefers WhatsApp");
    assertThat(customer.getAddresses()).isEmpty();
  }

  @Test
  void shouldRejectCustomerWithoutRequiredFields() {
    assertThatThrownBy(() -> Customer.create(null, "Maria", "(11) 99999-8888", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("tenantId must not be null");

    assertThatThrownBy(() -> Customer.create(TENANT_ID, "M", "(11) 99999-8888", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("name must have at least 2 characters");

    assertThatThrownBy(() -> Customer.create(TENANT_ID, "Maria", "no digits", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("phone must contain at least one digit");
  }

  @Test
  void shouldAddUpdateAndRemoveAddress() {
    var customer = Customer.create(TENANT_ID, "Maria", "(11) 99999-8888", null);
    var address = withRandomId(customer.addAddress(homeAddressDetails()));

    customer.updateAddress(address.getId(), workAddressDetails());

    assertThat(customer.getAddresses())
        .singleElement()
        .satisfies(
            updated -> {
              assertThat(updated.getLabel()).isEqualTo("Work");
              assertThat(updated.getAddressLine1()).isEqualTo("Av Paulista");
              assertThat(updated.getComplement()).isEqualTo("Sala 1");
              assertThat(updated.getReference()).isNull();
            });

    customer.removeAddress(address.getId());

    assertThat(customer.getAddresses()).isEmpty();
  }

  @Test
  void shouldExposeReadOnlyAddresses() {
    var customer = Customer.create(TENANT_ID, "Maria", "(11) 99999-8888", null);
    var addresses = customer.getAddresses();

    assertThatThrownBy(addresses::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  private CustomerAddressDetails homeAddressDetails() {
    return new CustomerAddressDetails(
        "Home",
        new AddressLine("Rua das Flores", "123", null),
        new CustomerAddressLocation("Centro", "Sao Paulo", "SP", "01000-000"),
        "Portao azul");
  }

  private CustomerAddressDetails workAddressDetails() {
    return new CustomerAddressDetails(
        "Work",
        new AddressLine("Av Paulista", "1000", "Sala 1"),
        new CustomerAddressLocation("Bela Vista", "Sao Paulo", "SP", "01310-100"),
        null);
  }
}
