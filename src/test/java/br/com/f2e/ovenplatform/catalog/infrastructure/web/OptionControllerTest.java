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

import br.com.f2e.ovenplatform.catalog.application.option.CreateOptionCommand;
import br.com.f2e.ovenplatform.catalog.application.option.OptionResult;
import br.com.f2e.ovenplatform.catalog.application.option.OptionService;
import br.com.f2e.ovenplatform.catalog.application.option.ReorderOptionsCommand;
import br.com.f2e.ovenplatform.catalog.application.option.UpdateOptionCommand;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.option.ChangeOptionStatusRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.option.CreateOptionRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.option.OptionController;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.option.ReorderOptionsRequest;
import br.com.f2e.ovenplatform.catalog.infrastructure.web.option.UpdateOptionRequest;
import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.math.BigDecimal;
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

@WebMvcTest(OptionController.class)
class OptionControllerTest extends AbstractControllerTest {

  private static final UUID PRODUCT_ID = UUID.fromString("22b2759d-35b2-4b04-ab39-df2a203a652c");
  private static final UUID OPTION_GROUP_ID =
      UUID.fromString("431e5f0b-a762-4d31-846f-e4fe83db6b19");
  private static final UUID OPTION_ID = UUID.fromString("3e9cba8e-6d8d-4b41-8c89-4f5fe8045bca");
  private static final UUID SECOND_OPTION_ID =
      UUID.fromString("ca6eb6a9-ff6b-4d3a-a7b5-bb80d8935b28");
  private static final String BASE_URL =
      "/products/" + PRODUCT_ID + "/option-groups/" + OPTION_GROUP_ID + "/options";

  @MockitoBean private OptionService service;

  @Test
  void shouldCreateOption() throws Exception {
    var command = new CreateOptionCommand("Cheese", new BigDecimal("2.50"));
    when(service.create(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, command)).thenReturn(result(true));

    var mvcResult =
        mockMvc
            .perform(
                post(BASE_URL)
                    .with(manager())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        JsonUtils.toJson(
                            new CreateOptionRequest("Cheese", new BigDecimal("2.50")))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(OPTION_ID.toString()))
            .andExpect(jsonPath("$.optionGroupId").value(OPTION_GROUP_ID.toString()))
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
            .andExpect(jsonPath("$.priceAdjustment").value(2.5))
            .andReturn();

    assertLocationPath(mvcResult, BASE_URL + "/" + OPTION_ID);
    verify(service).create(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, command);
  }

  @Test
  void shouldListUpdateChangeStatusAndReorderOptions() throws Exception {
    when(service.list(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID)).thenReturn(List.of(result(false)));
    var update = new UpdateOptionCommand("Cheese", new BigDecimal("3.00"));
    when(service.update(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, OPTION_ID, update))
        .thenReturn(result(true));

    mockMvc
        .perform(get(BASE_URL).with(reader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].active").value(false));
    mockMvc
        .perform(
            put(BASE_URL + "/" + OPTION_ID)
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonUtils.toJson(new UpdateOptionRequest("Cheese", new BigDecimal("3.00")))))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            put(BASE_URL + "/" + OPTION_ID + "/status")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new ChangeOptionStatusRequest(false))))
        .andExpect(status().isNoContent());
    mockMvc
        .perform(
            put(BASE_URL + "/display-order")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    JsonUtils.toJson(
                        new ReorderOptionsRequest(List.of(SECOND_OPTION_ID, OPTION_ID)))))
        .andExpect(status().isNoContent());

    verify(service).list(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID);
    verify(service).update(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, OPTION_ID, update);
    verify(service).changeStatus(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID, OPTION_ID, false);
    verify(service)
        .reorder(
            TENANT_ID,
            PRODUCT_ID,
            OPTION_GROUP_ID,
            new ReorderOptionsCommand(List.of(SECOND_OPTION_ID, OPTION_ID)));
  }

  @ParameterizedTest
  @MethodSource("invalidMutationRequests")
  void shouldRejectInvalidMutationRequests(MockHttpServletRequestBuilder request) throws Exception {
    mockMvc.perform(request.with(manager())).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void shouldReturnNotFoundFromService() throws Exception {
    when(service.list(TENANT_ID, PRODUCT_ID, OPTION_GROUP_ID))
        .thenThrow(new ResourceNotFoundException("OptionGroup", OPTION_GROUP_ID));

    mockMvc
        .perform(get(BASE_URL).with(reader()))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("OptionGroup id: %s not found".formatted(OPTION_GROUP_ID)));
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
            .content(JsonUtils.toJson(new CreateOptionRequest("", BigDecimal.ZERO))),
        put(BASE_URL + "/" + OPTION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new UpdateOptionRequest("", BigDecimal.ZERO))),
        put(BASE_URL + "/" + OPTION_ID + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new ChangeOptionStatusRequest(null))),
        put(BASE_URL + "/display-order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new ReorderOptionsRequest(null))));
  }

  private static Stream<MockHttpServletRequestBuilder> mutationRequests() {
    return Stream.of(
        post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new CreateOptionRequest("Cheese", BigDecimal.ZERO))),
        put(BASE_URL + "/" + OPTION_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new UpdateOptionRequest("Cheese", BigDecimal.ZERO))),
        put(BASE_URL + "/" + OPTION_ID + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new ChangeOptionStatusRequest(false))),
        put(BASE_URL + "/display-order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(JsonUtils.toJson(new ReorderOptionsRequest(List.of(OPTION_ID)))));
  }

  private static RequestPostProcessor manager() {
    return authenticatedTenantUser(
        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE);
  }

  private static RequestPostProcessor reader() {
    return authenticatedTenantUser(
        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CATALOG_READ);
  }

  private static OptionResult result(boolean active) {
    return new OptionResult(
        OPTION_ID, OPTION_GROUP_ID, TENANT_ID, "Cheese", new BigDecimal("2.50"), active, 0);
  }
}
