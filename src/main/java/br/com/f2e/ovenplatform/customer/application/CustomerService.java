package br.com.f2e.ovenplatform.customer.application;

import br.com.f2e.ovenplatform.customer.domain.AddressLine;
import br.com.f2e.ovenplatform.customer.domain.Customer;
import br.com.f2e.ovenplatform.customer.domain.CustomerAddressDetails;
import br.com.f2e.ovenplatform.customer.domain.CustomerAddressLocation;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

  private static final String RESOURCE = "Customer";

  private final CustomerRepository customerRepository;

  public CustomerService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  @Transactional
  public Customer create(UUID tenantId, CreateCustomerCommand command) {
    return customerRepository.save(
        Customer.create(tenantId, command.name(), command.phone(), command.notes()));
  }

  @Transactional(readOnly = true)
  public Optional<Customer> findCustomer(UUID tenantId, UUID customerId) {
    return customerRepository.findByIdAndTenantId(customerId, tenantId);
  }

  @Transactional(readOnly = true)
  public Customer getCustomer(UUID tenantId, UUID customerId) {
    return findRequiredCustomer(tenantId, customerId);
  }

  @Transactional(readOnly = true)
  public Optional<Customer> findByPhone(UUID tenantId, String phone) {
    return customerRepository.findByTenantIdAndNormalizedPhone(
        tenantId, Customer.normalizePhone(phone));
  }

  @Transactional(readOnly = true)
  public List<Customer> listCustomers(UUID tenantId) {
    return customerRepository.findByTenantId(tenantId);
  }

  @Transactional
  public Customer update(UUID tenantId, UUID customerId, UpdateCustomerCommand command) {
    var customer = findRequiredCustomer(tenantId, customerId);
    customer.update(command.name(), command.phone(), command.notes());
    return customerRepository.save(customer);
  }

  @Transactional
  public Customer addAddress(UUID tenantId, UUID customerId, CreateCustomerAddressCommand command) {
    var customer = findRequiredCustomer(tenantId, customerId);
    customer.addAddress(toAddressDetails(command));
    return customerRepository.save(customer);
  }

  @Transactional
  public Customer updateAddress(
      UUID tenantId, UUID customerId, UUID addressId, UpdateCustomerAddressCommand command) {
    var customer = findRequiredCustomer(tenantId, customerId);
    customer.updateAddress(addressId, toAddressDetails(command));
    return customerRepository.save(customer);
  }

  @Transactional
  public void removeAddress(UUID tenantId, UUID customerId, UUID addressId) {
    var customer = findRequiredCustomer(tenantId, customerId);
    customer.removeAddress(addressId);
    customerRepository.save(customer);
  }

  private Customer findRequiredCustomer(UUID tenantId, UUID customerId) {
    return customerRepository
        .findByIdAndTenantId(customerId, tenantId)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, customerId));
  }

  private CustomerAddressDetails toAddressDetails(CreateCustomerAddressCommand command) {
    return new CustomerAddressDetails(
        command.label(),
        new AddressLine(command.addressLine1(), command.number(), command.complement()),
        new CustomerAddressLocation(
            command.neighborhood(), command.city(), command.state(), command.postalCode()),
        command.reference());
  }

  private CustomerAddressDetails toAddressDetails(UpdateCustomerAddressCommand command) {
    return new CustomerAddressDetails(
        command.label(),
        new AddressLine(command.addressLine1(), command.number(), command.complement()),
        new CustomerAddressLocation(
            command.neighborhood(), command.city(), command.state(), command.postalCode()),
        command.reference());
  }
}
