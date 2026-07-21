package br.com.f2e.ovenplatform.customer.infrastructure.web;

import static br.com.f2e.ovenplatform.identity.infrastructure.security.test.SecurityTestRequestPostProcessors.authenticatedTenantUser;
import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withRandomId;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.LocationHeaderAssertions.assertLocationPath;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.customer.application.CreateCustomerAddressCommand;
import br.com.f2e.ovenplatform.customer.application.CreateCustomerCommand;
import br.com.f2e.ovenplatform.customer.application.CustomerService;
import br.com.f2e.ovenplatform.customer.application.UpdateCustomerAddressCommand;
import br.com.f2e.ovenplatform.customer.application.UpdateCustomerCommand;
import br.com.f2e.ovenplatform.customer.domain.AddressLine;
import br.com.f2e.ovenplatform.customer.domain.Customer;
import br.com.f2e.ovenplatform.customer.domain.CustomerAddressDetails;
import br.com.f2e.ovenplatform.customer.domain.CustomerAddressLocation;
import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = {CustomerController.class})
class CustomerControllerTest extends AbstractControllerTest {

  private static final String BASE_URL = "/customers";
  private static final UUID CUSTOMER_ID = UUID.fromString("70d86c89-69d6-4cda-9bba-fc3dac1c5b4b");
  private static final UUID ADDRESS_ID = UUID.fromString("52f812c6-83d5-4610-ad59-f0bbe8cbb6e9");

  @MockitoBean private CustomerService customerService;

  @Test
  void shouldCreateCustomerUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var customer = withRandomId(Customer.create(TENANT_ID, "Maria", "(11) 99999-8888", "Notes"));

    when(customerService.create(TENANT_ID, createCustomerCommand())).thenReturn(customer);

    var result =
        mockMvc
            .perform(
                post(BASE_URL)
                    .with(
                        authenticatedTenantUser(
                            TENANT_ID,
                            TenantMembershipRole.MANAGER,
                            TenantPermission.CUSTOMER_MANAGE))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(createCustomerRequest()))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
            .andExpect(jsonPath("$.name").value("Maria"))
            .andExpect(jsonPath("$.phone").value("(11) 99999-8888"))
            .andExpect(jsonPath("$.normalizedPhone").value("11999998888"))
            .andReturn();

    assertLocationPath(result, BASE_URL + "/" + customer.getId());

    verify(customerService).create(TENANT_ID, createCustomerCommand());
  }

  @Test
  void shouldReturn400WhenCreateRequestIsInvalid() throws Exception {
    var request = new CreateCustomerRequest(null, "(11) 99999-8888", null);

    mockMvc
        .perform(
            post(BASE_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CUSTOMER_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                "must not be blank",
                "name",
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(customerService);
  }

  @Test
  void shouldListCustomersUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var customer = withRandomId(Customer.create(TENANT_ID, "Maria", "(11) 99999-8888", null));

    when(customerService.listCustomers(TENANT_ID)).thenReturn(List.of(customer));

    mockMvc
        .perform(
            get(BASE_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CUSTOMER_READ))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$[0].name").value("Maria"));

    verify(customerService).listCustomers(TENANT_ID);
  }

  @Test
  void shouldLookupCustomerByPhoneUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var customer = withRandomId(Customer.create(TENANT_ID, "Maria", "(11) 99999-8888", null));

    when(customerService.findByPhone(TENANT_ID, "11 99999 8888"))
        .thenReturn(java.util.Optional.of(customer));

    mockMvc
        .perform(
            get(BASE_URL)
                .queryParam("phone", "11 99999 8888")
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CUSTOMER_READ))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").value(customer.getId().toString()));

    verify(customerService).findByPhone(TENANT_ID, "11 99999 8888");
  }

  @Test
  void shouldFindCustomerByIdUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var customer = withRandomId(Customer.create(TENANT_ID, "Maria", "(11) 99999-8888", null));

    when(customerService.getCustomer(TENANT_ID, CUSTOMER_ID)).thenReturn(customer);

    mockMvc
        .perform(
            get(BASE_URL + "/" + CUSTOMER_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CUSTOMER_READ))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(customer.getId().toString()));

    verify(customerService).getCustomer(TENANT_ID, CUSTOMER_ID);
  }

  @Test
  void shouldReturn404WhenCustomerDoesNotExist() throws Exception {
    when(customerService.getCustomer(TENANT_ID, CUSTOMER_ID))
        .thenThrow(new ResourceNotFoundException("Customer", CUSTOMER_ID));

    mockMvc
        .perform(
            get(BASE_URL + "/" + CUSTOMER_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CUSTOMER_READ))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Customer id: %s not found".formatted(CUSTOMER_ID)));

    verify(customerService).getCustomer(TENANT_ID, CUSTOMER_ID);
  }

  @Test
  void shouldUpdateCustomerUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var customer = withRandomId(Customer.create(TENANT_ID, "Joao", "(21) 98888-7777", null));

    when(customerService.update(TENANT_ID, CUSTOMER_ID, updateCustomerCommand()))
        .thenReturn(customer);

    mockMvc
        .perform(
            patch(BASE_URL + "/" + CUSTOMER_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CUSTOMER_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateCustomerRequest()))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Joao"))
        .andExpect(jsonPath("$.normalizedPhone").value("21988887777"));

    verify(customerService).update(TENANT_ID, CUSTOMER_ID, updateCustomerCommand());
  }

  @Test
  void shouldAddAddressUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var customer = customerWithAddress("Home");

    when(customerService.addAddress(TENANT_ID, CUSTOMER_ID, createAddressCommand()))
        .thenReturn(customer);

    mockMvc
        .perform(
            post(BASE_URL + "/" + CUSTOMER_ID + "/addresses")
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CUSTOMER_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(createAddressRequest()))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.addresses[0].id").value(ADDRESS_ID.toString()))
        .andExpect(jsonPath("$.addresses[0].addressLine1").value("Rua das Flores"));

    verify(customerService).addAddress(TENANT_ID, CUSTOMER_ID, createAddressCommand());
  }

  @Test
  void shouldUpdateAddressUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var customer = customerWithAddress("Work");

    when(customerService.updateAddress(TENANT_ID, CUSTOMER_ID, ADDRESS_ID, updateAddressCommand()))
        .thenReturn(customer);

    mockMvc
        .perform(
            patch(BASE_URL + "/" + CUSTOMER_ID + "/addresses/" + ADDRESS_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CUSTOMER_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateAddressRequest()))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.addresses[0].label").value("Work"));

    verify(customerService)
        .updateAddress(TENANT_ID, CUSTOMER_ID, ADDRESS_ID, updateAddressCommand());
  }

  @Test
  void shouldRemoveAddressUsingTenantFromAuthenticatedPrincipal() throws Exception {
    mockMvc
        .perform(
            delete(BASE_URL + "/" + CUSTOMER_ID + "/addresses/" + ADDRESS_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CUSTOMER_MANAGE))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(customerService).removeAddress(TENANT_ID, CUSTOMER_ID, ADDRESS_ID);
  }

  @Test
  void shouldReturn404WhenRemovingAddressFromUnknownCustomer() throws Exception {
    doThrow(new ResourceNotFoundException("Customer", CUSTOMER_ID))
        .when(customerService)
        .removeAddress(TENANT_ID, CUSTOMER_ID, ADDRESS_ID);

    mockMvc
        .perform(
            delete(BASE_URL + "/" + CUSTOMER_ID + "/addresses/" + ADDRESS_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CUSTOMER_MANAGE))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Customer id: %s not found".formatted(CUSTOMER_ID)));

    verify(customerService).removeAddress(TENANT_ID, CUSTOMER_ID, ADDRESS_ID);
  }

  @ParameterizedTest
  @MethodSource("customerProtectedRequests")
  void shouldReturnForbiddenWhenCustomerPermissionIsMissing(MockHttpServletRequestBuilder request)
      throws Exception {
    mockMvc
        .perform(
            request.with(
                authenticatedTenantUser(
                    TENANT_ID, TenantMembershipRole.KITCHEN, TenantPermission.KITCHEN_READ)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(customerService);
  }

  @Test
  void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
    mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());

    verifyNoInteractions(customerService);
  }

  private static Stream<Arguments> customerProtectedRequests() {
    return Stream.of(
        Arguments.of(get(BASE_URL)),
        Arguments.of(get(BASE_URL + "/" + CUSTOMER_ID)),
        Arguments.of(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(createCustomerRequest()))),
        Arguments.of(
            patch(BASE_URL + "/" + CUSTOMER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateCustomerRequest()))),
        Arguments.of(
            post(BASE_URL + "/" + CUSTOMER_ID + "/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(createAddressRequest()))),
        Arguments.of(
            patch(BASE_URL + "/" + CUSTOMER_ID + "/addresses/" + ADDRESS_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateAddressRequest()))),
        Arguments.of(delete(BASE_URL + "/" + CUSTOMER_ID + "/addresses/" + ADDRESS_ID)));
  }

  private Customer customerWithAddress(String label) {
    var customer = withRandomId(Customer.create(TENANT_ID, "Maria", "(11) 99999-8888", null));
    withId(customer.addAddress(addressDetails(label)), ADDRESS_ID);
    return customer;
  }

  private CustomerAddressDetails addressDetails(String label) {
    return new CustomerAddressDetails(
        label,
        new AddressLine("Rua das Flores", "123", null),
        new CustomerAddressLocation("Centro", "Sao Paulo", "SP", "01000-000"),
        "Portao azul");
  }

  private static CreateCustomerRequest createCustomerRequest() {
    return new CreateCustomerRequest("Maria", "(11) 99999-8888", "Notes");
  }

  private CreateCustomerCommand createCustomerCommand() {
    return new CreateCustomerCommand("Maria", "(11) 99999-8888", "Notes");
  }

  private static UpdateCustomerRequest updateCustomerRequest() {
    return new UpdateCustomerRequest("Joao", "(21) 98888-7777", null);
  }

  private UpdateCustomerCommand updateCustomerCommand() {
    return new UpdateCustomerCommand("Joao", "(21) 98888-7777", null);
  }

  private static CreateCustomerAddressRequest createAddressRequest() {
    return new CreateCustomerAddressRequest(
        "Home",
        "Rua das Flores",
        "123",
        null,
        "Centro",
        "Sao Paulo",
        "SP",
        "01000-000",
        "Portao azul");
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
        "Portao azul");
  }

  private static UpdateCustomerAddressRequest updateAddressRequest() {
    return new UpdateCustomerAddressRequest(
        "Work", "Rua das Flores", "123", null, "Centro", "Sao Paulo", "SP", "01000-000", null);
  }

  private UpdateCustomerAddressCommand updateAddressCommand() {
    return new UpdateCustomerAddressCommand(
        "Work", "Rua das Flores", "123", null, "Centro", "Sao Paulo", "SP", "01000-000", null);
  }
}
