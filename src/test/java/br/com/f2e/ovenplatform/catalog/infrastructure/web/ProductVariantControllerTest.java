package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import static br.com.f2e.ovenplatform.identity.infrastructure.security.test.SecurityTestRequestPostProcessors.authenticatedTenantUser;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.LocationHeaderAssertions.assertLocationPath;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.catalog.application.variant.CreateProductVariantCommand;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantResult;
import br.com.f2e.ovenplatform.catalog.application.variant.ProductVariantService;
import br.com.f2e.ovenplatform.catalog.application.variant.ReorderProductVariantsCommand;
import br.com.f2e.ovenplatform.catalog.application.variant.UpdateProductVariantCommand;
import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.math.BigDecimal;
import java.net.URI;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = ProductVariantController.class)
class ProductVariantControllerTest extends AbstractControllerTest {

  private static final UUID PRODUCT_ID = UUID.fromString("22b2759d-35b2-4b04-ab39-df2a203a652c");
  private static final UUID VARIANT_ID = UUID.fromString("431e5f0b-a762-4d31-846f-e4fe83db6b19");
  private static final UUID SECOND_VARIANT_ID =
      UUID.fromString("63cd1f20-2023-4073-8421-bdf92a0b24f4");
  private static final UUID IMAGE_ID = UUID.fromString("c03aac3d-3c87-4ca6-bf91-6337620f02a9");
  private static final String BASE_URL = "/products/" + PRODUCT_ID + "/variants";
  private static final String VALID_NAME = "Grande";
  private static final BigDecimal VALID_PRICE = new BigDecimal("37.50");
  private static final URI IMAGE_URL = URI.create("https://images.example/variants/large.webp");

  @MockitoBean private ProductVariantService service;

  @Test
  void shouldCreateVariantUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var command = createCommand();
    var variant = variantResult(true);
    when(service.create(TENANT_ID, PRODUCT_ID, command)).thenReturn(variant);

    var result =
        mockMvc
            .perform(
                post(BASE_URL)
                    .with(manager())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(createRequest()))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(VARIANT_ID.toString()))
            .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
            .andExpect(jsonPath("$.imageId").value(IMAGE_ID.toString()))
            .andExpect(jsonPath("$.imageUrl").value(IMAGE_URL.toString()))
            .andExpect(jsonPath("$.name").value(VALID_NAME))
            .andExpect(jsonPath("$.price").value(37.5))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.displayPosition").value(0))
            .andReturn();

    assertLocationPath(result, BASE_URL + "/" + VARIANT_ID);
    verify(service).create(TENANT_ID, PRODUCT_ID, command);
  }

  @ParameterizedTest
  @MethodSource("invalidCreateRequests")
  void shouldReturn400WhenCreateRequestIsInvalid(
      CreateProductVariantRequest request, String field, String message) throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(validationError(BASE_URL, field, message));

    verifyNoInteractions(service);
  }

  @Test
  void shouldListVariantsUsingTenantFromAuthenticatedPrincipal() throws Exception {
    when(service.listVariants(TENANT_ID, PRODUCT_ID)).thenReturn(List.of(variantResult(false)));

    mockMvc
        .perform(get(BASE_URL).with(reader()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].id").value(VARIANT_ID.toString()))
        .andExpect(jsonPath("$[0].productId").value(PRODUCT_ID.toString()))
        .andExpect(jsonPath("$[0].imageId").value(IMAGE_ID.toString()))
        .andExpect(jsonPath("$[0].imageUrl").value(IMAGE_URL.toString()))
        .andExpect(jsonPath("$[0].name").value(VALID_NAME))
        .andExpect(jsonPath("$[0].price").value(37.5))
        .andExpect(jsonPath("$[0].active").value(false))
        .andExpect(jsonPath("$[0].displayPosition").value(0));

    verify(service).listVariants(TENANT_ID, PRODUCT_ID);
  }

  @Test
  void shouldReturnEmptyListWhenProductHasNoVariants() throws Exception {
    when(service.listVariants(TENANT_ID, PRODUCT_ID)).thenReturn(List.of());

    mockMvc
        .perform(get(BASE_URL).with(reader()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());

    verify(service).listVariants(TENANT_ID, PRODUCT_ID);
  }

  @Test
  void shouldReturn404WhenListingVariantsOfUnknownProduct() throws Exception {
    when(service.listVariants(TENANT_ID, PRODUCT_ID))
        .thenThrow(new ResourceNotFoundException("Product", PRODUCT_ID));

    mockMvc
        .perform(get(BASE_URL).with(reader()).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Product id: %s not found".formatted(PRODUCT_ID)));

    verify(service).listVariants(TENANT_ID, PRODUCT_ID);
  }

  @Test
  void shouldUpdateVariantDetailsUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var command = updateCommand();
    var variant = variantResult(true);
    when(service.update(TENANT_ID, PRODUCT_ID, VARIANT_ID, command)).thenReturn(variant);

    mockMvc
        .perform(
            put(BASE_URL + "/" + VARIANT_ID)
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateRequest()))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(VARIANT_ID.toString()))
        .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
        .andExpect(jsonPath("$.name").value(VALID_NAME))
        .andExpect(jsonPath("$.price").value(37.5))
        .andExpect(jsonPath("$.active").value(true));

    verify(service).update(TENANT_ID, PRODUCT_ID, VARIANT_ID, command);
  }

  @ParameterizedTest
  @MethodSource("invalidUpdateRequests")
  void shouldReturn400WhenUpdateRequestIsInvalid(
      UpdateProductVariantRequest request, String field, String message) throws Exception {
    var path = BASE_URL + "/" + VARIANT_ID;

    mockMvc
        .perform(
            put(path)
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(validationError(path, field, message));

    verifyNoInteractions(service);
  }

  @Test
  void shouldReturn404WhenUpdatingUnknownVariant() throws Exception {
    var command = updateCommand();
    when(service.update(TENANT_ID, PRODUCT_ID, VARIANT_ID, command))
        .thenThrow(new ResourceNotFoundException("ProductVariant", VARIANT_ID));

    mockMvc
        .perform(
            put(BASE_URL + "/" + VARIANT_ID)
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateRequest()))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("ProductVariant id: %s not found".formatted(VARIANT_ID)));

    verify(service).update(TENANT_ID, PRODUCT_ID, VARIANT_ID, command);
  }

  @Test
  void shouldChangeVariantStatusUsingTenantFromAuthenticatedPrincipal() throws Exception {
    mockMvc
        .perform(
            put(BASE_URL + "/" + VARIANT_ID + "/status")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new ChangeProductVariantStatusRequest(false))))
        .andExpect(status().isNoContent());

    verify(service).changeStatus(TENANT_ID, PRODUCT_ID, VARIANT_ID, false);
  }

  @Test
  void shouldReturn400WhenStatusIsMissing() throws Exception {
    var path = BASE_URL + "/" + VARIANT_ID + "/status";

    mockMvc
        .perform(
            put(path)
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new ChangeProductVariantStatusRequest(null))))
        .andExpectAll(validationError(path, "active", "must not be null"));

    verifyNoInteractions(service);
  }

  @Test
  void shouldReturn404WhenChangingStatusOfUnknownVariant() throws Exception {
    doThrow(new ResourceNotFoundException("ProductVariant", VARIANT_ID))
        .when(service)
        .changeStatus(TENANT_ID, PRODUCT_ID, VARIANT_ID, false);

    mockMvc
        .perform(
            put(BASE_URL + "/" + VARIANT_ID + "/status")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new ChangeProductVariantStatusRequest(false))))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("ProductVariant id: %s not found".formatted(VARIANT_ID)));

    verify(service).changeStatus(TENANT_ID, PRODUCT_ID, VARIANT_ID, false);
  }

  @Test
  void shouldReorderVariantsUsingTenantFromAuthenticatedPrincipal() throws Exception {
    var request = reorderRequest();
    var command = new ReorderProductVariantsCommand(request.variantIds());

    mockMvc
        .perform(
            put(BASE_URL + "/display-order")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isNoContent());

    verify(service).reorder(TENANT_ID, PRODUCT_ID, command);
  }

  @Test
  void shouldAllowEmptyOrderWhenProductHasNoVariants() throws Exception {
    var request = new ReorderProductVariantsRequest(List.of());
    var command = new ReorderProductVariantsCommand(request.variantIds());

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
  @MethodSource("invalidReorderRequests")
  void shouldReturn400WhenReorderRequestIsInvalid(ReorderProductVariantsRequest request)
      throws Exception {
    mockMvc
        .perform(
            put(BASE_URL + "/display-order")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void shouldReturn404WhenReorderingVariantsOfUnknownProduct() throws Exception {
    var request = reorderRequest();
    var command = new ReorderProductVariantsCommand(request.variantIds());
    doThrow(new ResourceNotFoundException("Product", PRODUCT_ID))
        .when(service)
        .reorder(TENANT_ID, PRODUCT_ID, command);

    mockMvc
        .perform(
            put(BASE_URL + "/display-order")
                .with(manager())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("Product id: %s not found".formatted(PRODUCT_ID)));

    verify(service).reorder(TENANT_ID, PRODUCT_ID, command);
  }

  @ParameterizedTest
  @MethodSource("mutationRequests")
  void shouldReturnForbiddenWhenCatalogManagePermissionIsMissing(
      MockHttpServletRequestBuilder request) throws Exception {
    mockMvc.perform(request.with(reader())).andExpect(status().isForbidden());

    verifyNoInteractions(service);
  }

  @Test
  void shouldReturnForbiddenWhenCatalogReadPermissionIsMissing() throws Exception {
    mockMvc
        .perform(
            get(BASE_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.KITCHEN, TenantPermission.KITCHEN_READ)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(service);
  }

  @Test
  void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
    mockMvc.perform(get(BASE_URL)).andExpect(status().isUnauthorized());

    verifyNoInteractions(service);
  }

  private static Stream<Arguments> invalidCreateRequests() {
    return Stream.of(
        Arguments.of(
            new CreateProductVariantRequest(IMAGE_ID, null, VALID_PRICE),
            "name",
            "must not be blank"),
        Arguments.of(
            new CreateProductVariantRequest(IMAGE_ID, "a".repeat(81), VALID_PRICE),
            "name",
            "size must be between 0 and 80"),
        Arguments.of(
            new CreateProductVariantRequest(IMAGE_ID, VALID_NAME, BigDecimal.ZERO),
            "price",
            "must be greater than 0"),
        Arguments.of(
            new CreateProductVariantRequest(IMAGE_ID, VALID_NAME, null),
            "price",
            "must not be null"));
  }

  private static Stream<Arguments> invalidUpdateRequests() {
    return Stream.of(
        Arguments.of(
            new UpdateProductVariantRequest(IMAGE_ID, null, VALID_PRICE),
            "name",
            "must not be blank"),
        Arguments.of(
            new UpdateProductVariantRequest(IMAGE_ID, "a".repeat(81), VALID_PRICE),
            "name",
            "size must be between 0 and 80"),
        Arguments.of(
            new UpdateProductVariantRequest(IMAGE_ID, VALID_NAME, BigDecimal.ZERO),
            "price",
            "must be greater than 0"),
        Arguments.of(
            new UpdateProductVariantRequest(IMAGE_ID, VALID_NAME, null),
            "price",
            "must not be null"));
  }

  private static Stream<Arguments> mutationRequests() {
    return Stream.of(
        Arguments.of(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(createRequest()))),
        Arguments.of(
            put(BASE_URL + "/" + VARIANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(updateRequest()))),
        Arguments.of(
            put(BASE_URL + "/" + VARIANT_ID + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new ChangeProductVariantStatusRequest(false)))),
        Arguments.of(
            put(BASE_URL + "/display-order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(reorderRequest()))));
  }

  private static Stream<ReorderProductVariantsRequest> invalidReorderRequests() {
    return Stream.of(
        new ReorderProductVariantsRequest(null),
        new ReorderProductVariantsRequest(java.util.Arrays.asList(VARIANT_ID, null)));
  }

  private static RequestPostProcessor manager() {
    return authenticatedTenantUser(
        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.CATALOG_MANAGE);
  }

  private static RequestPostProcessor reader() {
    return authenticatedTenantUser(
        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.CATALOG_READ);
  }

  private static org.springframework.test.web.servlet.ResultMatcher[] validationError(
      String path, String field, String message) {
    return expectValidationErrors(
        HttpStatus.BAD_REQUEST,
        path,
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        ApiErrorCodes.VALIDATION_ERROR,
        message,
        field,
        HttpStatus.BAD_REQUEST.value());
  }

  private static CreateProductVariantRequest createRequest() {
    return new CreateProductVariantRequest(IMAGE_ID, VALID_NAME, VALID_PRICE);
  }

  private static UpdateProductVariantRequest updateRequest() {
    return new UpdateProductVariantRequest(IMAGE_ID, VALID_NAME, VALID_PRICE);
  }

  private static ReorderProductVariantsRequest reorderRequest() {
    return new ReorderProductVariantsRequest(List.of(SECOND_VARIANT_ID, VARIANT_ID));
  }

  private static CreateProductVariantCommand createCommand() {
    return new CreateProductVariantCommand(IMAGE_ID, VALID_NAME, VALID_PRICE);
  }

  private static UpdateProductVariantCommand updateCommand() {
    return new UpdateProductVariantCommand(IMAGE_ID, VALID_NAME, VALID_PRICE);
  }

  private static ProductVariantResult variantResult(boolean active) {
    return new ProductVariantResult(
        VARIANT_ID, TENANT_ID, PRODUCT_ID, IMAGE_ID, IMAGE_URL, VALID_NAME, VALID_PRICE, active, 0);
  }
}
