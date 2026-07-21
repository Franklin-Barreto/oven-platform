package br.com.f2e.ovenplatform.customer.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;

import br.com.f2e.ovenplatform.customer.application.CustomerService;
import br.com.f2e.ovenplatform.identity.application.api.security.CurrentTenantId;
import br.com.f2e.ovenplatform.shared.infrastructure.web.ResourceUriBuilder;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

  private final CustomerService customerService;

  public CustomerController(CustomerService customerService) {
    this.customerService = customerService;
  }

  @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE)
  public ResponseEntity<CustomerResponse> create(
      @CurrentTenantId UUID tenantId, @Valid @RequestBody CreateCustomerRequest request) {
    var customer = CustomerResponse.from(customerService.create(tenantId, request.toCommand()));
    var uri = ResourceUriBuilder.buildLocation(customer.id());
    return ResponseEntity.created(uri).body(customer);
  }

  @PreAuthorize("hasAuthority('CUSTOMER_READ')")
  @GetMapping(version = API_VERSION_VALUE)
  public ResponseEntity<List<CustomerResponse>> list(
      @CurrentTenantId UUID tenantId, @RequestParam(required = false) String phone) {
    var customers =
        phone == null
            ? customerService.listCustomers(tenantId)
            : customerService.findByPhone(tenantId, phone).stream().toList();

    return ResponseEntity.ok(customers.stream().map(CustomerResponse::from).toList());
  }

  @PreAuthorize("hasAuthority('CUSTOMER_READ')")
  @GetMapping(version = API_VERSION_VALUE, path = "/{id}")
  public ResponseEntity<CustomerResponse> find(
      @CurrentTenantId UUID tenantId, @PathVariable UUID id) {
    return ResponseEntity.ok(CustomerResponse.from(customerService.getCustomer(tenantId, id)));
  }

  @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
  @PatchMapping(version = API_VERSION_VALUE, path = "/{id}")
  public ResponseEntity<CustomerResponse> update(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCustomerRequest request) {
    return ResponseEntity.ok(
        CustomerResponse.from(customerService.update(tenantId, id, request.toCommand())));
  }

  @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
  @PostMapping(version = API_VERSION_VALUE, path = "/{customerId}/addresses")
  public ResponseEntity<CustomerResponse> addAddress(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID customerId,
      @Valid @RequestBody CreateCustomerAddressRequest request) {
    return ResponseEntity.ok(
        CustomerResponse.from(
            customerService.addAddress(tenantId, customerId, request.toCommand())));
  }

  @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
  @PatchMapping(version = API_VERSION_VALUE, path = "/{customerId}/addresses/{addressId}")
  public ResponseEntity<CustomerResponse> updateAddress(
      @CurrentTenantId UUID tenantId,
      @PathVariable UUID customerId,
      @PathVariable UUID addressId,
      @Valid @RequestBody UpdateCustomerAddressRequest request) {
    return ResponseEntity.ok(
        CustomerResponse.from(
            customerService.updateAddress(tenantId, customerId, addressId, request.toCommand())));
  }

  @PreAuthorize("hasAuthority('CUSTOMER_MANAGE')")
  @DeleteMapping(version = API_VERSION_VALUE, path = "/{customerId}/addresses/{addressId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAddress(
      @CurrentTenantId UUID tenantId, @PathVariable UUID customerId, @PathVariable UUID addressId) {
    customerService.removeAddress(tenantId, customerId, addressId);
  }
}
