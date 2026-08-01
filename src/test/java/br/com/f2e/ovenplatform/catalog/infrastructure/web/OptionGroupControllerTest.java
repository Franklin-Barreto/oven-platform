package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import static br.com.f2e.ovenplatform.identity.infrastructure.security.test.SecurityTestRequestPostProcessors.authenticatedTenantUser;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.LocationHeaderAssertions.assertLocationPath;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.catalog.application.optiongroup.CreateOptionGroupCommand;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupResult;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.OptionGroupService;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.ReorderOptionGroupsCommand;
import br.com.f2e.ovenplatform.catalog.application.optiongroup.UpdateOptionGroupCommand;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup.ChangeOptionGroupStatusRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup.CreateOptionGroupRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup.OptionGroupController;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup.ReorderOptionGroupsRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.optiongroup.UpdateOptionGroupRequest;
import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(OptionGroupController.class)
class OptionGroupControllerTest extends AbstractControllerTest {

  private static final UUID PRODUCT_ID = UUID.fromString("22b2759d-35b2-4b04-ab39-df2a203a652c");
  private static final UUID OPTION_GROUP_ID =
      UUID.fromString("431e5f0b-a762-4d31-846f-e4fe83db6b19");
  private static final UUID SECOND_OPTION_GROUP_ID =
      UUID.fromString("63cd1f20-2023-4073-8421-bdf92a0b24f4");
  private static final String BASE_URL = "/products/" + PRODUCT_ID + "/option-groups";

  @MockitoBean private OptionGroupService service;

  @Test
  void shouldCreateOptionGroup() throws Exception {
    var command = new CreateOptionGroupCommand("Extras", 1, 3);
    when(service.create(TENANT_ID, PRODUCT_ID, command)).thenReturn(result(true));

    var mvcResult =
        mockMvc
            .perform(
                post(BASE_URL)
                    .with(manager())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(new CreateOptionGroupRequest("Extras", 1, 3))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(OPTION_GROUP_ID.toString()))
            .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
            .andExpect(jsonPath("$.name").value("Extras"))
            .andExpect(jsonPath("$.minimumSelections").value(1))
            .andExpect(jsonPath("$.maximumSelections").value(3))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.displayPosition").value(0))
            .andReturn();

    assertLocationPath(mvcResult, BASE_URL + "/" + OPTION_GROUP_ID);
    verify(service).create(TENANT_ID, PRODUCT_ID, command);
  }

  @Test
  void shouldListOptionGroups() throws Exception {
    when(service.list(TENANT_ID, PRODUCT_ID)).thenReturn(List.of(result(false)));

    mockMvc
        .perform(get(BASE_URL).with(reader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].active").value(false))
        .andExpect(jsonPath("$[0].name").value("Extras"));
    verify(service).list(TENANT_ID, PRODUCT_ID);
  }

  @Test
  void shouldUpdateOptionGroup() throws Exception {
    var command = new UpdateOptionGroupCommand("Extras", 1, 3);
    when(service.update(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, command)).thenReturn(result(true));

    mockMvc
        .perform(
            put(BASE_URL + "/" + OPTION_GROUP_ID)
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new UpdateOptionGroupRequest("Extras", 1, 3))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(OPTION_GROUP_ID.toString()));
    verify(service).update(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, command);
  }

  @Test
  void shouldChangeOptionGroupStatus() throws Exception {
    mockMvc
        .perform(
            put(BASE_URL + "/" + OPTION_GROUP_ID + "/status")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new ChangeOptionGroupStatusRequest(false))))
        .andExpect(status().isNoContent());
    verify(service).changeStatus(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, false);
  }

  @Test
  void shouldReorderOptionGroups() throws Exception {
    var request = new ReorderOptionGroupsRequest(List.of(SECOND_OPTION_GROUP_ID, OPTION_GROUP_ID));
    var command = new ReorderOptionGroupsCommand(request.optionGroupIds());
    mockMvc
        .perform(
            put(BASE_URL + "/display-order")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isNoContent());
    verify(service).reorder(TENANT_ID, PRODUCT_ID, command);
  }

  @ParameterizedTest
  @MethodSource("invalidMutationRequests")
  void shouldRejectInvalidMutationRequests(MockHttpServletRequestBuilder request) throws Exception {
    mockMvc.perform(request.with(manager())).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void shouldReturnNotFoundFromService() throws Exception {
    when(service.list(TENANT_ID, PRODUCT_ID))
        .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));
    mockMvc
        .perform(get(BASE_URL).with(reader()))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Product id: %s not found".formatted(PRODUCT_ID)));
  }

  @ParameterizedTest
  @MethodSource("mutationRequests")
  void shouldForbidMutationsWithoutCatalogManagePermission(MockHttpServletRequestBuilder request)
      throws Exception {
    mockMvc.perform(request.with(reader())).andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void shouldForbidListWithoutCatalogReadPermission() throws Exception {
    mockMvc
        .perform(
            get(BASE_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.KITCHEN, TenantPermission.KITCHEN_READ)))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  private static Stream<MockHttpServletRequestBuilder> invalidMutationRequests() {
    return Stream.of(
        post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new CreateOptionGroupRequest("", 2, 1))),
        put(BASE_URL + "/" + OPTION_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new UpdateOptionGroupRequest("", 2, 1))),
        put(BASE_URL + "/" + OPTION_GROUP_ID + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new ChangeOptionGroupStatusRequest(null))),
        put(BASE_URL + "/display-order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new ReorderOptionGroupsRequest(null))));
  }

  private static Stream<MockHttpServletRequestBuilder> mutationRequests() {
    return Stream.of(
        post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new CreateOptionGroupRequest("Extras", 1, 3))),
        put(BASE_URL + "/" + OPTION_GROUP_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new UpdateOptionGroupRequest("Extras", 1, 3))),
        put(BASE_URL + "/" + OPTION_GROUP_ID + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new ChangeOptionGroupStatusRequest(false))),
        put(BASE_URL + "/display-order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new ReorderOptionGroupsRequest(List.of(OPTION_GROUP_ID)))));
  }

  private static RequestPostProcessor manager() {
    return authenticatedTenantUser(
        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE);
  }

  private static RequestPostProcessor reader() {
    return authenticatedTenantUser(
        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CATALOG_READ);
  }

  private static OptionGroupResult result(boolean active) {
    return new OptionGroupResult(OPTION_GROUP_ID, PRODUCT_ID, "Extras", 1, 3, active, 0);
  }
}
