package br.com.f2e.ovenplatform.kitchen.infrastructure.web.dto;

import br.com.f2e.ovenplatform.kitchen.domain.Ticket;
import br.com.f2e.ovenplatform.kitchen.domain.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
    UUID id,
    UUID tenantId,
    UUID orderId,
    TicketStatus status,
    Instant createdAt,
    Instant updatedAt,
    Instant startedAt,
    Instant readyAt,
    Instant cancelledAt,
    List<TicketItemResponse> items) {

  public TicketResponse {
    items = List.copyOf(items);
  }

  public static TicketResponse from(Ticket ticket) {
    return new TicketResponse(
        ticket.getId(),
        ticket.getTenantId(),
        ticket.getOrderId(),
        ticket.getStatus(),
        ticket.getCreatedAt(),
        ticket.getUpdatedAt(),
        ticket.getStartedAt(),
        ticket.getReadyAt(),
        ticket.getCancelledAt(),
        ticket.getItems().stream().map(TicketItemResponse::from).toList());
  }
}
