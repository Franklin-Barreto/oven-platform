package br.com.f2e.ovenplatform.orders.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.TENANT_ID_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.orders.application.CreateOrderCommand;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.domain.Order;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(controllers = OrderController.class)
@Import(TraceContext.class)
class OrderControllerTest {

  private static final String BASE_URL = "/orders";
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PRODUCT_ID = UUID.fromString("b6210129-f1d5-4942-8d0a-b144e518aecc");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OrderService orderService;
  @MockitoBean private JwtService jwtService;

  @Test
  void shouldCreateOrderWithItems() throws Exception {
    var orderRequest = new CreateOrderRequest(List.of(new OrderItemRequest(PRODUCT_ID, 3)));

    var order = createOrder(TENANT_ID, PRODUCT_ID, 3, new BigDecimal("35.40"));

    when(orderService.createOrder(eq(TENANT_ID), any(CreateOrderCommand.class))).thenReturn(order);

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(orderRequest))
                .header(TENANT_ID_HEADER, TENANT_ID))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString(BASE_URL)))
        .andExpect(jsonPath("$.id").value(order.getId().toString()))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$.status").value("CREATED"))
        .andExpect(jsonPath("$.totalAmount").value(106.20))
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
        .andExpect(jsonPath("$.items[0].quantity").value(3))
        .andExpect(jsonPath("$.items[0].unitPrice").value(35.40))
        .andExpect(jsonPath("$.items[0].subtotal").value(106.20));

    var commandCaptor = ArgumentCaptor.forClass(CreateOrderCommand.class);

    verify(orderService).createOrder(eq(TENANT_ID), commandCaptor.capture());

    var command = commandCaptor.getValue();

    assertThat(command.items()).hasSize(1);
    assertThat(command.items().getFirst().productId()).isEqualTo(PRODUCT_ID);
    assertThat(command.items().getFirst().quantity()).isEqualTo(3);
  }

  @Test
  void shouldReturnBadRequestWhenTenantHeaderIsMissing() throws Exception {
    var orderRequest = new CreateOrderRequest(List.of(new OrderItemRequest(PRODUCT_ID, 3)));
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(orderRequest)))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            validationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.MISSING_REQUEST_HEADER,
                "Required request header 'X-Tenant-Id' for method parameter type UUID is not present",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnBadRequestWhenTenantHeaderIsInvalid() throws Exception {
    var orderRequest = new CreateOrderRequest(List.of(new OrderItemRequest(PRODUCT_ID, 3)));
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(orderRequest))
                .header(TENANT_ID_HEADER, "invalid-request-id"))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            validationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.INVALID_ARGUMENT,
                "Invalid UUID string: invalid-request-id",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(orderService);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidCreateOrderRequests")
  void shouldReturnBadRequestWhenRequestBodyIsInvalid(
      String field, String message, CreateOrderRequest request) throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
                .header(TENANT_ID_HEADER, TENANT_ID)
                .accept(MediaType.APPLICATION_JSON))
        .andExpectAll(
            validationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                message,
                field,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(orderService);
  }

  private Order createOrder(UUID tenantId, UUID productId, int quantity, BigDecimal unitPrice) {
    var order = new Order(tenantId);
    order.addItem(productId, quantity, unitPrice);
    return order;
  }

  private static Stream<Arguments> invalidCreateOrderRequests() {
    return Stream.of(
        Arguments.of("items", "must not be null", new CreateOrderRequest(null)),
        Arguments.of("items", "items must have at least 1 item", new CreateOrderRequest(List.of())),
        Arguments.of(
            "items[0].productId",
            "must not be null",
            new CreateOrderRequest(List.of(new OrderItemRequest(null, 1)))),
        Arguments.of(
            "items[0].quantity",
            "must be greater than 0",
            new CreateOrderRequest(List.of(new OrderItemRequest(UUID.randomUUID(), 0)))),
        Arguments.of(
            "items[0].quantity",
            "must be greater than 0",
            new CreateOrderRequest(List.of(new OrderItemRequest(UUID.randomUUID(), -1)))));
  }

  private ResultMatcher[] validationErrors(
      HttpStatus httpStatus,
      String path,
      String error,
      String code,
      String message,
      String field,
      int statusCode) {
    return new ResultMatcher[] {
      status().is(httpStatus.value()),
      jsonPath("$.path").value(path),
      jsonPath("$.error").value(error),
      jsonPath("$.traceId").isNotEmpty(),
      jsonPath("$.errors[0].code").value(code),
      jsonPath("$.errors[0].message").value(message),
      jsonPath("$.errors[0].field").value(field),
      jsonPath("$.status").value(statusCode)
    };
  }
}
