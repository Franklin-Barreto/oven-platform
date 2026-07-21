package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import static br.com.f2e.ovenplatform.identity.infrastructure.security.test.SecurityTestRequestPostProcessors.authenticatedTenantUser;
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

import br.com.f2e.ovenplatform.catalog.application.CategoryService;
import br.com.f2e.ovenplatform.catalog.domain.Category;
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

@WebMvcTest(controllers = {CategoryController.class})
class CategoryControllerTest extends AbstractControllerTest {

  private static final String BASE_URL = "/categories";
  private static final String VALID_CATEGORY = "Pizzas";
  private static final String UPDATED_CATEGORY = "Drinks";

  private static final UUID CATEGORY_ID = UUID.fromString("5b2180d1-cae8-42bd-a3f4-2ab97a49a789");

  @MockitoBean private CategoryService categoryService;

  @Test
  void shouldCreateCategoryUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var category = withRandomId(new Category(VALID_CATEGORY, TENANT_ID));

    when(categoryService.save(TENANT_ID, VALID_CATEGORY)).thenReturn(category);

    var result =
        mockMvc
            .perform(
                post(BASE_URL)
                    .with(
                        authenticatedTenantUser(
                            TENANT_ID,
                            TenantMembershipRole.MANAGER,
                            TenantPermission.CATALOG_MANAGE))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(createCategoryRequest()))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
            .andExpect(jsonPath("$.name").value(VALID_CATEGORY))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();

    assertLocationPath(result, BASE_URL + "/" + category.getId());

    verify(categoryService).save(TENANT_ID, VALID_CATEGORY);
  }

  @ParameterizedTest
  @MethodSource("invalidCreateRequests")
  void shouldReturn400WhenCreateRequestIsInvalid(
      CreateCategoryRequest request, String field, String message) throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                message,
                field,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(categoryService);
  }

  @Test
  void shouldListCategoriesUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var category = withRandomId(new Category(VALID_CATEGORY, TENANT_ID));

    when(categoryService.listCategories(TENANT_ID)).thenReturn(List.of(category));

    mockMvc
        .perform(
            get(BASE_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CATALOG_READ))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$[0].name").value(VALID_CATEGORY))
        .andExpect(jsonPath("$[0].active").value(true));

    verify(categoryService).listCategories(TENANT_ID);
  }

  @Test
  void shouldReturnEmptyListWhenNoCategoriesExist() throws Exception {
    when(categoryService.listCategories(TENANT_ID)).thenReturn(List.of());

    mockMvc
        .perform(
            get(BASE_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CATALOG_READ))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());

    verify(categoryService).listCategories(TENANT_ID);
  }

  @Test
  void shouldUpdateCategoryUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var category = withRandomId(new Category(UPDATED_CATEGORY, TENANT_ID));
    category.deactivate();

    when(categoryService.update(TENANT_ID, CATEGORY_ID, UPDATED_CATEGORY, false))
        .thenReturn(category);

    mockMvc
        .perform(
            patch(BASE_URL + "/" + CATEGORY_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateCategoryRequest()))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$.name").value(UPDATED_CATEGORY))
        .andExpect(jsonPath("$.active").value(false));

    verify(categoryService).update(TENANT_ID, CATEGORY_ID, UPDATED_CATEGORY, false);
  }

  @ParameterizedTest
  @MethodSource("invalidUpdateRequests")
  void shouldReturn400WhenUpdateRequestIsInvalid(
      UpdateCategoryRequest request, String field, String message) throws Exception {
    mockMvc
        .perform(
            patch(BASE_URL + "/" + CATEGORY_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL + "/" + CATEGORY_ID,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                message,
                field,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(categoryService);
  }

  @Test
  void shouldReturn404WhenUpdatingUnknownCategory() throws Exception {
    when(categoryService.update(TENANT_ID, CATEGORY_ID, UPDATED_CATEGORY, false))
        .thenThrow(new ResourceNotFoundException("Category", CATEGORY_ID));

    mockMvc
        .perform(
            patch(BASE_URL + "/" + CATEGORY_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateCategoryRequest()))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Category id: %s not found".formatted(CATEGORY_ID)));

    verify(categoryService).update(TENANT_ID, CATEGORY_ID, UPDATED_CATEGORY, false);
  }

  @Test
  void shouldDeleteCategoryUsingTenantFromAuthenticatedPrincipal() throws Exception {
    mockMvc
        .perform(
            delete(BASE_URL + "/" + CATEGORY_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(categoryService).deactivate(TENANT_ID, CATEGORY_ID);
  }

  @Test
  void shouldReturn404WhenDeletingUnknownCategory() throws Exception {
    doThrow(new ResourceNotFoundException("Category", CATEGORY_ID))
        .when(categoryService)
        .deactivate(TENANT_ID, CATEGORY_ID);

    mockMvc
        .perform(
            delete(BASE_URL + "/" + CATEGORY_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Category id: %s not found".formatted(CATEGORY_ID)));

    verify(categoryService).deactivate(TENANT_ID, CATEGORY_ID);
  }

  @ParameterizedTest
  @MethodSource("categoryMutationRequests")
  void shouldReturnForbiddenWhenCategoryMutationPermissionIsMissing(
      MockHttpServletRequestBuilder request) throws Exception {
    mockMvc
        .perform(
            request.with(
                authenticatedTenantUser(
                    TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CATALOG_READ)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(categoryService);
  }

  @Test
  void shouldReturnForbiddenWhenCategoryReadPermissionIsMissing() throws Exception {
    mockMvc
        .perform(
            get(BASE_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.KITCHEN, TenantPermission.KITCHEN_READ)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(categoryService);
  }

  @Test
  void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
    mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());

    verifyNoInteractions(categoryService);
  }

  private static Stream<Arguments> invalidCreateRequests() {
    return Stream.of(
        Arguments.of(new CreateCategoryRequest(null), "name", "must not be blank"),
        Arguments.of(
            new CreateCategoryRequest("beer"), "name", "name must have at least 5 characters"));
  }

  private static Stream<Arguments> invalidUpdateRequests() {
    return Stream.of(
        Arguments.of(new UpdateCategoryRequest(null, true), "name", "must not be blank"),
        Arguments.of(
            new UpdateCategoryRequest("beer", true),
            "name",
            "name must have at least 5 characters"),
        Arguments.of(
            new UpdateCategoryRequest(UPDATED_CATEGORY, null), "active", "must not be null"));
  }

  private static Stream<Arguments> categoryMutationRequests() {
    return Stream.of(
        Arguments.of(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(createCategoryRequest()))),
        Arguments.of(
            patch(BASE_URL + "/" + CATEGORY_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateCategoryRequest()))),
        Arguments.of(delete(BASE_URL + "/" + CATEGORY_ID)));
  }

  private static CreateCategoryRequest createCategoryRequest() {
    return new CreateCategoryRequest(VALID_CATEGORY);
  }

  private static UpdateCategoryRequest updateCategoryRequest() {
    return new UpdateCategoryRequest(UPDATED_CATEGORY, false);
  }
}
