package br.com.f2e.ovenplatform.catalog.infrastructure.web;

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

import br.com.f2e.ovenplatform.catalog.application.CatalogService;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = {ProductController.class})
class ProductControllerTest extends AbstractControllerTest {

  private static final String BASE_URL = "/products";
  private static final BigDecimal VALID_PRICE = BigDecimal.valueOf(10.5);
  private static final String VALID_PRODUCT = "Coca-cola";
  private static final String VALID_DESCRIPTION = "Refrigerante lata";
  private static final UUID CATEGORY_ID = UUID.fromString("5b2180d1-cae8-42bd-a3f4-2ab97a49a789");
  private static final UUID PRODUCT_ID = UUID.fromString("22b2759d-35b2-4b04-ab39-df2a203a652c");

  @MockitoBean private CatalogService catalogService;

  @Test
  void shouldCreateProductUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var product = withRandomId(product());

    when(catalogService.createProduct(
            TENANT_ID, CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, VALID_PRICE))
        .thenReturn(product);

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
                    .content(JsonUtils.toJson(createProductRequest()))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
            .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID.toString()))
            .andExpect(jsonPath("$.name").value(VALID_PRODUCT))
            .andExpect(jsonPath("$.description").value(VALID_DESCRIPTION))
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();

    assertLocationPath(result, BASE_URL + "/" + product.getId());

    verify(catalogService)
        .createProduct(TENANT_ID, CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, VALID_PRICE);
  }

  @ParameterizedTest
  @MethodSource("invalidRequestsCreate")
  void shouldReturn400WhenCreateRequestIsInvalid(
      CreateProductRequest request, String field, String message) throws Exception {
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

    verifyNoInteractions(catalogService);
  }

  @Test
  void shouldListActiveProductsUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var product = product();

    when(catalogService.listActiveProducts(TENANT_ID)).thenReturn(List.of(product));

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
        .andExpect(jsonPath("$[0].categoryId").value(CATEGORY_ID.toString()))
        .andExpect(jsonPath("$[0].name").value(VALID_PRODUCT))
        .andExpect(jsonPath("$[0].description").value(VALID_DESCRIPTION))
        .andExpect(jsonPath("$[0].price").value(10.5))
        .andExpect(jsonPath("$[0].active").value(true));

    verify(catalogService).listActiveProducts(TENANT_ID);
  }

  @Test
  void shouldReturnEmptyListWhenNoActiveProductsExist() throws Exception {
    when(catalogService.listActiveProducts(TENANT_ID)).thenReturn(List.of());

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

    verify(catalogService).listActiveProducts(TENANT_ID);
  }

  @Test
  void shouldFindProductUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var product = withId(product(), PRODUCT_ID);

    when(catalogService.getProduct(TENANT_ID, PRODUCT_ID)).thenReturn(product);

    mockMvc
        .perform(
            get(BASE_URL + "/" + PRODUCT_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CATALOG_READ))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(PRODUCT_ID.toString()))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID.toString()))
        .andExpect(jsonPath("$.name").value(VALID_PRODUCT))
        .andExpect(jsonPath("$.description").value(VALID_DESCRIPTION))
        .andExpect(jsonPath("$.price").value(10.5))
        .andExpect(jsonPath("$.active").value(true));

    verify(catalogService).getProduct(TENANT_ID, PRODUCT_ID);
  }

  @Test
  void shouldReturn404WhenFindingUnknownProduct() throws Exception {
    when(catalogService.getProduct(TENANT_ID, PRODUCT_ID))
        .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

    mockMvc
        .perform(
            get(BASE_URL + "/" + PRODUCT_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CATALOG_READ))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Product id: %s not found".formatted(PRODUCT_ID)));

    verify(catalogService).getProduct(TENANT_ID, PRODUCT_ID);
  }

  @Test
  void shouldUpdateProductUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var product = withId(product(), PRODUCT_ID);
    product.deactivate();

    when(catalogService.update(
            TENANT_ID,
            PRODUCT_ID,
            CATEGORY_ID,
            VALID_PRODUCT,
            VALID_DESCRIPTION,
            VALID_PRICE,
            false))
        .thenReturn(product);

    mockMvc
        .perform(
            patch(BASE_URL + "/" + PRODUCT_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateProductRequest()))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID.toString()))
        .andExpect(jsonPath("$.name").value(VALID_PRODUCT))
        .andExpect(jsonPath("$.description").value(VALID_DESCRIPTION))
        .andExpect(jsonPath("$.price").value(10.5))
        .andExpect(jsonPath("$.active").value(false));

    verify(catalogService)
        .update(
            TENANT_ID,
            PRODUCT_ID,
            CATEGORY_ID,
            VALID_PRODUCT,
            VALID_DESCRIPTION,
            VALID_PRICE,
            false);
  }

  @ParameterizedTest
  @MethodSource("invalidRequestsUpdate")
  void shouldReturn400WhenUpdateRequestIsInvalid(
      UpdateProductRequest request, String field, String message) throws Exception {
    mockMvc
        .perform(
            patch(BASE_URL + "/" + PRODUCT_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL + "/" + PRODUCT_ID,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                message,
                field,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(catalogService);
  }

  @Test
  void shouldReturn404WhenUpdatingUnknownProduct() throws Exception {
    when(catalogService.update(
            TENANT_ID,
            PRODUCT_ID,
            CATEGORY_ID,
            VALID_PRODUCT,
            VALID_DESCRIPTION,
            VALID_PRICE,
            false))
        .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

    mockMvc
        .perform(
            patch(BASE_URL + "/" + PRODUCT_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateProductRequest()))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Product id: %s not found".formatted(PRODUCT_ID)));

    verify(catalogService)
        .update(
            TENANT_ID,
            PRODUCT_ID,
            CATEGORY_ID,
            VALID_PRODUCT,
            VALID_DESCRIPTION,
            VALID_PRICE,
            false);
  }

  @Test
  void shouldDeleteProductUsingTenantFromAuthenticatedPrincipal() throws Exception {
    mockMvc
        .perform(
            delete(BASE_URL + "/" + PRODUCT_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

    verify(catalogService).deactivate(TENANT_ID, PRODUCT_ID);
  }

  @Test
  void shouldReturn404WhenDeletingUnknownProduct() throws Exception {
    doThrow(new ResourceNotFoundException("Product", PRODUCT_ID))
        .when(catalogService)
        .deactivate(TENANT_ID, PRODUCT_ID);

    mockMvc
        .perform(
            delete(BASE_URL + "/" + PRODUCT_ID)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Product id: %s not found".formatted(PRODUCT_ID)));

    verify(catalogService).deactivate(TENANT_ID, PRODUCT_ID);
  }

  @ParameterizedTest
  @MethodSource("productMutationRequests")
  void shouldReturnForbiddenWhenProductMutationPermissionIsMissing(
      MockHttpServletRequestBuilder request) throws Exception {
    mockMvc
        .perform(
            request.with(
                authenticatedTenantUser(
                    TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CATALOG_READ)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(catalogService);
  }

  @ParameterizedTest
  @MethodSource("productReadRequests")
  void shouldReturnForbiddenWhenProductReadPermissionIsMissing(
      MockHttpServletRequestBuilder request) throws Exception {
    mockMvc
        .perform(
            request.with(
                authenticatedTenantUser(
                    TENANT_ID, TenantMembershipRole.KITCHEN, TenantPermission.KITCHEN_READ)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(catalogService);
  }

  @Test
  void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
    mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());

    verifyNoInteractions(catalogService);
  }

  private static Stream<Arguments> invalidRequestsCreate() {
    return Stream.of(
        Arguments.of(
            new CreateProductRequest(null, VALID_PRODUCT, VALID_DESCRIPTION, VALID_PRICE),
            "categoryId",
            "must not be null"),
        Arguments.of(
            new CreateProductRequest(CATEGORY_ID, null, VALID_DESCRIPTION, VALID_PRICE),
            "name",
            "must not be blank"),
        Arguments.of(
            new CreateProductRequest(CATEGORY_ID, "ab", VALID_DESCRIPTION, VALID_PRICE),
            "name",
            "name must have at least 5 characters"),
        Arguments.of(
            new CreateProductRequest(CATEGORY_ID, VALID_PRODUCT, "a".repeat(501), VALID_PRICE),
            "description",
            "description must have at most 500 characters"),
        Arguments.of(
            new CreateProductRequest(
                CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, BigDecimal.ZERO),
            "price",
            "must be greater than 0"),
        Arguments.of(
            new CreateProductRequest(
                CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, new BigDecimal("-10")),
            "price",
            "must be greater than 0"),
        Arguments.of(
            new CreateProductRequest(CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, null),
            "price",
            "must not be null"));
  }

  private static Stream<Arguments> invalidRequestsUpdate() {
    return Stream.of(
        Arguments.of(
            new UpdateProductRequest(null, VALID_PRODUCT, VALID_DESCRIPTION, VALID_PRICE, true),
            "categoryId",
            "must not be null"),
        Arguments.of(
            new UpdateProductRequest(CATEGORY_ID, null, VALID_DESCRIPTION, VALID_PRICE, true),
            "name",
            "must not be blank"),
        Arguments.of(
            new UpdateProductRequest(CATEGORY_ID, "ab", VALID_DESCRIPTION, VALID_PRICE, true),
            "name",
            "name must have at least 5 characters"),
        Arguments.of(
            new UpdateProductRequest(
                CATEGORY_ID, VALID_PRODUCT, "a".repeat(501), VALID_PRICE, true),
            "description",
            "description must have at most 500 characters"),
        Arguments.of(
            new UpdateProductRequest(
                CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, BigDecimal.ZERO, true),
            "price",
            "must be greater than 0"),
        Arguments.of(
            new UpdateProductRequest(
                CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, VALID_PRICE, null),
            "active",
            "must not be null"));
  }

  private static Stream<Arguments> productMutationRequests() {
    return Stream.of(
        Arguments.of(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(createProductRequest()))),
        Arguments.of(
            patch(BASE_URL + "/" + PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateProductRequest()))),
        Arguments.of(delete(BASE_URL + "/" + PRODUCT_ID)));
  }

  private static Stream<Arguments> productReadRequests() {
    return Stream.of(Arguments.of(get(BASE_URL)), Arguments.of(get(BASE_URL + "/" + PRODUCT_ID)));
  }

  private static CreateProductRequest createProductRequest() {
    return new CreateProductRequest(CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, VALID_PRICE);
  }

  private static UpdateProductRequest updateProductRequest() {
    return new UpdateProductRequest(
        CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, VALID_PRICE, false);
  }

  private static Product product() {
    return new Product(TENANT_ID, CATEGORY_ID, VALID_PRODUCT, VALID_DESCRIPTION, VALID_PRICE);
  }
}
