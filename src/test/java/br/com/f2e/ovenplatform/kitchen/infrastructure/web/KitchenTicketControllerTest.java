package br.com.f2e.ovenplatform.kitchen.infrastructure.web;

import static br.com.f2e.ovenplatform.identity.infrastructure.security.test.SecurityTestRequestPostProcessors.authenticatedTenantUser;
import static br.com.f2e.ovenplatform.shared.infrastructure.persistence.test.EntityIdTestUtils.withId;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_HEADER;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders.API_VERSION_VALUE;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.infrastructure.security.JwtService;
import br.com.f2e.ovenplatform.kitchen.application.KitchenService;
import br.com.f2e.ovenplatform.kitchen.domain.Ticket;
import br.com.f2e.ovenplatform.kitchen.domain.TicketItem;
import br.com.f2e.ovenplatform.kitchen.domain.TicketStatus;
import br.com.f2e.ovenplatform.kitchen.domain.exception.InvalidTicketStatusTransitionException;
import br.com.f2e.ovenplatform.shared.application.exception.ResourceNotFoundException;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorResponseFactory;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KitchenTicketController.class)
@Import({ApiErrorResponseFactory.class})
class KitchenTicketControllerTest {

  private static final String BASE_URL = "/kitchen/";
  private static final String TICKETS_URL = "/kitchen/tickets";
  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID TICKET_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID ORDER_ID = UUID.fromString("bb210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final String PRODUCT_NAME = "Pizza Portuguesa";
  private static final int VALID_QUANTITY = 2;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private KitchenService kitchenService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private Tracer tracer;
  @MockitoBean private Span span;
  @MockitoBean private TraceContext traceContext;

  @BeforeEach
  void setUp() {
    doReturn(span).when(tracer).currentSpan();
    when(span.context()).thenReturn(traceContext);
    when(traceContext.traceId()).thenReturn("abc-123");
  }

  @Test
  void shouldListTickets() throws Exception {
    var ticket = createTicket();

    when(kitchenService.list(TENANT_ID)).thenReturn(List.of(ticket));

    mockMvc
        .perform(get(TICKETS_URL).with(authenticatedTenantUser(TENANT_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isNotEmpty())
        .andExpect(jsonPath("$[0].id").value(TICKET_ID.toString()))
        .andExpect(jsonPath("$[0].tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$[0].orderId").value(ORDER_ID.toString()))
        .andExpect(jsonPath("$[0].status").value(TicketStatus.RECEIVED.name()))
        .andExpect(jsonPath("$[0].items").isArray())
        .andExpect(jsonPath("$[0].items.length()").value(1))
        .andExpect(jsonPath("$[0].items[0].productId").value(PRODUCT_ID.toString()))
        .andExpect(jsonPath("$[0].items[0].productName").value(PRODUCT_NAME))
        .andExpect(jsonPath("$[0].items[0].quantity").value(2));

    verify(kitchenService).list(TENANT_ID);
  }

  @Test
  void shouldFindTicketById() throws Exception {

    when(kitchenService.findByIdWithItems(TENANT_ID, TICKET_ID)).thenReturn(createTicket());

    mockMvc
        .perform(get(TICKETS_URL + "/" + TICKET_ID).with(authenticatedTenantUser(TENANT_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isNotEmpty())
        .andExpect(jsonPath("$.id").value(TICKET_ID.toString()))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
        .andExpect(jsonPath("$.status").value(TicketStatus.RECEIVED.name()))
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
        .andExpect(jsonPath("$.items[0].productName").value(PRODUCT_NAME))
        .andExpect(jsonPath("$.items[0].quantity").value(2));

    verify(kitchenService).findByIdWithItems(TENANT_ID, TICKET_ID);
  }

  @Test
  void shouldReturnNotFoundWhenFindingTicketByIdAndTicketDoesNotExist() throws Exception {

    when(kitchenService.findByIdWithItems(TENANT_ID, TICKET_ID))
        .thenThrow(new ResourceNotFoundException("Ticket", TICKET_ID));

    var fullpath = TICKETS_URL + "/" + TICKET_ID;
    mockMvc
        .perform(get(fullpath).with(authenticatedTenantUser(TENANT_ID)))
        .andExpect(status().isNotFound())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.NOT_FOUND,
                fullpath,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ApiErrorCodes.RESOURCE_NOT_FOUND,
                "Ticket id: %s not found".formatted(TICKET_ID),
                null,
                HttpStatus.NOT_FOUND.value()));

    verify(kitchenService).findByIdWithItems(TENANT_ID, TICKET_ID);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("ticketCommands")
  void shouldExecuteTicketCommand(
      String scenario, String endpoint, Consumer<KitchenService> serviceVerification)
      throws Exception {

    mockMvc
        .perform(
            post(TICKETS_URL + "/" + TICKET_ID + endpoint)
                .with(authenticatedTenantUser(TENANT_ID))
                .header(API_VERSION_HEADER, API_VERSION_VALUE))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    serviceVerification.accept(verify(kitchenService));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("ticketCommandsWithNotFound")
  void shouldReturnNotFoundWhenExecutingCommandAndTicketDoesNotExist(
      String scenario, String endpoint, Consumer<KitchenService> serviceMock) throws Exception {

    var fullPath = TICKETS_URL + "/" + TICKET_ID + endpoint;

    serviceMock.accept(
        doThrow(new ResourceNotFoundException("Ticket", TICKET_ID)).when(kitchenService));

    mockMvc
        .perform(
            post(fullPath)
                .with(authenticatedTenantUser(TENANT_ID))
                .header(API_VERSION_HEADER, API_VERSION_VALUE))
        .andExpect(status().isNotFound())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.NOT_FOUND,
                fullPath,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ApiErrorCodes.RESOURCE_NOT_FOUND,
                "Ticket id: %s not found".formatted(TICKET_ID),
                null,
                HttpStatus.NOT_FOUND.value()));

    serviceMock.accept(verify(kitchenService));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("ticketCommandsWithInvalidTransition")
  void shouldReturnConflictWhenExecutingCommandWithInvalidTransition(
      String scenario,
      String endpoint,
      TicketStatus currentStatus,
      TicketStatus targetStatus,
      Consumer<KitchenService> serviceMock)
      throws Exception {

    var fullPath = TICKETS_URL + "/" + TICKET_ID + endpoint;

    serviceMock.accept(
        doThrow(new InvalidTicketStatusTransitionException(currentStatus, targetStatus))
            .when(kitchenService));

    mockMvc
        .perform(
            post(fullPath)
                .with(authenticatedTenantUser(TENANT_ID))
                .header(API_VERSION_HEADER, API_VERSION_VALUE))
        .andExpect(status().isConflict())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.CONFLICT,
                fullPath,
                HttpStatus.CONFLICT.getReasonPhrase(),
                ApiErrorCodes.INVALID_TICKET_STATUS_TRANSITION,
                "Cannot transition ticket from %s to %s.".formatted(currentStatus, targetStatus),
                null,
                HttpStatus.CONFLICT.value()));

    serviceMock.accept(verify(kitchenService));
  }

  @Test
  void shouldFindTicketByOrderId() throws Exception {

    when(kitchenService.findByOrderIdWithItems(TENANT_ID, ORDER_ID)).thenReturn(createTicket());

    mockMvc
        .perform(
            get(BASE_URL + "orders/" + ORDER_ID + "/ticket")
                .with(authenticatedTenantUser(TENANT_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isNotEmpty())
        .andExpect(jsonPath("$.id").value(TICKET_ID.toString()))
        .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
        .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
        .andExpect(jsonPath("$.status").value(TicketStatus.RECEIVED.name()))
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
        .andExpect(jsonPath("$.items[0].productName").value(PRODUCT_NAME))
        .andExpect(jsonPath("$.items[0].quantity").value(2));

    verify(kitchenService).findByOrderIdWithItems(TENANT_ID, ORDER_ID);
  }

  @Test
  void shouldReturnNotFoundWhenFindingTicketByOrderIdAndTicketDoesNotExist() throws Exception {

    when(kitchenService.findByOrderIdWithItems(TENANT_ID, ORDER_ID))
        .thenThrow(new ResourceNotFoundException("Ticket", "orderId", ORDER_ID));

    var fullpath = BASE_URL + "orders/" + ORDER_ID + "/ticket";
    mockMvc
        .perform(get(fullpath).with(authenticatedTenantUser(TENANT_ID)))
        .andExpect(status().isNotFound())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.NOT_FOUND,
                fullpath,
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ApiErrorCodes.RESOURCE_NOT_FOUND,
                "Ticket orderId: %s not found".formatted(ORDER_ID),
                null,
                HttpStatus.NOT_FOUND.value()));

    verify(kitchenService).findByOrderIdWithItems(TENANT_ID, ORDER_ID);
  }

  private static Stream<Arguments> ticketCommands() {
    return Stream.of(
        Arguments.of(
            "start preparation",
            "/start-preparation",
            (Consumer<KitchenService>) service -> service.startPreparation(TENANT_ID, TICKET_ID)),
        Arguments.of(
            "mark as ready",
            "/mark-ready",
            (Consumer<KitchenService>) service -> service.markAsReady(TENANT_ID, TICKET_ID)),
        Arguments.of(
            "cancel",
            "/cancel",
            (Consumer<KitchenService>) service -> service.cancel(TENANT_ID, TICKET_ID)));
  }

  private static Stream<Arguments> ticketCommandsWithNotFound() {
    return Stream.of(
        Arguments.of(
            "start preparation when ticket does not exist",
            "/start-preparation",
            (Consumer<KitchenService>) service -> service.startPreparation(TENANT_ID, TICKET_ID)),
        Arguments.of(
            "mark as ready when ticket does not exist",
            "/mark-ready",
            (Consumer<KitchenService>) service -> service.markAsReady(TENANT_ID, TICKET_ID)),
        Arguments.of(
            "cancel when ticket does not exist",
            "/cancel",
            (Consumer<KitchenService>) service -> service.cancel(TENANT_ID, TICKET_ID)));
  }

  private static Stream<Arguments> ticketCommandsWithInvalidTransition() {
    return Stream.of(
        Arguments.of(
            "cannot start preparation from ready",
            "/start-preparation",
            TicketStatus.READY,
            TicketStatus.IN_PREPARATION,
            (Consumer<KitchenService>) service -> service.startPreparation(TENANT_ID, TICKET_ID)),
        Arguments.of(
            "cannot mark received ticket as ready",
            "/mark-ready",
            TicketStatus.RECEIVED,
            TicketStatus.READY,
            (Consumer<KitchenService>) service -> service.markAsReady(TENANT_ID, TICKET_ID)),
        Arguments.of(
            "cannot cancel ready ticket",
            "/cancel",
            TicketStatus.READY,
            TicketStatus.CANCELLED,
            (Consumer<KitchenService>) service -> service.cancel(TENANT_ID, TICKET_ID)));
  }

  private static @NotNull Ticket createTicket() {
    return withId(
        new Ticket(
            TENANT_ID, ORDER_ID, List.of(new TicketItem(PRODUCT_ID, PRODUCT_NAME, VALID_QUANTITY))),
        TICKET_ID);
  }
}
