package br.com.f2e.ovenplatform.orders.infrastructure.web;

import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_HEADER;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.TENANT_ID_HEADER;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.LocationHeaderAssertions.assertLocationPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.orders.application.CreateOrderCommand;
import br.com.f2e.ovenplatform.orders.application.OrderPaymentMethod;
import br.com.f2e.ovenplatform.orders.application.OrderPaymentStatus;
import br.com.f2e.ovenplatform.orders.application.OrderService;
import br.com.f2e.ovenplatform.orders.application.PaymentInfo;
import br.com.f2e.ovenplatform.orders.domain.Order;
import br.com.f2e.ovenplatform.orders.domain.OrderStatus;
import br.com.f2e.ovenplatform.orders.domain.exception.InvalidOrderStatusTransitionException;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.CreateOrderRequest;
import br.com.f2e.ovenplatform.orders.infrastructure.web.dto.OrderItemRequest;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.tracing.TraceContext;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OrderController.class)
@Import(TraceContext.class)
class OrderControllerTest {

  private static final String BASE_URL = "/orders";
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID PRODUCT_ID = UUID.fromString("b6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("b6210129-f1d5-4942-8d0a-b144e518aecd");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OrderService orderService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private ApplicationEventPublisher eventPublisher;

  @Test
  void shouldCreateOrderWithItems() throws Exception {
    var orderRequest = createOrderRequest(PRODUCT_ID, 3);

    var order = createOrder(TENANT_ID, ORDER_ID, PRODUCT_ID, 3, new BigDecimal("35.40"));

    when(orderService.createOrder(eq(TENANT_ID), any(CreateOrderCommand.class))).thenReturn(order);

    var result =
        mockMvc
            .perform(
                post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonUtils.toJson(orderRequest))
                    .header(TENANT_ID_HEADER, TENANT_ID))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(order.getId().toString()))
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
            .andExpect(jsonPath("$.status").value(OrderStatus.CREATED.name()))
            .andExpect(jsonPath("$.totalAmount").value(106.20))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
            .andExpect(jsonPath("$.items[0].quantity").value(3))
            .andExpect(jsonPath("$.items[0].unitPrice").value(35.40))
            .andExpect(jsonPath("$.items[0].subtotal").value(106.20))
            .andReturn();

    assertLocationPath(result, BASE_URL + "/" + order.getId());

    var commandCaptor = ArgumentCaptor.forClass(CreateOrderCommand.class);

    verify(orderService).createOrder(eq(TENANT_ID), commandCaptor.capture());

    var command = commandCaptor.getValue();

    assertThat(command.items()).hasSize(1);
    assertThat(command.items().getFirst().productId()).isEqualTo(PRODUCT_ID);
    assertThat(command.items().getFirst().quantity()).isEqualTo(3);
    assertThat(command.paymentInfo()).isNotNull();
    assertThat(command.paymentInfo().method()).isEqualTo(OrderPaymentMethod.CASH);
    assertThat(command.paymentInfo().status()).isEqualTo(OrderPaymentStatus.PAID);
  }

  @Test
  void shouldReturnBadRequestWhenTenantHeaderIsMissing() throws Exception {
    var orderRequest = createOrderRequest(PRODUCT_ID, 3);
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(orderRequest)))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            expectValidationErrors(
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
    var orderRequest = createOrderRequest(PRODUCT_ID, 3);
    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(orderRequest))
                .header(TENANT_ID_HEADER, "invalid-request-id"))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            expectValidationErrors(
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
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                message,
                field,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnOrderResponseWhenFoundById() throws Exception {
    var unitPrice = new BigDecimal("25.30");

    var order = createOrder(TENANT_ID, ORDER_ID, PRODUCT_ID, 3, unitPrice);

    when(orderService.findOrder(TENANT_ID, ORDER_ID)).thenReturn(Optional.of(order));

    mockMvc
        .perform(get(BASE_URL + "/" + ORDER_ID).header(TENANT_ID_HEADER, TENANT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$.status").value(OrderStatus.CREATED.name()))
        .andExpect(jsonPath("$.totalAmount").value(75.90))
        .andExpect(jsonPath("$.createdAt").isEmpty())
        .andExpect(jsonPath("$.readyAt").isEmpty())
        .andExpect(jsonPath("$.deliveredAt").isEmpty())
        .andExpect(jsonPath("$.cancelledAt").isEmpty())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
        .andExpect(jsonPath("$.items[0].quantity").value(3))
        .andExpect(jsonPath("$.items[0].unitPrice").value(25.30))
        .andExpect(jsonPath("$.items[0].subtotal").value(75.90));

    verify(orderService).findOrder(TENANT_ID, ORDER_ID);
  }

  @Test
  void shouldReturn404WhenOrderIsNotFound() throws Exception {

    when(orderService.findOrder(TENANT_ID, ORDER_ID)).thenReturn(Optional.empty());

    mockMvc
        .perform(get(BASE_URL + "/" + ORDER_ID).header(TENANT_ID_HEADER, TENANT_ID))
        .andExpect(status().isNotFound());

    verify(orderService).findOrder(TENANT_ID, ORDER_ID);
  }

  @Test
  void shouldReturn400WhenOrderIdIsInvalid() throws Exception {
    var invalidOrderId = "invalid-uuid";

    mockMvc
        .perform(get(BASE_URL + "/" + invalidOrderId).header(TENANT_ID_HEADER, TENANT_ID))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                BASE_URL + "/" + invalidOrderId,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.INVALID_ARGUMENT,
                "Invalid UUID string: invalid-uuid",
                null,
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(orderService);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("transitionEndpoints")
  void shouldReturnNoContentForOrderTransitionEndpoints(
      String endpoint, Consumer<OrderService> verification) throws Exception {
    mockMvc
        .perform(
            post(BASE_URL + "/" + ORDER_ID + endpoint)
                .header(TENANT_ID_HEADER, TENANT_ID)
                .header(API_VERSION_HEADER, API_VERSION_VALUE))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verification.accept(verify(orderService));
  }

  @Test
  void shouldReturn404WhenOrderDoesNotExistToBeMarkedAsReady() throws Exception {
    var fullPath = BASE_URL + "/" + ORDER_ID + "/mark-ready";

    doThrow(new ResourceNotFoundException("Order", ORDER_ID))
        .when(orderService)
        .markAsReady(TENANT_ID, ORDER_ID);

    mockMvc
        .perform(post(fullPath).header(TENANT_ID_HEADER, TENANT_ID))
        .andExpect(status().isNotFound())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.NOT_FOUND,
                fullPath,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ApiErrorCodes.RESOURCE_NOT_FOUND,
                "Order id: %s not found".formatted(ORDER_ID),
                null,
                HttpStatus.NOT_FOUND.value()));

    verify(orderService).markAsReady(TENANT_ID, ORDER_ID);
  }

  @Test
  void shouldReturn409WhenCancellingReadyOrder() throws Exception {
    var fullPath = BASE_URL + "/" + ORDER_ID + "/cancel";

    doThrow(new InvalidOrderStatusTransitionException(OrderStatus.READY, OrderStatus.CANCELLED))
        .when(orderService)
        .cancel(TENANT_ID, ORDER_ID);

    mockMvc
        .perform(post(fullPath).header(TENANT_ID_HEADER, TENANT_ID))
        .andExpect(status().isConflict())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.CONFLICT,
                fullPath,
                HttpStatus.CONFLICT.getReasonPhrase(),
                ApiErrorCodes.INVALID_ORDER_STATUS_TRANSITION,
                "Cannot transition order from %s to %s."
                    .formatted(OrderStatus.READY, OrderStatus.CANCELLED),
                null,
                HttpStatus.CONFLICT.value()));

    verify(orderService).cancel(TENANT_ID, ORDER_ID);
  }

  @Test
  void shouldListOrdersByTenant() throws Exception {
    var unitPrice = new BigDecimal("25.30");
    var secondOrderId = UUID.randomUUID();

    var orders =
        List.of(
            createOrder(TENANT_ID, ORDER_ID, PRODUCT_ID, 3, unitPrice),
            createOrder(TENANT_ID, secondOrderId, PRODUCT_ID, 2, unitPrice));

    when(orderService.listOrders(TENANT_ID)).thenReturn(orders);

    var secondOrderJson = "$[1]";

    mockMvc
        .perform(get(BASE_URL).header(TENANT_ID_HEADER, TENANT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(ORDER_ID.toString()))
        .andExpect(jsonPath(secondOrderJson + ".id").value(secondOrderId.toString()))
        .andExpect(jsonPath(secondOrderJson + ".tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath(secondOrderJson + ".status").value(OrderStatus.CREATED.name()))
        .andExpect(jsonPath(secondOrderJson + ".totalAmount").value(50.60))
        .andExpect(jsonPath(secondOrderJson + ".createdAt").isEmpty())
        .andExpect(jsonPath(secondOrderJson + ".readyAt").isEmpty())
        .andExpect(jsonPath(secondOrderJson + ".deliveredAt").isEmpty())
        .andExpect(jsonPath(secondOrderJson + ".cancelledAt").isEmpty())
        .andExpect(jsonPath(secondOrderJson + ".items").isArray())
        .andExpect(jsonPath(secondOrderJson + ".items.length()").value(1))
        .andExpect(jsonPath(secondOrderJson + ".items[0].productId").value(PRODUCT_ID.toString()))
        .andExpect(jsonPath(secondOrderJson + ".items[0].quantity").value(2))
        .andExpect(jsonPath(secondOrderJson + ".items[0].unitPrice").value(25.30))
        .andExpect(jsonPath(secondOrderJson + ".items[0].subtotal").value(50.60));

    verify(orderService).listOrders(TENANT_ID);
  }

  private Order createOrder(
      UUID tenantId, UUID orderId, UUID productId, int quantity, BigDecimal unitPrice) {
    var order = withId(new Order(tenantId), orderId);
    order.addItem(productId, quantity, unitPrice);
    return order;
  }

  private static Stream<Arguments> invalidCreateOrderRequests() {
    return Stream.of(
        Arguments.of("items", "must not be null", createOrderRequest(null)),
        Arguments.of(
            "items",
            "items must have at least 1 item",
            createOrderRequest(Collections.emptyList())),
        Arguments.of("items[0].productId", "must not be null", createOrderRequest(null, 1)),
        Arguments.of(
            "items[0].quantity", "must be greater than 0", createOrderRequest(PRODUCT_ID, 0)),
        Arguments.of(
            "items[0].quantity", "must be greater than 0", createOrderRequest(PRODUCT_ID, -1)));
  }

  private static Stream<Arguments> transitionEndpoints() {
    return Stream.of(
        Arguments.of(
            "/mark-ready",
            (Consumer<OrderService>) service -> service.markAsReady(TENANT_ID, ORDER_ID)),
        Arguments.of(
            "/mark-delivered",
            (Consumer<OrderService>) service -> service.markAsDelivered(TENANT_ID, ORDER_ID)),
        Arguments.of(
            "/cancel", (Consumer<OrderService>) service -> service.cancel(TENANT_ID, ORDER_ID)));
  }

  private static CreateOrderRequest createOrderRequest(UUID productId, int quantity) {
    return new CreateOrderRequest(
        List.of(new OrderItemRequest(productId, quantity)),
        new PaymentInfo(OrderPaymentMethod.CASH, OrderPaymentStatus.PAID));
  }

  private static CreateOrderRequest createOrderRequest(List<OrderItemRequest> items) {
    return new CreateOrderRequest(
        items, new PaymentInfo(OrderPaymentMethod.CASH, OrderPaymentStatus.PAID));
  }
}
