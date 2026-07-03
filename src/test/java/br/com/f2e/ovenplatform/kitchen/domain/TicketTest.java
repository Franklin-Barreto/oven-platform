package br.com.f2e.ovenplatform.kitchen.domain;

import static br.com.f2e.ovenplatform.kitchen.domain.TicketStatus.CANCELLED;
import static br.com.f2e.ovenplatform.kitchen.domain.TicketStatus.IN_PREPARATION;
import static br.com.f2e.ovenplatform.kitchen.domain.TicketStatus.READY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.f2e.ovenplatform.kitchen.domain.exception.InvalidTicketStatusTransitionException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TicketTest {

  private static final UUID TENANT_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecc");
  private static final UUID ORDER_ID = UUID.fromString("a6210129-f1d5-4942-8d0a-b144e518aecd");
  private static final UUID PRODUCT_ID = UUID.fromString("b5b6c3d2-3f69-45c5-8a4b-8d6d8a9c1234");
  private static final String PRODUCT_NAME = "Pizza Portuguesa";
  private static final int VALID_QUANTITY = 2;

  @Test
  void shouldCreateTicketWithInitialState() {

    var ticket = ticket(TENANT_ID, ORDER_ID);

    assertThat(ticket)
        .isNotNull()
        .satisfies(
            createdTicket -> {
              assertThat(createdTicket.getTenantId()).isEqualTo(TENANT_ID);
              assertThat(createdTicket.getOrderId()).isEqualTo(ORDER_ID);
              assertThat(createdTicket.getStatus()).isEqualTo(TicketStatus.RECEIVED);
              assertThat(createdTicket.getStartedAt()).isNull();
              assertThat(createdTicket.getReadyAt()).isNull();
              assertThat(createdTicket.getCancelledAt()).isNull();

              assertThat(createdTicket.getItems())
                  .hasSize(1)
                  .first()
                  .satisfies(
                      ticketItem -> {
                        assertThat(ticketItem.getProductId()).isEqualTo(PRODUCT_ID);
                        assertThat(ticketItem.getProductName()).isEqualTo(PRODUCT_NAME);
                        assertThat(ticketItem.getQuantity()).isEqualTo(VALID_QUANTITY);
                      });
            });
  }

  @Test
  void shouldRejectNullTenantId() {
    assertThatThrownBy(() -> ticket(null, ORDER_ID))
        .hasMessage("tenantId must not be null")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectNullOrderId() {
    assertThatThrownBy(() -> ticket(TENANT_ID, null))
        .hasMessage("orderId must not be null")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @MethodSource("invalidItems")
  void shouldRejectInvalidItemData(
      UUID productId, String productName, int quantity, String expectedMessage) {
    assertThatThrownBy(() -> ticket(productId, productName, quantity))
        .hasMessage(expectedMessage)
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldExposeItemsAsReadOnlyCollection() {

    var ticketItems = ticket(TENANT_ID, ORDER_ID).getItems();
    var ticketItem = new TicketItem(UUID.randomUUID(), "Pizza Calabresa", VALID_QUANTITY);

    assertThat(ticketItems).hasSize(1);

    assertThatThrownBy(() -> ticketItems.add(ticketItem))
        .isInstanceOf(UnsupportedOperationException.class);

    assertThat(ticketItems).hasSize(1);
  }

  @Test
  void shouldStartPreparation() {
    var ticket = ticket(TENANT_ID, ORDER_ID);
    var startedAt = Instant.parse("2026-05-12T20:18:00Z");

    ticket.startPreparation(startedAt);

    assertThat(ticket)
        .satisfies(
            preparedTicket -> {
              assertThat(preparedTicket.getStatus()).isEqualTo(IN_PREPARATION);
              assertThat(preparedTicket.getStartedAt()).isEqualTo(startedAt);
              assertThat(preparedTicket.getReadyAt()).isNull();
              assertThat(preparedTicket.getCancelledAt()).isNull();
            });
  }

  @Test
  void shouldMarkTicketAsReady() {
    var ticket = ticket(TENANT_ID, ORDER_ID);
    var startedAt = Instant.parse("2026-05-12T20:18:00Z");
    var readyAt = Instant.parse("2026-05-12T20:25:00Z");

    ticket.startPreparation(startedAt);
    ticket.markAsReady(readyAt);

    assertThat(ticket)
        .satisfies(
            preparedTicket -> {
              assertThat(preparedTicket.getStatus()).isEqualTo(READY);
              assertThat(preparedTicket.getStartedAt()).isEqualTo(startedAt);
              assertThat(preparedTicket.getReadyAt()).isEqualTo(readyAt);
              assertThat(preparedTicket.getCancelledAt()).isNull();
            });
  }

  @Test
  void shouldCancelReceivedTicket() {
    var ticket = ticket(TENANT_ID, ORDER_ID);
    var cancelledAt = Instant.parse("2026-05-12T20:18:00Z");

    ticket.cancel(cancelledAt);

    assertThat(ticket)
        .satisfies(
            preparedTicket -> {
              assertThat(preparedTicket.getStatus()).isEqualTo(CANCELLED);
              assertThat(preparedTicket.getStartedAt()).isNull();
              assertThat(preparedTicket.getReadyAt()).isNull();
              assertThat(preparedTicket.getCancelledAt()).isEqualTo(cancelledAt);
            });
  }

  @Test
  void shouldCancelTicketInPreparation() {

    var ticket = ticket(TENANT_ID, ORDER_ID);
    var startedAt = Instant.parse("2026-05-12T20:18:00Z");
    var cancelledAt = Instant.parse("2026-05-12T20:18:00Z");

    ticket.startPreparation(startedAt);
    ticket.cancel(cancelledAt);

    assertThat(ticket)
        .satisfies(
            preparedTicket -> {
              assertThat(preparedTicket.getStatus()).isEqualTo(CANCELLED);
              assertThat(preparedTicket.getStartedAt()).isEqualTo(startedAt);
              assertThat(preparedTicket.getReadyAt()).isNull();
              assertThat(preparedTicket.getCancelledAt()).isEqualTo(cancelledAt);
            });
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidStatusTransitions")
  void shouldThrowExceptionWhenTransitionIsInvalid(
      String scenario,
      Consumer<Ticket> arrange,
      Consumer<Ticket> invalidOperation,
      TicketStatus expectedCurrentStatus,
      TicketStatus expectedTargetStatus) {

    var ticket = ticket(TENANT_ID, ORDER_ID);

    arrange.accept(ticket);

    assertThatThrownBy(() -> invalidOperation.accept(ticket))
        .isInstanceOf(InvalidTicketStatusTransitionException.class)
        .hasMessage(
            "Cannot transition ticket from %s to %s."
                .formatted(expectedCurrentStatus, expectedTargetStatus));

    assertThat(ticket.getStatus()).isEqualTo(expectedCurrentStatus);
  }

  @Test
  void shouldKeepStartedTimestampWhenStartingPreparationAgain() {
    var startedAt = Instant.parse("2026-05-12T20:18:00Z");
    var secondAttemptAt = Instant.parse("2026-05-12T20:25:00Z");

    var ticket = ticket(TENANT_ID, ORDER_ID);

    ticket.startPreparation(startedAt);
    ticket.startPreparation(secondAttemptAt);

    assertThat(ticket.getStatus()).isEqualTo(IN_PREPARATION);
    assertThat(ticket.getStartedAt()).isEqualTo(startedAt);
    assertThat(ticket.getStartedAt()).isNotEqualTo(secondAttemptAt);
    assertThat(ticket.getReadyAt()).isNull();
    assertThat(ticket.getCancelledAt()).isNull();
  }

  @Test
  void shouldKeepReadyTimestampWhenMarkingReadyTicketAsReadyAgain() {
    var startedAt = Instant.parse("2026-05-12T20:18:00Z");
    var readyAt = Instant.parse("2026-05-12T20:25:00Z");
    var secondAttemptAt = Instant.parse("2026-05-12T20:30:00Z");

    var ticket = ticket(TENANT_ID, ORDER_ID);

    ticket.startPreparation(startedAt);
    ticket.markAsReady(readyAt);
    ticket.markAsReady(secondAttemptAt);

    assertThat(ticket.getStatus()).isEqualTo(READY);
    assertThat(ticket.getStartedAt()).isEqualTo(startedAt);
    assertThat(ticket.getReadyAt()).isEqualTo(readyAt);
    assertThat(ticket.getReadyAt()).isNotEqualTo(secondAttemptAt);
    assertThat(ticket.getCancelledAt()).isNull();
  }

  @Test
  void shouldKeepCancelledTimestampWhenCancellingCancelledTicketAgain() {
    var cancelledAt = Instant.parse("2026-05-12T20:18:00Z");
    var secondAttemptAt = Instant.parse("2026-05-12T20:25:00Z");

    var ticket = ticket(TENANT_ID, ORDER_ID);

    ticket.cancel(cancelledAt);
    ticket.cancel(secondAttemptAt);

    assertThat(ticket.getStatus()).isEqualTo(CANCELLED);
    assertThat(ticket.getStartedAt()).isNull();
    assertThat(ticket.getReadyAt()).isNull();
    assertThat(ticket.getCancelledAt()).isEqualTo(cancelledAt);
    assertThat(ticket.getCancelledAt()).isNotEqualTo(secondAttemptAt);
  }

  @Test
  void shouldRejectEmptyItems() {
    assertThatThrownBy(() -> new Ticket(TENANT_ID, ORDER_ID, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("items must have at least 1 item");
  }

  @Test
  void shouldRejectNullItem() {
    assertThatThrownBy(() -> new Ticket(TENANT_ID, ORDER_ID, Collections.singletonList(null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ticketItem must not be null");
  }

  private static Stream<Arguments> invalidItems() {
    return Stream.of(
        Arguments.of(null, PRODUCT_NAME, VALID_QUANTITY, "productId must not be null"),
        Arguments.of(PRODUCT_ID, null, VALID_QUANTITY, "productName must not be null"),
        Arguments.of(PRODUCT_ID, "", VALID_QUANTITY, "productName must not be blank"),
        Arguments.of(
            PRODUCT_ID, "beer", VALID_QUANTITY, "productName must have at least 5 characters"),
        Arguments.of(PRODUCT_ID, PRODUCT_NAME, 0, "quantity must be greater than zero"),
        Arguments.of(PRODUCT_ID, PRODUCT_NAME, -1, "quantity must be greater than zero"));
  }

  private static Stream<Arguments> invalidStatusTransitions() {
    var startedAt = Instant.parse("2026-05-12T20:18:00Z");
    var readyAt = Instant.parse("2026-05-12T20:25:00Z");
    var cancelledAt = Instant.parse("2026-05-12T20:30:00Z");

    return Stream.of(
        Arguments.of(
            "marking received ticket as ready",
            (Consumer<Ticket>) _ -> {},
            (Consumer<Ticket>) ticket -> ticket.markAsReady(readyAt),
            TicketStatus.RECEIVED,
            TicketStatus.READY),
        Arguments.of(
            "starting preparation on ready ticket",
            (Consumer<Ticket>)
                ticket -> {
                  ticket.startPreparation(startedAt);
                  ticket.markAsReady(readyAt);
                },
            (Consumer<Ticket>) ticket -> ticket.startPreparation(startedAt),
            TicketStatus.READY,
            IN_PREPARATION),
        Arguments.of(
            "starting preparation on cancelled ticket",
            (Consumer<Ticket>) ticket -> ticket.cancel(cancelledAt),
            (Consumer<Ticket>) ticket -> ticket.startPreparation(startedAt),
            TicketStatus.CANCELLED,
            IN_PREPARATION),
        Arguments.of(
            "cancelling ready ticket",
            (Consumer<Ticket>)
                ticket -> {
                  ticket.startPreparation(startedAt);
                  ticket.markAsReady(readyAt);
                },
            (Consumer<Ticket>) ticket -> ticket.cancel(cancelledAt),
            TicketStatus.READY,
            TicketStatus.CANCELLED),
        Arguments.of(
            "marking cancelled ticket as ready",
            (Consumer<Ticket>) ticket -> ticket.cancel(cancelledAt),
            (Consumer<Ticket>) ticket -> ticket.markAsReady(readyAt),
            TicketStatus.CANCELLED,
            TicketStatus.READY));
  }

  private static Ticket ticket(UUID tenantId, UUID orderId) {
    return new Ticket(
        tenantId, orderId, List.of(new TicketItem(PRODUCT_ID, PRODUCT_NAME, VALID_QUANTITY)));
  }

  private static void ticket(UUID productId, String productName, int quantity) {
    new Ticket(TENANT_ID, ORDER_ID, List.of(new TicketItem(productId, productName, quantity)));
  }
}
