package br.com.f2e.ovenplatform.kitchen.application;

import br.com.f2e.ovenplatform.kitchen.domain.Ticket;
import br.com.f2e.ovenplatform.kitchen.domain.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public record TicketUpdateResult(
    UUID tenantId,
    UUID ticketId,
    UUID orderId,
    TicketStatus status,
    Instant readyAt,
    boolean changed) {

  public static TicketUpdateResult from(Ticket ticket, boolean changed) {
    return new TicketUpdateResult(
        ticket.getTenantId(),
        ticket.getId(),
        ticket.getOrderId(),
        ticket.getStatus(),
        ticket.getReadyAt(),
        changed);
  }
}
