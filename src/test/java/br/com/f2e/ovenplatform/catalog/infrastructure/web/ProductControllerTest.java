package br.com.f2e.ovenplatform.catalog.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withRandomId;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.TENANT_ID_HEADER;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.LocationHeaderAssertions.assertLocationPath;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.catalog.application.CatalogService;
import br.com.f2e.ovenplatform.catalog.domain.Product;
import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceContext;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {ProductController.class})
@Import(value = {TraceContext.class})
class ProductControllerTest {

  private static final String BASE_URL = "/products";
  private static final BigDecimal VALID_PRICE = BigDecimal.valueOf(10.5);
  private static final String VALID_PRODUCT = "Coca-cola";
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");

  @Autowired private MockMvc mockMvc;
  @MockitoBean private CatalogService catalogService;
  @MockitoBean private JwtService jwtService;

  @Test
  void shouldCreateProduct() throws Exception {

    var product = withRandomId(new Product(TENANT_ID, VALID_PRODUCT, VALID_PRICE));
    when(catalogService.createProduct(TENANT_ID, VALID_PRODUCT, VALID_PRICE)).thenReturn(product);

    var result =
        mockMvc
            .perform(
                post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(TENANT_ID_HEADER, TENANT_ID)
                    .content(JsonUtils.toJson(createProductRequest()))
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
            .andReturn();

    assertLocationPath(result, BASE_URL + "/" + product.getId());

    verify(catalogService).createProduct(TENANT_ID, VALID_PRODUCT, VALID_PRICE);
  }

  @ParameterizedTest
  @MethodSource("invalidRequestsCreate")
  void shouldReturn400WhenCreateRequestIsInvalid(
      CreateProductRequest request, String field, String message) throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .header(TENANT_ID_HEADER, TENANT_ID)
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
  void shouldReturnBadRequestWhenTenantHeaderIsMissing() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(createProductRequest())))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.MISSING_REQUEST_HEADER,
                "Required request header 'X-Tenant-Id' for method parameter type UUID is not present",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(catalogService);
  }

  @Test
  void shouldReturnBadRequestWhenTenantHeaderIsInvalid() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(createProductRequest()))
                .header(TENANT_ID_HEADER, "invalid-uuid"))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.INVALID_ARGUMENT,
                "Invalid UUID string: invalid-uuid",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(catalogService);
  }

  @Test
  void shouldListActiveProducts() throws Exception {
    var product = new Product(TENANT_ID, VALID_PRODUCT, VALID_PRICE);

    when(catalogService.listActiveProducts(TENANT_ID)).thenReturn(List.of(product));

    mockMvc
        .perform(
            get(BASE_URL).header(TENANT_ID_HEADER, TENANT_ID).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$[0].name").value(VALID_PRODUCT))
        .andExpect(jsonPath("$[0].price").value(10.5))
        .andExpect(jsonPath("$[0].active").value(true));

    verify(catalogService).listActiveProducts(TENANT_ID);
  }

  @Test
  void shouldReturnEmptyListWhenNoActiveProductsExist() throws Exception {
    when(catalogService.listActiveProducts(TENANT_ID)).thenReturn(List.of());

    mockMvc
        .perform(
            get(BASE_URL).header(TENANT_ID_HEADER, TENANT_ID).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());

    verify(catalogService).listActiveProducts(TENANT_ID);
  }

  @Test
  void shouldReturnBadRequestWhenListTenantHeaderIsMissing() throws Exception {
    mockMvc
        .perform(get(BASE_URL).accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.MISSING_REQUEST_HEADER,
                "Required request header 'X-Tenant-Id' for method parameter type UUID is not present",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(catalogService);
  }

  @Test
  void shouldReturnBadRequestWhenListTenantHeaderIsInvalid() throws Exception {
    mockMvc
        .perform(
            get(BASE_URL)
                .accept(MediaType.APPLICATION_JSON)
                .header(TENANT_ID_HEADER, "invalid-uuid"))
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.INVALID_ARGUMENT,
                "Invalid UUID string: invalid-uuid",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(catalogService);
  }

  private static Stream<Arguments> invalidRequestsCreate() {
    return Stream.of(
        Arguments.of(new CreateProductRequest(null, VALID_PRICE), "name", "must not be blank"),
        Arguments.of(
            new CreateProductRequest("ab", VALID_PRICE),
            "name",
            "name must have at least 5 characters"),
        Arguments.of(
            new CreateProductRequest(VALID_PRODUCT, BigDecimal.ZERO),
            "price",
            "must be greater than 0"),
        Arguments.of(
            new CreateProductRequest(VALID_PRODUCT, new BigDecimal("-10")),
            "price",
            "must be greater than 0"),
        Arguments.of(new CreateProductRequest(VALID_PRODUCT, null), "price", "must not be null"));
  }

  private static CreateProductRequest createProductRequest() {
    return new CreateProductRequest(VALID_PRODUCT, VALID_PRICE);
  }
}
