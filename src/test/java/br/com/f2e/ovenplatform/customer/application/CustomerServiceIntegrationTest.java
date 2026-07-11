package br.com.f2e.ovenplatform.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.customer.domain.Customer;
import br.com.f2e.ovenplatform.customer.infrastructure.persistence.JpaCustomerRepositoryAdapter;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.DataJpaIntegrationTest;
import br.com.f2e.ovenplatform.tenant.domain.Plan;
import br.com.f2e.ovenplatform.tenant.domain.Tenant;
import br.com.f2e.ovenplatform.tenant.infrastructure.persistence.SpringDataTenantRepository;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({CustomerService.class, JpaCustomerRepositoryAdapter.class})
class CustomerServiceIntegrationTest extends DataJpaIntegrationTest {

  private static final String CUSTOMER_NAME = "Maria";
  private static final String PHONE = "(11) 99999-8888";

  @Autowired private CustomerService customerService;
  @Autowired private SpringDataTenantRepository tenantRepository;

  @Test
  void shouldCreateCustomer() {
    var tenant = createTenant();

    var customer = createCustomer(tenant);

    assertThat(customer)
        .satisfies(
            created -> {
              assertThat(created.getId()).isNotNull();
              assertThat(created.getTenantId()).isEqualTo(tenant.getId());
              assertThat(created.getName()).isEqualTo(CUSTOMER_NAME);
              assertThat(created.getPhone()).isEqualTo(PHONE);
              assertThat(created.getNormalizedPhone()).isEqualTo("11999998888");
            });
  }

  @Test
  void shouldFindCustomerByIdAndTenantId() {
    var tenant = createTenant();
    var customer = createCustomer(tenant);

    var foundCustomer = customerService.findCustomer(tenant.getId(), customer.getId());

    assertThat(foundCustomer)
        .isPresent()
        .get()
        .extracting(Customer::getId)
        .isEqualTo(customer.getId());
  }

  @Test
  void shouldReturnEmptyWhenCustomerBelongsToAnotherTenant() {
    var ownerTenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var customer = createCustomer(ownerTenant);

    var foundCustomer = customerService.findCustomer(anotherTenant.getId(), customer.getId());

    assertThat(foundCustomer).isEmpty();
  }

  @Test
  void shouldFindCustomerByNormalizedPhoneWithinTenant() {
    var tenant = createTenant();
    var customer = createCustomer(tenant);

    var foundCustomer = customerService.findByPhone(tenant.getId(), "11 99999 8888");

    assertThat(foundCustomer)
        .isPresent()
        .get()
        .extracting(Customer::getId)
        .isEqualTo(customer.getId());
  }

  @Test
  void shouldListCustomersByTenant() {
    var tenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    var customer = createCustomer(tenant);
    createCustomer(anotherTenant, "Tony", "(11) 90000-0000");

    var customers = customerService.listCustomers(tenant.getId());

    assertThat(customers).extracting(Customer::getId).containsOnly(customer.getId());
  }

  @Test
  void shouldUpdateCustomer() {
    var tenant = createTenant();
    var customer = createCustomer(tenant);

    var updated =
        customerService.update(
            tenant.getId(),
            customer.getId(),
            new UpdateCustomerCommand("Joao", "(21) 98888-7777", "New note"));

    assertThat(updated.getName()).isEqualTo("Joao");
    assertThat(updated.getPhone()).isEqualTo("(21) 98888-7777");
    assertThat(updated.getNormalizedPhone()).isEqualTo("21988887777");
    assertThat(updated.getNotes()).isEqualTo("New note");
  }

  @Test
  void shouldEnforcePhoneUniquenessPerTenant() {
    var tenant = createTenant();
    createCustomer(tenant);
    createCustomer(tenant, "Joao", "11 99999 8888");

    assertThatThrownBy(this::flushAndClear).isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  void shouldAllowSamePhoneAcrossDifferentTenants() {
    var tenant = createTenant("Don Corleone Pizzeria");
    var anotherTenant = createTenant("Soprano Pizzeria");
    createCustomer(tenant);

    var customer = createCustomer(anotherTenant);

    assertThat(customer.getTenantId()).isEqualTo(anotherTenant.getId());
  }

  @Test
  void shouldAddUpdateAndRemoveAddress() {
    var tenant = createTenant();
    var customer = createCustomer(tenant);

    var withAddress =
        customerService.addAddress(tenant.getId(), customer.getId(), createAddressCommand());

    var address = withAddress.getAddresses().getFirst();
    assertThat(address.getId()).isNotNull();
    assertThat(address.getAddressLine1()).isEqualTo("Rua das Flores");

    var updated =
        customerService.updateAddress(
            tenant.getId(), customer.getId(), address.getId(), updateAddressCommand());

    assertThat(updated.getAddresses())
        .singleElement()
        .satisfies(
            updatedAddress -> {
              assertThat(updatedAddress.getAddressLine1()).isEqualTo("Av Paulista");
              assertThat(updatedAddress.getLabel()).isEqualTo("Work");
            });

    customerService.removeAddress(tenant.getId(), customer.getId(), address.getId());

    assertThat(customerService.getCustomer(tenant.getId(), customer.getId()).getAddresses())
        .isEmpty();
  }

  @Test
  void shouldThrowResourceNotFoundWhenUpdatingUnknownCustomer() {
    var tenant = createTenant();
    var customerId = UUID.randomUUID();
    var command = new UpdateCustomerCommand("Joao", PHONE, null);
    var tenantId = tenant.getId();

    assertThatThrownBy(() -> customerService.update(tenantId, customerId, command))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Customer id: %s not found".formatted(customerId));
  }

  private Tenant createTenant() {
    return createTenant("Don Corleone Pizzeria");
  }

  private Tenant createTenant(String name) {
    return tenantRepository.save(new Tenant(name, Plan.MVP));
  }

  private Customer createCustomer(Tenant tenant) {
    return createCustomer(tenant, CUSTOMER_NAME, PHONE);
  }

  private Customer createCustomer(Tenant tenant, String name, String phone) {
    return customerService.create(tenant.getId(), new CreateCustomerCommand(name, phone, null));
  }

  private CreateCustomerAddressCommand createAddressCommand() {
    return new CreateCustomerAddressCommand(
        "Home",
        "Rua das Flores",
        "123",
        null,
        "Centro",
        "Sao Paulo",
        "SP",
        "01000-000",
        "Portão azul");
  }

  private UpdateCustomerAddressCommand updateAddressCommand() {
    return new UpdateCustomerAddressCommand(
        "Work",
        "Av Paulista",
        "1000",
        "Sala 1",
        "Bela Vista",
        "Sao Paulo",
        "SP",
        "01310-100",
        null);
  }
}
