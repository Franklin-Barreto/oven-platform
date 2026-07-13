package br.com.f2e.ovenplatform.customer.application;

import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoLookup;
import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoResult;
import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoResult.Address;
import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoResult.Line;
import br.com.f2e.ovenplatform.customer.application.api.CustomerDeliveryInfoResult.Location;
import br.com.f2e.ovenplatform.customer.domain.Customer;
import br.com.f2e.ovenplatform.customer.domain.CustomerAddress;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerDeliveryInfoLookupService implements CustomerDeliveryInfoLookup {

  private final CustomerService customerService;

  public CustomerDeliveryInfoLookupService(CustomerService customerService) {
    this.customerService = customerService;
  }

  @Transactional(readOnly = true)
  @Override
  public CustomerDeliveryInfoResult findDeliveryInfo(
      UUID tenantId, UUID customerId, UUID customerAddressId) {
    var customer = customerService.getCustomer(tenantId, customerId);
    var customerAddress = getCustomerAddress(customer, customerAddressId);

    return toResult(customer, customerAddress);
  }

  private CustomerAddress getCustomerAddress(Customer customer, UUID customerAddressId) {
    try {
      return customer.getAddress(customerAddressId);
    } catch (NoSuchElementException _) {
      throw new ResourceNotFoundException("CustomerAddress", customerAddressId);
    }
  }

  private CustomerDeliveryInfoResult toResult(Customer customer, CustomerAddress customerAddress) {
    var line =
        new Line(
            customerAddress.getAddressLine1(),
            customerAddress.getNumber(),
            customerAddress.getComplement());

    var location =
        new Location(
            customerAddress.getNeighborhood(),
            customerAddress.getCity(),
            customerAddress.getState(),
            customerAddress.getPostalCode());

    var address =
        new Address(
            customerAddress.getId(),
            customerAddress.getLabel(),
            line,
            location,
            customerAddress.getReference());

    return new CustomerDeliveryInfoResult(
        customer.getId(), customer.getName(), customer.getPhone(), address);
  }
}
